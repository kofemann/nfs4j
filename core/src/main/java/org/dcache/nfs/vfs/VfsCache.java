/*
 * Copyright (c) 2009 - 2017 Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program (see the file COPYING.LIB for more
 * details); if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.dcache.nfs.vfs;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.hazelcast.config.CacheConfig;
import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.InMemoryFormat;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.security.auth.Subject;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import javax.cache.spi.CachingProvider;

import static java.util.Objects.requireNonNull;

/**
 * Caching decorator.
 */
public class VfsCache extends ForwardingFileSystem {

    private final Cache<CacheKey, Inode> _lookupCache;
    private final Cache<Inode, Stat> _statCache;
    private final Cache<Inode, Inode> _parentCache;
    private final Supplier<FsStat> _fsStatSupplier;

    private final VirtualFileSystem _inner;

    public VfsCache(VirtualFileSystem inner, VfsCacheConfig cacheConfig) {
        _inner = inner;

        _lookupCache = new JCacheBuilder<>()
                .withCacheName("vfs-lookup-cache")
                .withKeyType(CacheKey.class)
                .withValueType(Inode.class)
                .withCacheLoader(new LookupLoader())
                .withMaximumSize(cacheConfig.getMaxEntries())
                .expireAfterWrite(cacheConfig.getLifeTime(), cacheConfig.getTimeUnit())
                .recordStats()
                .build();

        _statCache = new JCacheBuilder<>()
                .withCacheName("vfs-stat-cache")
                .withKeyType(Inode.class)
                .withValueType(Stat.class)
                .withCacheLoader( new StatLoader())
                .withMaximumSize(cacheConfig.getMaxEntries())
                .expireAfterWrite(cacheConfig.getLifeTime(), cacheConfig.getTimeUnit())
                .recordStats()
                .build();

        _parentCache = new JCacheBuilder<>()
                .withCacheName("vfs-parent-cache")
                .withKeyType(Inode.class)
                .withValueType(Inode.class)
                .withCacheLoader(new ParentLoader())
                .withMaximumSize(cacheConfig.getMaxEntries())
                .expireAfterWrite(100, TimeUnit.MILLISECONDS)
                .recordStats()
                .build();

        _fsStatSupplier = cacheConfig.getFsStatLifeTime() > 0 ?
                Suppliers.memoizeWithExpiration(new FsStatSupplier(), cacheConfig.getFsStatLifeTime(), cacheConfig.getFsSataTimeUnit()) :
                new FsStatSupplier();
    }

    @Override
    protected VirtualFileSystem delegate() {
        return _inner;
    }

    @Override
    public void commit(Inode inode, long offset, int count) throws IOException {
        invalidateStatCache(inode);
        _inner.commit(inode, offset, count);
    }

    @Override
    public Inode symlink(Inode parent, String path, String link, Subject subject, int mode) throws IOException {
        Inode inode = _inner.symlink(parent, path, link, subject, mode);
	invalidateStatCache(parent);
	return inode;
    }

    @Override
    public void remove(Inode parent, String path) throws IOException {
	Inode inode = lookup(parent, path);
        _inner.remove(parent, path);
        invalidateLookupCache(parent, path);
	invalidateStatCache(parent);
	invalidateStatCache(inode);
    }

    @Override
    public Inode parentOf(Inode inode) throws IOException {
        return parentFromCacheOrLoad(inode);
    }

    @Override
    public boolean move(Inode src, String oldName, Inode dest, String newName) throws IOException {

        boolean isChanged = _inner.move(src, oldName, dest, newName);
	if (isChanged) {
	    invalidateLookupCache(src, oldName);
	    invalidateLookupCache(dest, newName);
	    invalidateStatCache(src);
	    invalidateStatCache(dest);
	}
	return isChanged;
    }

    @Override
    public Inode mkdir(Inode parent, String path, Subject subject, int mode) throws IOException {
        Inode inode = _inner.mkdir(parent, path, subject, mode);
        updateLookupCache(parent, path, inode);
	invalidateStatCache(parent);
        return inode;
    }

    @Override
    public Inode link(Inode parent, Inode link, String path, Subject subject) throws IOException {
        Inode inode = _inner.link(parent, link, path, subject);
        updateLookupCache(parent, path, inode);
	invalidateStatCache(parent);
	invalidateStatCache(inode);
        return inode;
    }

