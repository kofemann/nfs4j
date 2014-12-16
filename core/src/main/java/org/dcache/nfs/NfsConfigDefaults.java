/*
 * Copyright (c) 2009 - 2014 Deutsches Elektronen-Synchroton,
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

public class NfsConfigDefaults extends NfsConfig {

    private static final int NFS4_LEASE_TIME = 90;
    private static final int NFS4_MAXFILENAME = 255;

    /**
     * maximal read/write buffer size.
     */
    private static final long NFS4_MAXIOBUFFERSIZE = 1024*1024*4;

    // theoretically, there is no limit on file size
    private static final long NFS4_MAXFILESIZE = Long.MAX_VALUE;

    /**
     * max link count
     */
    private static final int NFS4_MAXLINK = 255;

    // setting the stripe size
    private static final int NFS4_STRIPE_SIZE = (int)NFS4_MAXIOBUFFERSIZE;


    /**
     * NFSv4.1 implementation ID
     */
    private static final String NFS4_IMPLEMENTATION_ID = "Chimera NFSv4.1";

    /**
     * NFSv4.1 implementation domain
     */
    private static final String NFS4_IMPLEMENTATION_DOMAIN = "dCache.ORG";

    /**
     * Maximal number of operations in a compound call
     */
    private static final int NFS4_MAX_OPS = 128;

    /**
     * Maximal number of session slots
     */
    private static final int NFS4_MAX_SESSION_SLOTS = 16;

    @Override
    public void setMaxSesseionSlots(int maxSesseionSlots) {
        throw new UnsupportedOperationException("Not allowed");
    }

    @Override
    public int getMaxSesseionSlots() {
        return NFS4_MAX_SESSION_SLOTS;
    }

    @Override
    public void setMaxRequestOps(int maxRequestOps) {
        throw new UnsupportedOperationException("Not allowed");
    }

    @Override
    public int getMaxRequestOps() {
        return NFS4_MAX_OPS;
    }

    @Override
    public void setImplementationDomain(String implementationDomain) {
        throw new UnsupportedOperationException("Not allowed");
    }

    @Override
    public String getImplementationDomain() {
        return NFS4_IMPLEMENTATION_DOMAIN;
    }

    @Override
    public void setImplementationId(String implementationId) {
        throw new UnsupportedOperationException("Not allowed");
    }

    @Override
    public String getImplementationId() {
        return NFS4_IMPLEMENTATION_ID;
    }

    @Override
    public void setStripeSize(int stripeSize) {
        throw new UnsupportedOperationException("Not allowed");
    }

    @Override
    public int getStripeSize() {
        return NFS4_STRIPE_SIZE;
    }

    @Override
    public void setMaxLinkCount(int maxLinkCount) {
        throw new UnsupportedOperationException("Not allowed");
    }

    @Override
    public int getMaxLinkCount() {
        return NFS4_MAXLINK;
    }

    @Override
    public void setMaxFileSize(long maxFileSize) {
        throw new UnsupportedOperationException("Not allowed");
    }

    @Override
    public long getMaxFileSize() {
        return NFS4_MAXFILESIZE;
    }

    @Override
    public long getMaxReadSize() {
        return NFS4_MAXIOBUFFERSIZE;
    }

    @Override
    public void setMaxReadSize(long maxReadSize) {
        throw new UnsupportedOperationException("Not allowed");
    }

    @Override
    public long getMaxWriteSize() {
        return NFS4_MAXIOBUFFERSIZE;
    }

    @Override
    public void setMaxWriteSize(long maxWriteSize) {
        throw new UnsupportedOperationException("Not allowed");
    }

    @Override
    public void setMaxFileName(int maxFileName) {
        throw new UnsupportedOperationException("Not allowed");
    }

    @Override
    public int getMaxFileName() {
        return NFS4_MAXFILENAME;
    }

    @Override
    public void setLeaseTime(int leaseTime) {
        throw new UnsupportedOperationException("Not allowed");
    }

    @Override
    public int getLeaseTime() {
        return NFS4_LEASE_TIME;
    }

}
