package org.dcache.nfs.v4.ds;

public interface DelayedReplyMXBean {

    long getDelay();

    void delay(long delay);
}
