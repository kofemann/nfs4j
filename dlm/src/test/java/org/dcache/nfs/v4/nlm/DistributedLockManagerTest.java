package org.dcache.nfs.v4.nlm;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.dcache.nfs.v4.StateOwner;
import org.dcache.nfs.v4.xdr.clientid4;
import org.dcache.nfs.v4.xdr.nfs_lock_type4;
import org.dcache.nfs.v4.xdr.state_owner4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

public class DistributedLockManagerTest  {

    private HazelcastInstance hzSerrver;
    private HazelcastInstance hzClient;
    private LockManager lm1;
    private LockManager lm2;

    @Before
    public void setUp() throws Exception {

        hzSerrver = Hazelcast.newHazelcastInstance();

        ClientConfig config = new ClientConfig();
        config.getNetworkConfig().setAddresses(Arrays.asList(

                hzSerrver.getCluster().getLocalMember().getAddress().getHost() +
                        ":" + hzSerrver.getCluster().getLocalMember().getAddress().getPort()));

        hzClient = HazelcastClient.newHazelcastClient(config);
        lm1 = new DistributedLockManager(hzClient, "distributed-byte-range-lock");
        lm2 = new DistributedLockManager(hzClient, "distributed-byte-range-lock");
    }


    @Test(expected = LockDeniedException.class)
    public void shouldFailOnConflictingLockDifferentOwner() throws LockException {
        given().owner("owner1")
                .on(lm1)
                .on("file1")
                .from(0)
                .length(1)
                .read()
                .lock();

        given().owner("owner2")
                .on(lm2)
                .on("file1")
                .from(0)
                .length(1)
                .write()
                .lock();
    }

    @Test
    public void shouldAllowConflictingLockSameOwner() throws LockException {
        given().owner("owner1")
                .on(lm1)
                .on("file1")
                .from(0)
                .length(1)
                .read()
                .lock();

        given().owner("owner1")
                .on(lm2)
                .on("file1")
                .from(0)
                .length(1)
                .write()
                .lock();

        assertLocked();
    }

    private void assertLocked() {
        try {
            given().on(lm2)
                    .on("file1")
                    .from(0)
                    .length(-1)
                    .write()
                    .test();
            fail("Not locked in lm2");
        } catch (LockException e) {
            // pass
        }

        try {
            given().on(lm1)
                    .on("file1")
                    .from(0)
                    .length(-1)
                    .write()
                    .test();
            fail("Not locked in lm1");
        } catch (LockException e) {
            // pass
        }
    }

    @After
    public void tearDown() {
        if (hzClient != null) {
            hzClient.shutdown();
        }
        if (hzSerrver != null) {
            hzSerrver.shutdown();
        }
    }

    private LockBuilder given() {
        return new LockBuilder();
    }

    private class LockBuilder {

        private LockManager nlm;
        private byte[] file;
        private long offset;
        private long length;
        private StateOwner owner;
        private int lockType;

        LockBuilder on(String file) {
            this.file = file.getBytes(StandardCharsets.UTF_8);
            return this;
        }

        LockBuilder on(LockManager nlm) {
            this.nlm = nlm;
            return this;
        }

        LockBuilder owner(String owner) {
            state_owner4 so = new state_owner4();

            so.owner = owner.getBytes(StandardCharsets.UTF_8);
            so.clientid = new clientid4(1);
            this.owner = new StateOwner(so, 1);
            return this;
        }

        LockBuilder from(long offset) {
            this.offset = offset;
            return this;
        }

        LockBuilder length(long length) {
            this.length = length;
            return this;
        }

        LockBuilder read() {
            this.lockType = nfs_lock_type4.READ_LT;
            return this;
        }

        LockBuilder write() {
            this.lockType = nfs_lock_type4.WRITE_LT;
            return this;
        }

        void lock() throws LockException {
            NlmLock lock = new NlmLock(owner, lockType, offset, length);
            nlm.lock(file, lock);
        }

        void unlock() throws LockException {
            NlmLock lock = new NlmLock(owner, lockType, offset, length);
            nlm.unlock(file, lock);
        }

        void test() throws LockException {
            NlmLock lock = new NlmLock(owner, lockType, offset, length);
            nlm.test(file, lock);
        }

    }
}
