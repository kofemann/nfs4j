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

import org.dcache.nfs.vfs.Inode;
import org.junit.Test;

import org.junit.Before;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 *
 * @author tigran
 */
public class LockManagerTest {

    private static final byte[] OWNER1 = "owner1".getBytes();
    private static final byte[] OWNER2 = "owner2".getBytes();
    private LockManager lockManager;

    @Before
    public void setUp() {
        lockManager = new LockManager();
    }

    @Test
    public void testAddNew() throws ConflictingLockException {
        Inode inode = mock(Inode.class);
        Lock lock = new Lock(0, 1, OWNER1);
        lockManager.add(inode, lock);
    }

    @Test(expected = ConflictingLockException.class)
    public void testAddConflicting() throws ConflictingLockException {
        Inode inode = mock(Inode.class);
        Lock lock1 = new Lock(0, 1, OWNER1);
        Lock lock2 = new Lock(0, 1, OWNER2);
        lockManager.add(inode, lock1);
        lockManager.add(inode, lock2);
    }

    @Test
    public void testRemoveConflicting() throws ConflictingLockException {
        Inode inode = mock(Inode.class);
        Lock lock1 = new Lock(0, 1, OWNER1);
        Lock lock2 = new Lock(0, 1, OWNER2);
        lockManager.add(inode, lock1);
        lockManager.remove(inode, lock1);
        lockManager.add(inode, lock2);
    }

    @Test
    public void testConflictingDifferentInode() throws ConflictingLockException {
        Inode inode1 = mock(Inode.class);
        Inode inode2 = mock(Inode.class);
        Lock lock1 = new Lock(0, 1, OWNER1);
        Lock lock2 = new Lock(0, 1, OWNER2);
        lockManager.add(inode1, lock1);
        lockManager.add(inode2, lock2);
    }
}
