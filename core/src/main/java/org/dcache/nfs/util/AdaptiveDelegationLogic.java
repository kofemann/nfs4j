/*
 * Copyright (c) 2025 Deutsches Elektronen-Synchroton,
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
package org.dcache.nfs.util;


import com.google.common.annotations.VisibleForTesting;
import org.dcache.nfs.vfs.Inode;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <pre>
///
///  Adaptive Delegation Logic for NFSv4 file delegation heuristic.
///
///  This system uses two queues to track file access patterns:
///    - Eviction Queue: Tracks files that have been accessed only once
///    - Active Queue: Contains files that have been accessed multiple times
///
///
///  The algorithm works as follows:
///
///    1. When a file is accessed for the first time, it is added to the Eviction Queue
///    2. If a file in the Eviction Queue is accessed again, it is moved to the Active Queue
///    3. If a file in the Active Queue is accessed again, the system recommends delegation
///    4. If a file in the Active Queue is not accessed for a specified idle time, it is moved to the Eviction Queue
///    5. When active queue reaches capacity, the least recently used (LRU) file is pushed into eviction queue
///    6. When evicted from active queue entry is not accessed for a specified idle time, entry evicted
///    7. When eviction queue reaches capacity, the least recently used (LRU) file is evicted
///
///
///  Uses LinkedHashMap to maintain insertion order and LRU eviction policy.
///
///
///
///                                                  +---------------+
///                                                  |  File Access  |
///                                                  +-------+-------+
///                                                          |
///                                                          v
///                                                 +--------------------+
///                                                 | Is in Active Queue?|
///                                                 +--------+-----------+
///                                                          |
///                                      +-------------------+-------------------+
///                                      | No                                Yes |
///                                      |                                       |
///                                      v                                       v
///                          +---------------------+                   +---------------------+
///                          |Is in Eviction Queue?|                   | is Idletime Excided?|
///                          +---------+-----------+                   +---------------------+
///                                    |                                          |
///                     +--------------|---------------+              +----------------------+
///                     |                              |              | Yes               No |
///                     | Yes                          | No           |                      |
///                     v                              v              |                      V
///         +--------------------+          +----------------------+  |             +-------------------+
///         |Move to Active Queue|          |Add to Eviction Queue | <|             | Offer Delegation  |
///         +--------------------+          +----------+-----------+                +-------------------+
///                     |                              |
///                     v                              v
///             +---------------+              +---------------+
///             |Is A.Q. Full?  |              |Is E.Q. Full?  |
///             +-------+-------+              +-------+-------+
///                     |                              |
///          +----------+----------+        +----------+----------+
///          |                     |  No    |                     |
///          | Yes                 +--------+                Yes  |
///          v                          |                         v
///  +---------------+                  v                 +---------------+
///  |LRU  to E.Q.   |             +--------+             |LRU from E.Q.  |
///  +---------------+             |  Noop  |             +---------------+
///                                +--------+
///
///
 </pre>
*/

public class AdaptiveDelegationLogic {


    /**
     * Maximum size of eviction queue.
     */
    private final int maxEvictionQueueSize;

    /**
     * Maximum size of active queue.
     */
    private final int maxActiveQueueSize;


    /**
     * Eviction Queue for files accessed once.
     */
    private final LinkedHashMap<Inode, Instant> evictionQueue;

    /**
     * Active Queue for files accessed multiple times.
     */
    private final LinkedHashMap<Inode, Instant> activeQueue;

    /**
     * Maximum idle time for files in the active queue.
     * Files that exceed this time will be discarded by-passing eviction queue.
     */
    private final Duration maxIdleTime;


    /**
     * Time source for generating timestamps.
     * This allows for easier testing and mocking of time-related functionality.
     */
    private final Clock clock;

