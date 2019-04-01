/*
 * Copyright (c) 2009 - 2019 Deutsches Elektronen-Synchroton,
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
package org.dcache.nfs;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.sleepycat.bind.tuple.LongBinding;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link ExportTable} that uses BerkeleyDB to store export
 * entries.
 */
public class ExportDB implements ExportTable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportDB.class);

    /**
     * Export db name.
     */
    //   private static final String EXPORT_DB = "nfs-export-db";
    /**
     * DB runtime environment.
     */
//    private final Environment env;
    /**
     * Database access configuration.
     */
    //   private final DatabaseConfig dbConfig;
    /**
     * Database with actual export entries.
     */
    //   private Database exportDatabase;
    /**
     * DB cursor default configuration.
     */
    //   private final CursorConfig config = new CursorConfig();
    private final Multimap<Integer, FsExport> exports;

    /**
     * Readwrite lock to fanel concurrency.
     */
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public ExportDB(File dir) {
        /*
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setTransactional(true);
        envConfig.setAllowCreate(true);
        envConfig.setReadOnly(false);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        env = new Environment(dir, envConfig);

        dbConfig = new DatabaseConfig();
        dbConfig.setTransactional(true);
        dbConfig.setAllowCreate(true);
        dbConfig.setReadOnly(false);

        env.openDatabase(null, EXPORT_DB, dbConfig);
         */
        exports = MultimapBuilder
                .hashKeys()
                .arrayListValues()
                .build();
    }

    @Override
    public Stream<FsExport> exports() {

        Lock lock = rwLock.readLock();
        lock.lock();
        try {
            return new ArrayList<>(exports.values()).stream();
        } finally {
            lock.unlock();
        }

    }

    @Override
    public Stream<FsExport> exports(InetAddress client) {
        return exports()
                .filter(e -> e.isAllowed(client))
                .sorted(Ordering.from(HostEntryComparator::compare).onResultOf(FsExport::client));
    }

    @Override
    public FsExport getExport(String path, InetAddress client) {
        String normalizedPath = FsExport.normalize(path);
        return getExport(FsExport.getExportIndex(normalizedPath), client);
    }

    @Override
    public FsExport getExport(int index, InetAddress client) {

        Lock lock = rwLock.readLock();
        lock.lock();
        try {
            return exports.get(index).stream()
                    .filter(e -> e.isAllowed(client))
                    .findFirst()
                    .orElse(null);
        } finally {
            lock.unlock();
        }
    }

    public void addExport(FsExport export) {
        Lock lock = rwLock.writeLock();
        lock.lock();
        try {
            exports.put(export.getIndex(), export);
            exports.replaceValues(export.getIndex(),
                    exports.get(export.getIndex())
                            .stream().sorted(Ordering.from(HostEntryComparator::compare)
                            .onResultOf(FsExport::client)).collect(Collectors.toList()));
        } finally {
            lock.unlock();
        }
    }

    public void removeExport(String path) {
        String normalizedPath = FsExport.normalize(path);
        int index = FsExport.getExportIndex(normalizedPath);
        Lock lock = rwLock.writeLock();
        lock.lock();
        try {
            exports.removeAll(index);
        } finally {
            lock.unlock();
        }
    }

    public void removeExport(String path, String client) {
        String normalizedPath = FsExport.normalize(path);
        int index = FsExport.getExportIndex(normalizedPath);
        Lock lock = rwLock.writeLock();
        lock.lock();
        try {
            exports.replaceValues(index, exports.get(index).stream().filter(e -> e.client().equals(client)).collect(Collectors.toSet()));
        } finally {
            lock.unlock();
        }
    }
}
