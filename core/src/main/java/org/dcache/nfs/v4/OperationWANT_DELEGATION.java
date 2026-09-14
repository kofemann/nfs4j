/*
 * Copyright (c) 2009 - 2026 Deutsches Elektronen-Synchroton,
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
package org.dcache.nfs.v4;

import java.io.IOException;

import org.dcache.nfs.ChimeraNFSException;
import org.dcache.nfs.FsExport;
import org.dcache.nfs.nfsstat;
import org.dcache.nfs.status.AccessException;
import org.dcache.nfs.status.InvalException;
import org.dcache.nfs.v4.FileTracker.DelegationState;
import org.dcache.nfs.v4.xdr.WANT_DELEGATION4res;
import org.dcache.nfs.v4.xdr.acetype4;
import org.dcache.nfs.v4.xdr.aceflag4;
import org.dcache.nfs.v4.xdr.acemask4;
import org.dcache.nfs.v4.xdr.nfs4_prot;
import org.dcache.nfs.v4.xdr.nfs_argop4;
import org.dcache.nfs.v4.xdr.nfs_opnum4;
import org.dcache.nfs.v4.xdr.nfs_resop4;
import org.dcache.nfs.v4.xdr.nfsace4;
import org.dcache.nfs.v4.xdr.open_claim_type4;
import org.dcache.nfs.v4.xdr.open_delegation4;
import org.dcache.nfs.v4.xdr.open_delegation_type4;
import org.dcache.nfs.v4.xdr.open_none_delegation4;
import org.dcache.nfs.v4.xdr.open_read_delegation4;
import org.dcache.nfs.v4.xdr.utf8str_mixed;
import org.dcache.nfs.v4.xdr.why_no_delegation4;
import org.dcache.nfs.vfs.Inode;
import org.dcache.nfs.vfs.Stat;
import org.dcache.oncrpc4j.rpc.OncRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperationWANT_DELEGATION extends AbstractNFSv4Operation {

    private static final Logger _log = LoggerFactory.getLogger(OperationWANT_DELEGATION.class);

    public OperationWANT_DELEGATION(nfs_argop4 args) {
        super(args, nfs_opnum4.OP_WANT_DELEGATION);
    }

    @Override
    public void process(CompoundContext context, nfs_resop4 result) throws ChimeraNFSException, IOException,
            OncRpcException {

        WANT_DELEGATION4res res = result.opwant_delegation;

        // WANT_DELEGATION is an NFSv4.1 operation.
        if (context.getMinorversion() <= 0) {
            res.wdr_status = nfsstat.NFSERR_NOTSUPP;
            return;
        }

        int want = _args.opwant_delegation.wda_want.value;

        // The client must indicate an explicit desire for a delegation; WANT_NO_DELEG is ignored.
        if ((want & nfs4_prot.OPEN4_SHARE_ACCESS_WANT_DELEG_MASK & ~nfs4_prot.OPEN4_SHARE_ACCESS_WANT_NO_DELEG) == 0) {
            throw new InvalException("no delegation desired");
        }

        // Only CLAIM_FH is supported; delegation reclaim (CLAIM_PREVIOUS, CLAIM_DELEG_PREV_FH) is not.
        if (_args.opwant_delegation.wda_claim.dc_claim != open_claim_type4.CLAIM_FH) {
            res.wdr_status = nfsstat.NFSERR_NOTSUPP;
            return;
        }

        final Inode inode = context.currentInode();
        Stat stat = context.getFs().getattr(inode);
        if (stat.type() == Stat.Type.DIRECTORY) {
            none(res, why_no_delegation4.WND4_IS_DIR);
            return;
        }

        boolean allowDelegations = isDelegationAllowed(context, inode);
        DelegationState delegation = context.getStateHandler().getFileTracker()
                .wantDelegation(context.getSession().getClient(), inode, want, allowDelegations);

        if (delegation != null) {
            res.wdr_status = nfsstat.NFS_OK;
            open_delegation4 delegationInfo = new open_delegation4();
            delegationInfo.delegation_type = open_delegation_type4.OPEN_DELEGATE_READ;
            delegationInfo.read = new open_read_delegation4();
            delegationInfo.read.stateid = delegation.delegationStateid().stateid();
            delegationInfo.read.permissions = new nfsace4();
            delegationInfo.read.permissions.type = new acetype4(nfs4_prot.ACE4_ACCESS_ALLOWED_ACE_TYPE);
            delegationInfo.read.permissions.flag = new aceflag4(0);
            delegationInfo.read.permissions.access_mask = new acemask4(nfs4_prot.ACCESS4_READ);
            delegationInfo.read.permissions.who = new utf8str_mixed(context.getPrincipal().getName());
            res.wdr_resok4 = delegationInfo;
        } else {
            none(res, why_no_delegation4.WND4_CONTENTION);
        }
    }

    private void none(WANT_DELEGATION4res res, int why) {
        res.wdr_status = nfsstat.NFS_OK;
        open_delegation4 delegation = new open_delegation4();
        delegation.delegation_type = open_delegation_type4.OPEN_DELEGATE_NONE_EXT;
        delegation.od_whynone = new open_none_delegation4();
        delegation.od_whynone.ond_why = why;
        delegation.od_whynone.ond_server_will_push_deleg = false;
        delegation.od_whynone.ond_server_will_signal_avail = false;
        res.wdr_resok4 = delegation;
    }

    private boolean isDelegationAllowed(CompoundContext context, Inode inode) throws ChimeraNFSException {

        // a pNFS data server runs without an export table, thus there is nothing to consult.
        if (context.getExportTable() == null) {
            return true;
        }

        FsExport export = context.getExportTable()
                .getExport(inode.exportIndex(), context.getRemoteSocketAddress().getAddress());
        if (export == null) {
            throw new AccessException("no export");
        }

        return export.isWithDelegations();
    }
}
