/*
 * Copyright (c) 2009 - 2013 Deutsches Elektronen-Synchroton,
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
package org.dcache.nfs.lm;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkArgument;
import java.util.Arrays;
import org.dcache.utils.Bytes;


public class Lock {

    private final static long WHOLE_FILE = Long.MAX_VALUE;

    private final long _begin;
    private final long _end;
    private final byte[] _owner;

    public Lock(long begin, long end, byte[] owner) {

        checkArgument(begin >= 0, "negative begin offset");
        checkArgument(end >= -1,  "negative end offset");
        checkArgument(owner != null , "ower can't be null");

        _begin = begin;
        _end = end < 0 ? WHOLE_FILE : end;
        checkArgument(_end > _begin , "invalid range");
        _owner = Arrays.copyOf(owner, owner.length);
    }

    public long getBegin() {
        return _begin;
    }

    public long getEnd() {
        return _end;
    }

    public byte[] getOwner() {
        return Arrays.copyOf(_owner, _owner.length);
    }

    public boolean conflicts(Lock lock) {
        if (Arrays.equals(_owner, lock.getOwner()))
            return false;

        return (_begin <= lock.getEnd() && _end >= lock.getBegin())
                ||
                (_begin >= lock.getBegin() && _end <= lock.getEnd());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + (int) (this._begin ^ (this._begin >>> 32));
        hash = 83 * hash + (int) (this._end ^ (this._end >>> 32));
        hash = 83 * hash + Arrays.hashCode(this._owner);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Lock other = (Lock) obj;
        if (this._begin != other._begin) {
            return false;
        }
        if (this._end != other._end) {
            return false;
        }
        return Arrays.equals(this._owner, other._owner);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper("lock")
                .add("owner", Bytes.toHexString(_owner))
                .add("begin", _begin)
                .add("end", _end)
                .toString();
    }

}