    @Override
    public Inode lookup(Inode parent, String path) throws IOException {
        return lookupFromCacheOrLoad(parent, path);
    }

    @Override
    public FsStat getFsStat() throws IOException {
        return _fsStatSupplier.get();
    }

    @Override
    public Inode create(Inode parent, Stat.Type type, String path, Subject subject, int mode) throws IOException {
        Inode inode = _inner.create(parent, type, path, subject, mode);
        updateLookupCache(parent, path, inode);
	invalidateStatCache(parent);
        updateParentCache(inode, parent);
        return inode;
    }

    @Override
    public Stat getattr(Inode inode) throws IOException {
        return statFromCacheOrLoad(inode);
    }

    @Override
    public void setattr(Inode inode, Stat stat) throws IOException {
        _inner.setattr(inode, stat);
	invalidateStatCache(inode);
    }

    /*
       Utility methods for cache manipulation.
     */

    /**
     * Discards cached value in lookup cache for given inode and path.
     *
     * @param parent inode
     * @param path to invalidate
     */
    public void invalidateLookupCache(Inode parent, String path) {
	_lookupCache.remove(new CacheKey(parent, path));
    }

    private void updateLookupCache(Inode parent, String path, Inode inode) {
	_lookupCache.put(new CacheKey(parent, path), inode);
    }

    /**
     * Discards cached {@link Stat} value for given {@link Inode}.
     *
     * @param inode inode of the object to invalidate in the cache.
     */
    public void invalidateStatCache(final Inode inode) {
	_statCache.remove(inode);
    }

    private void updateParentCache(Inode inode, Inode parent) {
        _parentCache.put(inode, parent);
    }

    private abstract class SimpleCacheLoader<K, V> implements CacheLoader<K, V> {

        @Override
        public Map<K, V> loadAll(Iterable<? extends K> keys) throws CacheLoaderException {
            Map<K, V> loadedValues = new HashMap<>();
            keys.forEach(k -> loadedValues.put(k, load(k)));
            return loadedValues;
        }
    }

    private class LookupLoader extends SimpleCacheLoader<CacheKey, Inode> {

        @Override
        public Inode load(CacheKey k) throws CacheLoaderException {
            try {
                return _inner.lookup(k.getParent(), k.getName());
            } catch (IOException e) {
                throw new CacheLoaderException(e);
            }
        }
    }

    private Inode lookupFromCacheOrLoad(final Inode parent, final String path) throws IOException {
	try {
	    return _lookupCache.get(new CacheKey(parent, path));
	} catch (CacheLoaderException e) {
	    Throwable t = e.getCause();
	    Throwables.throwIfInstanceOf(t, IOException.class);
            Throwables.throwIfUnchecked(t);
	    throw new IOException(e.getMessage(), t);
	}
    }

    private class StatLoader extends SimpleCacheLoader<Inode, Stat> {

        @Override
        public Stat load(Inode key) throws CacheLoaderException {
            try {
                return _inner.getattr(key);
            } catch (IOException e) {
                throw new CacheLoaderException(e);
            }
        }
    }

    private Stat statFromCacheOrLoad(final Inode inode) throws IOException {
	try {
	    return _statCache.get(inode);
	} catch (CacheLoaderException e) {
	    Throwable t = e.getCause();
	    Throwables.throwIfInstanceOf(t, IOException.class);
            Throwables.throwIfUnchecked(t);
	    throw new IOException(e.getMessage(), t);
	}
    }

    private class ParentLoader extends SimpleCacheLoader<Inode, Inode> {

        @Override
        public Inode load(Inode inode) throws CacheLoaderException {
            try {
                return _inner.parentOf(inode);
            } catch (IOException e) {
                throw new CacheLoaderException(e);
            }
        }
    }

    private Inode parentFromCacheOrLoad(final Inode inode) throws IOException {
        try {
            return _parentCache.get(inode);
        } catch (CacheLoaderException e) {
            Throwable t = e.getCause();
            Throwables.throwIfInstanceOf(t, IOException.class);
            throw new IOException(e.getMessage(), t);
        }
    }
    /**
     * Cache entry key based on parent id and name
     */
    private static class CacheKey implements Serializable {

        private static final long serialVersionUID = 6728520521839650679L;

        private final Inode _parent;
        private final String _name;

