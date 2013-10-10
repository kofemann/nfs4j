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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import org.dcache.nfs.vfs.Inode;


public class LockManager {
    private  final Multimap<Inode, Lock> allLocks = ArrayListMultimap.create();

    public synchronized void add(Inode inode, Lock lock) throws ConflictingLockException {

        Collection<Lock> inodeLocks = allLocks.get(inode);
        for (Lock existingLock: inodeLocks) {
            if (existingLock.conflicts(lock)) {
                throw new ConflictingLockException(existingLock);
            }
        }

        inodeLocks.add(lock);
    }

    public synchronized void remove(Inode inode, Lock lock) {
        Collection<Lock> inodeLocks = allLocks.get(inode);
        inodeLocks.remove(lock);
    }

    public synchronized void removeAll(Inode inode) {
        allLocks.removeAll(inode);
    }
}
