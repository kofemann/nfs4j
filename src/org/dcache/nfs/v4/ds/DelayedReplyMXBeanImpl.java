package org.dcache.nfs.v4.ds;

import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

/**
 *
 * @author tigran
 */
public class DelayedReplyMXBeanImpl implements DelayedReplyMXBean {

    private volatile long delay;

    public DelayedReplyMXBeanImpl(String name) {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
            String beanName = String.format("%s:type=DelayedReply,name=%s",
                    DelayedReplyMXBeanImpl.class.getPackage().getName(), name);
            ObjectName mxBeanName = new ObjectName(beanName);
            if (!server.isRegistered(mxBeanName)) {
                server.registerMBean(this, new ObjectName(beanName));
            }
        } catch (MalformedObjectNameException |
                InstanceAlreadyExistsException |
                MBeanRegistrationException |
                NotCompliantMBeanException ex) {
            // NOP
        }
    }


    @Override
    public long getDelay() {
        return delay;
    }

    @Override
    public void delay(long delay) {
        this.delay = delay;
    }
}
