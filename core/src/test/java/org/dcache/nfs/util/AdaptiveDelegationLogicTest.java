package org.dcache.nfs.util;

import static org.junit.Assert.*;
import org.dcache.nfs.vfs.Inode;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

public class AdaptiveDelegationLogicTest {

    public static final int EVICTION_QUEUE_CAPACITY = 10;
    public static final int ACTIVE_QUEUE_CAPACITY = 10;

    private AdaptiveDelegationLogic logic;
    private ManualClock clock;


    @Before
    public void setUp() {
        clock = new ManualClock();
        logic = new AdaptiveDelegationLogic(ACTIVE_QUEUE_CAPACITY, EVICTION_QUEUE_CAPACITY, Duration.ofSeconds(10), clock);
    }

    @Test
    public void shouldDelegateReturnsFalseForFirstTime() {
        Inode inode = Inode.forFile("file1".getBytes());
        assertFalse("File in eviction queue should not be delegated", logic.shouldDelegate(inode));
    }

    @Test
    public void shouldDelegateReturnsTrueForActiveQueueFile() {
        Inode inode = Inode.forFile("file1".getBytes());
        logic.shouldDelegate(inode); // Add to eviction queue
        logic.shouldDelegate(inode); // Move to active queue
        assertTrue("File in active queue should be delegated", logic.shouldDelegate(inode));
    }

    @Test
    public void isActiveReturnsTrueForActiveQueueFile() {
        Inode inode = Inode.forFile("file4".getBytes());
        logic.shouldDelegate(inode); // Add to eviction queue
        logic.shouldDelegate(inode); // Move to active queue
        assertTrue("File in active queue should be active", logic.isInActive(inode));
    }

    @Test
    public void isActiveReturnsFalseForEvictionQueueFile() {
        Inode inode = Inode.forFile("file5".getBytes());
        logic.shouldDelegate(inode); // Add to eviction queue
        assertFalse("File in eviction queue should not be active", logic.isInActive(inode));
    }

    @Test
    public void isInEvictionQueueReturnsTrueForEvictionQueueFile() {
        Inode inode = Inode.forFile("file6".getBytes());
        logic.shouldDelegate(inode); // Add to eviction queue
        assertTrue("File in eviction queue should be detected", logic.isInEvictionQueue(inode));
    }

    @Test
    public void isInEvictionQueueReturnsFalseForActiveQueueFile() {
        Inode inode = Inode.forFile("file7".getBytes());
        logic.shouldDelegate(inode); // Add to eviction queue
        logic.shouldDelegate(inode); // Move to active queue
        assertFalse("File in active queue should not be in eviction queue", logic.isInEvictionQueue(inode));
    }

    @Test
    public void activeQueueEvictsLeastRecentlyUsedFileToEvictionQueue() {
        Inode inode1 = Inode.forFile("file".getBytes());

        logic.shouldDelegate(inode1); // Add to eviction queue
        logic.shouldDelegate(inode1); // Add to active queue

        // trigger eviction from active queue
        for (int i = 0; i < ACTIVE_QUEUE_CAPACITY; i++) {
            // call twice to trigger move to active queue
            logic.shouldDelegate(Inode.forFile(("file" + (i + 10)).getBytes())); // Fill eviction queue
            logic.shouldDelegate(Inode.forFile(("file" + (i + 10)).getBytes())); // Fill eviction queue
        }

        assertTrue("File should be in eviction queue", logic.isInEvictionQueue(inode1));
    }

    @Test
    public void shouldDiscardExpiredEntries() {
        Inode inode1 = Inode.forFile("file".getBytes());

        logic.shouldDelegate(inode1); // Add to eviction queue
        logic.shouldDelegate(inode1); // Add to active queue

        clock.advance(Duration.ofSeconds(11));

        // trigger eviction from active queue
        for (int i = 0; i < ACTIVE_QUEUE_CAPACITY; i++) {
            // call twice to trigger move to active queue
            logic.shouldDelegate(Inode.forFile(("file" + (i + 10)).getBytes())); // Fill eviction queue
            logic.shouldDelegate(Inode.forFile(("file" + (i + 10)).getBytes())); // Fill eviction queue
        }

        assertFalse("Expired entry should not be in active queue", logic.isInActive(inode1));
        assertFalse("Expired entry should not be in eviction queue", logic.isInEvictionQueue(inode1));

    }


    @Test
    public void shouldMoveToEvictionQueueIfIdle() {
        Inode inode1 = Inode.forFile("file".getBytes());

        logic.shouldDelegate(inode1); // Add to eviction queue
        logic.shouldDelegate(inode1); // Add to active queue

        clock.advance(Duration.ofSeconds(11));

        logic.shouldDelegate(inode1); // Move to eviction queue as being idle

        assertTrue("File should be in eviction queue", logic.isInEvictionQueue(inode1));
    }


    @Test
    public void clearRemovesAllEntriesFromQueues() {
        Inode inode = Inode.forFile("file14".getBytes());
        logic.shouldDelegate(inode); // Add to eviction queue
        logic.shouldDelegate(inode); // Move to active queue
        logic.reset();
        assertFalse("Active queue should be empty after clear", logic.isInActive(inode));
        assertFalse("Eviction queue should be empty after clear", logic.isInEvictionQueue(inode));
    }

}