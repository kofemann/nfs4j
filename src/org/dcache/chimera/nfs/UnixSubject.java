/*
 * Copyright (c) 2009 - 2012 Deutsches Elektronen-Synchroton,
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
package org.dcache.chimera.nfs;

import com.google.common.base.Objects;

import java.util.Arrays;

/**
 * A subject representing UNIX user.
 */
public class UnixSubject {

    private final int _uid;
    private final int _gid;
    private final int[] _gids;

    /**
     * Construct a new {@code UnixSubject} with given uid, gid and aux gids.
     *
     * @param uid  user's UID
     * @param gid  user's primary GID
     * @param gids user's secondary GIDs
     */
    public UnixSubject(int uid, int gid, int... gids) {
        _uid = uid;
        _gid = gid;
        _gids = Arrays.copyOf(gids, gids.length);
    }

    public int getUid() {
        return _uid;
    }

    public int getGid() {
        return _gid;
    }

    public int[] getGids() {
        return _gids;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("uid", _uid)
                .add("gid", _gid)
                .add("gids", Arrays.toString(_gids))
                .toString();
    }
}
