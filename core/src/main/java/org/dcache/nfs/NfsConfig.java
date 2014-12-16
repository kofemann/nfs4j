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

public class NfsConfig {

    private int leaseTime;
    private int maxFileName;
    private long maxReadSize;
    private long maxWriteSize;
    private long maxFileSize;
    private int maxLinkCount;
    private int stripeSize;
    private String implementationId;
    private String implementationDomain;
    private int maxRequestOps;
    private int maxSesseionSlots;

    public static final NfsConfig DEFAULT = new NfsConfigDefaults();

    public int getLeaseTime() {
        return leaseTime;
    }

    public void setLeaseTime(int leaseTime) {
        this.leaseTime = leaseTime;
    }

    public int getMaxFileName() {
        return maxFileName;
    }

    public void setMaxFileName(int maxFileName) {
        this.maxFileName = maxFileName;
    }

    public long getMaxReadSize() {
        return maxReadSize;
    }

    public void setMaxReadSize(long maxReadSize) {
        this.maxReadSize = maxReadSize;
    }

    public long getMaxWriteSize() {
        return maxWriteSize;
    }

    public void setMaxWriteSize(long maxWriteSize) {
        this.maxWriteSize = maxWriteSize;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public int getMaxLinkCount() {
        return maxLinkCount;
    }

    public void setMaxLinkCount(int maxLinkCount) {
        this.maxLinkCount = maxLinkCount;
    }

    public int getStripeSize() {
        return stripeSize;
    }

    public void setStripeSize(int stripeSize) {
        this.stripeSize = stripeSize;
    }

    public String getImplementationId() {
        return implementationId;
    }

    public void setImplementationId(String implementationId) {
        this.implementationId = implementationId;
    }

    public String getImplementationDomain() {
        return implementationDomain;
    }

    public void setImplementationDomain(String implementationDomain) {
        this.implementationDomain = implementationDomain;
    }

    public int getMaxRequestOps() {
        return maxRequestOps;
    }

    public void setMaxRequestOps(int maxRequestOps) {
        this.maxRequestOps = maxRequestOps;
    }

    public int getMaxSesseionSlots() {
        return maxSesseionSlots;
    }

    public void setMaxSesseionSlots(int maxSesseionSlots) {
        this.maxSesseionSlots = maxSesseionSlots;
    }
}