        public CacheKey(Inode parent, String name) {
            _parent = parent;
            _name = name;
        }

        @Override
        public boolean equals(Object obj) {

            if (obj == this) {
                return true;
            }
            if (!(obj instanceof CacheKey)) {
                return false;
            }

            final CacheKey other = (CacheKey) obj;
            return other._parent.equals(_parent)
                    & other._name.equals(_name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(_name, _parent);
        }

        public String getName() {
            return _name;
        }

        public Inode getParent() {
            return _parent;
        }
    }

    private class FsStatSupplier implements Supplier<FsStat> {

        @Override
        public FsStat get() {
            try {
                return _inner.getFsStat();
            }catch (IOException e) {
                // not true, but good enough.
                return new FsStat(0, 0, 0, 0);
            }
        }
    }

    private static class JCacheBuilder<K, V> {

        private EvictionConfig.MaxSizePolicy maxSizePolicy;
        private int cacheSize = Integer.MAX_VALUE;

        private CacheLoader<K, V> cacheLoader;

        private Duration createExpiry = Duration.ETERNAL;
        private Duration accessExpiry = Duration.ETERNAL;
        private Duration updateExpiry = Duration.ETERNAL;

        private Class<K> keyType;
        private Class<V> valueType;
        private String cacheName;
        private boolean recordStats;

        public JCacheBuilder withCacheLoader(CacheLoader<K, V> cacheLoader) {
            this.cacheLoader = cacheLoader;
            return this;
        }

        public JCacheBuilder withKeyType(Class<K> keyType) {
            this.keyType = keyType;
            return this;
        }

        public JCacheBuilder withValueType(Class<V> valueType) {
            this.valueType = valueType;
            return this;
        }

        public JCacheBuilder withMaximumSize(int size) {
            cacheSize = size;
            return this;
        }

        public JCacheBuilder expireAfterWrite(long time, TimeUnit unit) {
            createExpiry = new Duration(unit, time);
            updateExpiry = new Duration(unit, time);
            return this;
        }

        public JCacheBuilder expireAfterAccess(long time, TimeUnit unit) {
            accessExpiry = new Duration(unit, time);
            return this;
        }

        public JCacheBuilder withCacheName(String cacheName) {
            this.cacheName = cacheName;
            return this;
        }

        public JCacheBuilder recordStats() {
            this.recordStats = true;
            return this;
        }

        Cache<? extends K, ? extends V> build() {

            requireNonNull(cacheName, "Cache name must be initialized");
            requireNonNull(keyType, "Key type must be initialized");
            requireNonNull(valueType, "Value type must be initialized");

            final CachingProvider provider = Caching.getCachingProvider();
            final CacheManager cacheManager = provider.getCacheManager();

            final ExpiryPolicy expiry = new ExpiryPolicy() {
                @Override
                public Duration getExpiryForCreation() {
                    return createExpiry;
                }

                @Override
                public Duration getExpiryForAccess() {
                    return accessExpiry;
                }

                @Override
                public Duration getExpiryForUpdate() {
                    return updateExpiry;
                }
            };

            final CompleteConfiguration<K, V> configuration = new MutableConfiguration()
                    .setTypes(keyType, valueType)
                    .setStoreByValue(false)
                    .setStatisticsEnabled(recordStats)
                    .setExpiryPolicyFactory(() -> expiry)
                    .setReadThrough(cacheLoader != null)
                    .setCacheLoaderFactory(() -> cacheLoader);

            /*
             * JSR107 does not provides a way to configure cache size.
             * Thus we need to use back-end implementation specific code tricks.
             */

            /*
             * REVISIT:
             * We can initialize hazelcast by putting configuration into external
             * XML file. Nevertheless, this requires to expose some internal
             * classes and maintain an external XML file.
             */

            CacheConfig hzRawConfig = new CacheConfig(configuration);
            hzRawConfig.setInMemoryFormat(InMemoryFormat.OBJECT);

            EvictionConfig ec = new EvictionConfig(hzRawConfig.getEvictionConfig())
                    .setMaximumSizePolicy(EvictionConfig.MaxSizePolicy.ENTRY_COUNT)
                    .setSize(cacheSize);
            hzRawConfig.setEvictionConfig(ec);

            return cacheManager.createCache(cacheName, hzRawConfig);
        }
    }
}
