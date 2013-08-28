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
package org.dcache.nfs.v4.proxy;

import com.sun.security.auth.module.UnixSystem;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;
import org.dcache.nfs.nfsstat;
import org.dcache.nfs.v4.client.CompoundBuilder;
import org.dcache.nfs.v4.xdr.COMPOUND4args;
import org.dcache.nfs.v4.xdr.COMPOUND4res;
import org.dcache.nfs.v4.xdr.clientid4;
import org.dcache.nfs.v4.xdr.nfs4_prot;
import org.dcache.nfs.v4.xdr.nfs_opnum4;
import org.dcache.nfs.v4.xdr.sequenceid4;
import org.dcache.nfs.v4.xdr.state_protect_how4;

import org.dcache.xdr.IpProtocolType;
import org.dcache.xdr.OncRpcClient;
import org.dcache.xdr.OncRpcException;
import org.dcache.xdr.RpcAuth;
import org.dcache.xdr.RpcAuthTypeUnix;
import org.dcache.xdr.RpcCall;
import org.dcache.xdr.XdrTransport;

public class EmbeddedV4Client {

    private final RpcCall client;
    private final OncRpcClient rpcClient;

    private long _lastUpdate = -1;
    private sequenceid4 _sequenceID;
    private clientid4 _clientIdByServer;
    private boolean _isMDS = false;
    private boolean _isDS = false;

    public EmbeddedV4Client(InetSocketAddress address) throws IOException {

        rpcClient = new OncRpcClient(address, IpProtocolType.TCP);
        XdrTransport transport;
        transport = rpcClient.connect();

        UnixSystem unix = new UnixSystem();
        RpcAuth credential = new RpcAuthTypeUnix(
                (int) unix.getUid(),
                (int) unix.getGid(),
                new int[]{(int) unix.getGid()},
                (int) (System.currentTimeMillis() / 1000),
                InetAddress.getLocalHost().getHostName());

        client = new RpcCall(100003, 4, credential, transport);
    }

    public void exchange_id(String domain) throws OncRpcException, IOException {

        String name = "dCache.ORG java based client";

        COMPOUND4args args = new CompoundBuilder()
                .withExchangeId(domain, name, UUID.randomUUID().toString(), 0, state_protect_how4.SP4_NONE)
                .withTag("exchange_id")
                .build();

        COMPOUND4res compound4res = sendCompound(args);

        if (compound4res.resarray.get(0).opexchange_id.eir_resok4.eir_server_impl_id.length > 0) {
            String serverId = compound4res.resarray.get(0).opexchange_id.eir_resok4.eir_server_impl_id[0].nii_name.toString();
            System.out.println("Connected to: " + serverId);
        } else {
            System.out.println("Connected to: Mr. X");
        }

        _clientIdByServer = compound4res.resarray.get(0).opexchange_id.eir_resok4.eir_clientid;
        _sequenceID = compound4res.resarray.get(0).opexchange_id.eir_resok4.eir_sequenceid;

        if ((compound4res.resarray.get(0).opexchange_id.eir_resok4.eir_flags.value
                & nfs4_prot.EXCHGID4_FLAG_USE_PNFS_MDS) != 0) {
            _isMDS = true;
        }

        if ((compound4res.resarray.get(0).opexchange_id.eir_resok4.eir_flags.value
                & nfs4_prot.EXCHGID4_FLAG_USE_PNFS_DS) != 0) {
            _isDS = true;
        }

        System.out.println("pNFS MDS: " + _isMDS);
        System.out.println("pNFS  DS: " + _isDS);
    }

    private COMPOUND4res sendCompound(COMPOUND4args compound4args)
            throws OncRpcException, IOException {

        COMPOUND4res compound4res = new COMPOUND4res();
        client.call(nfs4_prot.NFSPROC4_COMPOUND_4, compound4args, compound4res);

        processSequence(compound4res);

        return compound4res;
    }

    public void processSequence(COMPOUND4res compound4res) {

        if (compound4res.resarray.get(0).resop == nfs_opnum4.OP_SEQUENCE && compound4res.resarray.get(0).opsequence.sr_status == nfsstat.NFS_OK) {
            _lastUpdate = System.currentTimeMillis();
            ++_sequenceID.value.value;
        }
    }
}