    /**
     * Creates a new instance of DelegationQueueSystem
     *
     * @param maxActiveQueueSize   Maximum size of active queue.
     * @param maxEvictionQueueSize Maximum size of eviction queue.
     * @param maxIdleTime Maximum idle time for files in the active queue.
     */
    public AdaptiveDelegationLogic(int maxActiveQueueSize, int maxEvictionQueueSize, Duration maxIdleTime) {
        this(maxActiveQueueSize, maxEvictionQueueSize, maxIdleTime, Clock.systemDefaultZone());
    }

    /**
     * Creates a new instance of DelegationQueueSystem. For internal use only.
     *
     * @param maxActiveQueueSize   Maximum size of active queue.
     * @param maxEvictionQueueSize Maximum size of eviction queue.
     * @param maxIdleTime Maximum idle time for files in the active queue.
     * @param clock Clock to use for timestamp generation, for testing purposes.
     */
    @VisibleForTesting
    AdaptiveDelegationLogic(int maxActiveQueueSize, int maxEvictionQueueSize, Duration maxIdleTime, Clock clock) {
        this.maxActiveQueueSize = maxActiveQueueSize;
        this.maxEvictionQueueSize = maxEvictionQueueSize;
        this.maxIdleTime = maxIdleTime;
        this.clock = clock;

        // Initialize eviction queue with LRU eviction policy
        this.evictionQueue = new LinkedHashMap<>(maxEvictionQueueSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Inode, Instant> eldest) {
                return size() > maxEvictionQueueSize;
            }
        };

        // Initialize active queue with LRU eviction policy
        this.activeQueue = new LinkedHashMap<>(maxActiveQueueSize, 0.75f, true);
    }

    /**
     * Check if a file should be delegated based on access frequency.
     *
     * @param inode The file to check
     * @return true if the file should be delegated, false otherwise
     */
    public synchronized boolean shouldDelegate(Inode inode) {
        var currentTime = Instant.now(clock);

        // Case 1: File is in Active Queue - offer delegation
        var lastAccessTime = activeQueue.get(inode);
        if (lastAccessTime != null) {

            // Check if the file has been idle for too long
            if (lastAccessTime.plus(maxIdleTime).isBefore(currentTime)) {
                // Move to Eviction Queue
                evictionQueue.put(inode, lastAccessTime);
                activeQueue.remove(inode);
                return false;
            }

            // Update access time
            activeQueue.put(inode, currentTime);
            return true;
        }

        // Case 2: File is in Eviction Queue - move to Active Queue
        if (evictionQueue.containsKey(inode)) {
            // Remove from Eviction Queue
            evictionQueue.remove(inode);
            // Add to Active Queue
            activeQueue.put(inode, currentTime);

            // Move the least recently used file from Active Queue into Eviction Queue or drop, if too old
            if (activeQueue.size() > maxActiveQueueSize) {

                var eldestEntry = activeQueue.entrySet().iterator().next();
                if (eldestEntry.getValue().plus(maxIdleTime).isAfter(currentTime)) {
                    evictionQueue.put(eldestEntry.getKey(), eldestEntry.getValue());
                }

                activeQueue.remove(eldestEntry.getKey());
            }
        } else {
            // Case 3: File is not in any queue - add to Eviction Queue
            evictionQueue.put(inode, currentTime);
        }
        return false;
    }

    /**
     * Check if a file is in the active queue
     *
     * @param inode The file to check
     * @return true if the file is in the active queue, false otherwise
     */
    @VisibleForTesting
    synchronized boolean isInActive(Inode inode) {
        return activeQueue.containsKey(inode);
    }

    /**
     * Check if a file is in the eviction queue
     *
     * @param inode The file to check
     * @return true if the file is in the eviction queue, false otherwise
     */
    @VisibleForTesting
    synchronized boolean isInEvictionQueue(Inode inode) {
        return evictionQueue.containsKey(inode);
    }

    /**
     * Reset all queues
     */
    public synchronized void reset() {
        evictionQueue.clear();
        activeQueue.clear();
    }
}