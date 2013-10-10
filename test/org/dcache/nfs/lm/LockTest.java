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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tigran
 */


public class LockTest {

    private static final byte[] OWNER1 = "owner1".getBytes();
    private static final byte[] OWNER2 = "owner2".getBytes();
    private static final byte[] OWNER3 = "owner3".getBytes();

    @Test
    public void testConficlintg() {
        Lock lock1 = new Lock(7, 10, OWNER1);
        Lock lock2 = new Lock(8, 11, OWNER2);

        assertTrue("conflicting locks not detected", lock1.conflicts(lock2));
        assertTrue("conflicting locks not detected", lock2.conflicts(lock1));
    }

    @Test
    public void testConficlintgSameOwner() {
        Lock lock1 = new Lock(7, 10, OWNER1);
        Lock lock2 = new Lock(8, 11, OWNER1);

        assertFalse("same owner not respected", lock1.conflicts(lock2));
        assertFalse("same owner not respected", lock2.conflicts(lock1));
    }

    @Test
    public void testConficlintgSameDifferentOwner() {
        Lock lock1 = new Lock(8, 11, OWNER1);
        Lock lock2 = new Lock(8, 11, OWNER2);

        assertTrue("conflicting locks not detected", lock1.conflicts(lock2));
        assertTrue("conflicting locks not detected", lock2.conflicts(lock1));
    }

    @Test
    public void testNonConficlintg() {
        Lock lock1 = new Lock(7, 10, OWNER1);
        Lock lock2 = new Lock(11, 15, OWNER2);

        assertFalse("not overlapping locks conflicted", lock1.conflicts(lock2));
        assertFalse("not overlapping locks conflicted", lock2.conflicts(lock1));
    }

    @Test
    public void testWholeFile() {
        Lock lock1 = new Lock(0, -1, OWNER1);
        Lock lock2 = new Lock(11, 15, OWNER2);

        assertTrue("conflicting locks not detected", lock1.conflicts(lock2));
        assertTrue("conflicting locks not detected", lock2.conflicts(lock1));
    }

    @Test
    public void testEquals() {
        Lock lock1 = new Lock(7, 10, OWNER1);
        Lock lock2 = new Lock(11, 15, OWNER2);
        Lock lock3 = new Lock(11, 15, OWNER3);
        Lock lock4 = new Lock(7, 10, OWNER1);
        Lock lock5 = new Lock(7, 12, OWNER1);

        assertTrue(lock1.equals(lock4));
        assertFalse(lock1.equals(lock2));
        assertFalse(lock2.equals(lock3));
        assertFalse(lock1.equals(lock5));
    }

}
