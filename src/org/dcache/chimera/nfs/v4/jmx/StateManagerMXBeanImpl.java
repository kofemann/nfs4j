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
package org.dcache.chimera.nfs.v4.jmx;

import java.lang.management.ManagementFactory;
import java.util.List;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.dcache.chimera.nfs.ChimeraNFSException;
import org.dcache.chimera.nfs.v4.NFS4Client;
import org.dcache.chimera.nfs.v4.NFSv41Session;
import org.dcache.chimera.nfs.v4.NFSv4StateHandler;
import org.dcache.chimera.nfs.v4.xdr.sessionid4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateManagerMXBeanImpl implements StateManagerMXBean {

    private final static Logger _log = LoggerFactory.getLogger(StateManagerMXBeanImpl.class);
    private final NFSv4StateHandler _stateManager;

    public StateManagerMXBeanImpl(NFSv4StateHandler stateManager) {
        _stateManager = stateManager;
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
            String name = String.format("%s:type=StateManager,name=%s",
                    _stateManager.getClass().getPackage().getName(), "StateManager");
            ObjectName mxBeanName = new ObjectName(name);
            if (!server.isRegistered(mxBeanName)) {
                server.registerMBean(this, new ObjectName(name));
            }
        } catch (MalformedObjectNameException ex) {
            _log.error(ex.getMessage(), ex);
        } catch (InstanceAlreadyExistsException ex) {
            _log.error(ex.getMessage(), ex);
        } catch (MBeanRegistrationException ex) {
           _log.error(ex.getMessage(), ex);
        } catch (NotCompliantMBeanException ex) {
            _log.error(ex.getMessage(), ex);
        }
    }

    @Override
    public String[] getClients() {
        List<NFS4Client> clients = _stateManager.getClients();

        String[] reply = new String[clients.size()];
        for(int i = 0; i < reply.length; i++) {
            NFS4Client client = clients.get(i);
            StringBuilder sb = new StringBuilder();
            sb.append("id=").append(Long.toHexString(client.getId())).append('\n');
            sb.append("   addr=").append(client.getRemoteAddress().getAddress().getHostName()).append('\n');
            sb.append("   states#=").append(client.getStatesCount()).append('\n');
            sb.append("   sessions:").append('\n');
            for(NFSv41Session session: client.sessions()) {
                sb.append("      ").append(session).append('\n');
            }
            reply[i] = sb.toString();
        }
        return reply;
    }

    @Override
    public void destroy_session(String session) {
        byte[] sessionId = hexStringToByteArray(session);
        try {
            _stateManager.removeSessionById( new sessionid4(sessionId));
        }catch (ChimeraNFSException ignored) {}
    }

    @Override
    public void destory_cleint(String client) {
        long clientid = Long.parseLong(client, 16);
        try {
            NFS4Client nfsClient = _stateManager.getClientByID(clientid);
            _stateManager.removeClient(nfsClient);
        } catch (ChimeraNFSException ignored) {}
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
