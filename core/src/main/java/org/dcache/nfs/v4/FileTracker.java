/*
 * Copyright (c) 2016 Deutsches Elektronen-Synchroton,
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.dcache.nfs.ChimeraNFSException;
import org.dcache.nfs.status.BadStateidException;
import org.dcache.nfs.status.ShareDeniedException;
import org.dcache.nfs.v4.xdr.stateid4;
import org.dcache.nfs.vfs.Inode;
import org.dcache.utils.Opaque;

/**
 * A class which tracks open files.
 */
public class FileTracker {

    private final Map<Opaque, List<OpenState>> files = new ConcurrentHashMap<>();

    private static class OpenState {

        private final NFS4Client client;
        private final stateid4 stateid;
        private final int shareAccess;
        private final int shareDeny;

        public OpenState(NFS4Client client, stateid4 stateid, int shareAccess, int shareDeny) {
            this.client = client;
            this.stateid = stateid;
            this.shareAccess = shareAccess;
            this.shareDeny = shareDeny;
        }

        public stateid4 getStateid() {
            return stateid;
        }

        public int getShareAccess() {
            return shareAccess;
        }

        public int getShareDeny() {
            return shareDeny;
        }
    }

    /**
     * Add a new open to the list of open files. If provided {@code shareAccess}
     * and {@code shareDeny} conflicts with existing opens, @{link ShareDeniedException}
     * exception will be thrown.
     * @param client nfs client performing the open operation.
     * @param inode of opened file.
     * @param shareAccess type of access required.
     * @param shareDeny type of access to deny others.
     * @return stateid associated with open.
     * @throws ShareDeniedException if share reservation conflicts with an existing open.
     * @throws ChimeraNFSException
     */
    public stateid4 addOpen(NFS4Client client, Inode inode, int shareAccess, int shareDeny) throws  ChimeraNFSException {

        Opaque fileId = new Opaque(inode.getFileId());
        // check for existing opens on that file
        final List<OpenState> opens = files.computeIfAbsent(fileId,
                x -> {
                    return new ArrayList<>();
                });

        stateid4 stateid;
        synchronized (opens) {
            // check for conflickting open
            for(OpenState os: opens) {
                if ((shareAccess & os.getShareDeny()) != 0
                        || (shareDeny & os.getShareAccess()) != 0) {
                    throw new ShareDeniedException("Conflicting share");
                }
            }
            NFS4State state = client.createState();
            stateid = state.stateid();
            OpenState openState = new OpenState(client, stateid, shareAccess, shareDeny);
            opens.add(openState);
            if(opens.size() == 1) {
                /*
                 * this is the the only entry. EEither this is the first open
                 * on that file or or concurrent open have removed the last entry
                 * while we was waiting for synchronized block. In this case, the
                 * list is removed from the hashmap.
                 */
                files.putIfAbsent(fileId, opens);
            }
            state.addDisposeListener(s -> {removeOpen(inode, stateid);} );
            return stateid;
        }
    }

    /**
     * Utility method to remove an open from the list.
     * @param inode of the opened file
     * @param stateid associated with the open.
     */
    private void removeOpen(Inode inode, stateid4 stateid) {

        Opaque fileId = new Opaque(inode.getFileId());
        final List<OpenState> opens = files.get(fileId);

        if (opens != null) {
            synchronized (opens) {

                Iterator<OpenState> osi = opens.listIterator();
                while(osi.hasNext()) {
                    OpenState os = osi.next();
                    if (os.stateid.equals(stateid)) {
                        osi.remove();
                        return;
                    }
                }

                /**
                 * As we hold the lock, nobody else have added something into it.
                 * A concurrent open request may be still waiting for a lock, but
                 * it have to detect removal and add into hashmap back.
                 */
                if (opens.isEmpty()) {
                    files.remove(fileId);
                }
            }
        }
    }

    /**
     * Get open access type used by opened file.
     * @param client nfs client which performs the request.
     * @param inode of the opened file
     * @param stateid associated with the open.
     * @return share access typed used.
     * @throws BadStateidException if no open file associated with provided state id.
     */
    public int getShareAccess(NFS4Client client, Inode inode, stateid4 stateid) throws BadStateidException {

        Opaque fileId = new Opaque(inode.getFileId());

        final List<OpenState> opens = files.get(fileId);

        if (opens != null) {
            synchronized (opens) {
                return opens.stream()
                        .filter(s -> client.getId() == s.client.getId())
                        .filter(s -> s.stateid.equals(stateid))
                        .map(OpenState::getShareAccess)
                        .findFirst()
                        .orElseThrow(BadStateidException::new);
            }
        }
        throw new BadStateidException("no matching open");
    }
}
