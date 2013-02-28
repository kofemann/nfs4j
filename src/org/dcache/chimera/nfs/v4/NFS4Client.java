/*
 * Copyright (c) 2009 - 2012 Deutsches Elektronen-Synchroton,
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
package org.dcache.chimera.nfs.v4;

/**
 *  with great help of William A.(Andy) Adamson
 */
import org.dcache.chimera.nfs.ChimeraNFSException;
import org.dcache.chimera.nfs.nfsstat;
import org.dcache.chimera.nfs.v4.xdr.stateid4;
import org.dcache.utils.Opaque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.dcache.chimera.nfs.v4.xdr.verifier4;

public class NFS4Client {

    private static final Logger _log = LoggerFactory.getLogger(NFS4Client.class);

    /**
     * Server boot id.
     */
    private final static long BOOTID = (System.currentTimeMillis() / 1000);

    /**
     * client id generator.
     */
    private final static AtomicInteger CLIENTID = new AtomicInteger(0);

    /*
     * from NFSv4.1 spec:
     *
     *
     *  A server's client record is a 5-tuple:
     *
     *   1. co_ownerid
     *          The client identifier string, from the eia_clientowner structure
     *          of the EXCHANGE_ID4args structure
     *   2. co_verifier:
     *          A client-specific value used to indicate reboots, from
     *          the eia_clientowner structure of the EXCHANGE_ID4args structure
     *   3. principal:
     *         The RPCSEC_GSS principal sent via the RPC headers
     *   4. client ID:
     *          The shorthand client identifier, generated by the server and
     *          returned via the eir_clientid field in the EXCHANGE_ID4resok structure
     *   5. confirmed:
     *          A private field on the server indicating whether or not a client
     *          record has been confirmed. A client record is confirmed if there
     *          has been a successful CREATE_SESSION operation to confirm it.
     *          Otherwise it is unconfirmed. An unconfirmed record is established
     *          by a EXCHANGE_ID call. Any unconfirmed record that is not confirmed
     *          within a lease period may be removed.
     *
     */

    /**
     * The client identifier string, from the eia_clientowner structure
     * of the EXCHANGE_ID4args structure
     */
    private final Opaque _ownerId;

    /**
     * A client-specific value used to indicate reboots, from
     * the eia_clientowner structure of the EXCHANGE_ID4args structure
     */

    private final verifier4 _verifier;
    /**
     * The RPCSEC_GSS principal sent via the RPC headers.
     */
    private final Principal _principal;
    /**
     * Client id generated by the server.
     */
    private final long _clientId;

    /**
     * A flag to indicate whether or not a client record has been confirmed.
     */
    private boolean _isConfirmed = false;

    // per client wide unique counter of client stateid requests
    // generated by client, incremented by server
    private int _openStateId = 1;

    /**
     * The sequence number used to track session creations.
     */
    private int _sessionSequence = 1;

    private Map<stateid4, NFS4State> _clientStates = new HashMap<stateid4, NFS4State>();
    /**
     * sessions associated with the client
     */
    private final Map<Integer, NFSv41Session> _sessions = new HashMap<Integer, NFSv41Session>();
    private long _cl_time = System.currentTimeMillis();        // time of last lease renewal

    /*

    Client identification is encapsulated in the following structure:

    struct nfs_client_id4 {
        verifier4     verifier;
        opaque        id<NFS4_OPAQUE_LIMIT>;
    };

    The first field, verifier is a client incarnation verifier that is
    used to detect client reboots.  Only if the verifier is different
    from that which the server has previously recorded the client (as
    identified by the second field of the structure, id) does the server
    start the process of canceling the client's leased state.

    The second field, id is a variable length string that uniquely
    defines the client.

     */

    /**
     * Client's {@link InetSocketAddress} seen by server.
     */
    private final InetSocketAddress _clientAddress;
    /**
     * Server's {@link InetSocketAddress} seen by client;
     */
    private final InetSocketAddress _localAddress;
    private ClientCB _cl_cb = null; /* callback info */

    /**
     * lease expiration time in milliseconds.
     */
    private final long _leaseTime;

    public NFS4Client(InetSocketAddress clientAddress, InetSocketAddress localAddress,
            byte[] ownerID, verifier4 verifier, Principal principal, long leaseTime) {

        _ownerId = new Opaque(ownerID);
        _verifier = verifier;
        _principal = principal;
        _clientId = (BOOTID << 32) | CLIENTID.incrementAndGet();

        _clientAddress = clientAddress;
        _localAddress = localAddress;
        _leaseTime = leaseTime;
        _log.debug("New client id: {}", Long.toHexString(_clientId));

    }

    public void setCB(ClientCB cb) {
        _cl_cb = cb;
    }

    public ClientCB getCB() {
        return _cl_cb;
    }

    /**
     * Owner ID provided by client.
     * @return owner id
     */
    public Opaque getOwner() {
        return _ownerId;
    }

    /**
     *
     * @return client generated verifier
     */
    public verifier4 verifier() {
        return _verifier;
    }

    /**
     *
     * @return client id generated by server
     */
    public long getId() {
        return _clientId;
    }

    public boolean verifierEquals(verifier4 verifier) {
        return _verifier.equals(verifier);
    }

    public boolean isConfirmed() {
        return _isConfirmed;
    }

    public void setConfirmed() {
        _isConfirmed = true;
    }

    public boolean isLeaseValid() {
        return (System.currentTimeMillis() - _cl_time) > _leaseTime;
    }

    /**
     * sets client lease time with current time
     * @param max_lease_time
     * @throws ChimeraNFSException if difference between current time and last
     * lease more than max_lease_time
     */
    public void updateLeaseTime() throws ChimeraNFSException {

        long curentTime = System.currentTimeMillis();
        if ((curentTime - _cl_time) > _leaseTime) {
            _clientStates.clear();
            throw new ChimeraNFSException(nfsstat.NFSERR_EXPIRED, "lease time expired");
        }
        _cl_time = curentTime;
    }

    /**
     * sets client lease time with current time
     */
    public void refreshLeaseTime() {
        _cl_time = System.currentTimeMillis();
    }

    /**
     * Get the client's {@link InetSocketAddress} seen by server.
     * @return client's address
     */
    public InetSocketAddress getRemoteAddress() {
        return _clientAddress;
    }

    /**
     * Get server's {@link InetSocketAddress} seen by the client.
     * @return server's address
     */
    public InetSocketAddress getLocalAddress() {
        return _localAddress;
    }

    public int currentSeqID() {
        return _sessionSequence;
    }

    public NFS4State createState() {
        NFS4State state = new NFS4State(_clientId, _openStateId);
        _openStateId++;
        _clientStates.put(state.stateid(), state);
        return state;
    }

    public NFS4State state(stateid4 stateid) throws ChimeraNFSException {
        NFS4State state = _clientStates.get(stateid);
        if(state == null) {
            throw new ChimeraNFSException(nfsstat.NFSERR_BAD_STATEID,
                    "State not know to the client.");
        }
        return state;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(_clientAddress).append(":").
                append(_ownerId).append("@").append(_clientId);
        return sb.toString();
    }

    /**
     *
     * @return list of sessions created by client.
     */
    public Collection<NFSv41Session> sessions() {
        return _sessions.values();
    }

    public NFSv41Session createSession(int sequence, int cacheSize) throws ChimeraNFSException {

        /*
         * For unconfirmed cleints server expects sequence number to be equal to
         * value of eir_sequenceid that was returned in results of the EXCHANGE_ID.
         */
        _log.debug("session for sequience: {}", sequence);
        if (sequence > _sessionSequence && _isConfirmed) {
            throw new ChimeraNFSException(nfsstat.NFSERR_SEQ_MISORDERED,
                    "bad sequence id: " + _sessionSequence + " / " + sequence);
        }

        if (sequence == _sessionSequence - 1 && !_isConfirmed) {
            throw new ChimeraNFSException(nfsstat.NFSERR_SEQ_MISORDERED,
                    "bad sequence id: " + _sessionSequence + " / " + sequence);
        }

        if (sequence == _sessionSequence - 1) {
            _log.debug("Retransmit on create session detected");
            return _sessions.get(sequence);
        }

        if (sequence != _sessionSequence ) {
            throw new ChimeraNFSException(nfsstat.NFSERR_SEQ_MISORDERED,
                    "bad sequence id: " + _sessionSequence + " / " + sequence);
        }

        NFSv41Session session = new NFSv41Session(this, _sessionSequence, cacheSize);
        _sessions.put(_sessionSequence, session);
        _sessionSequence++;

        if(!_isConfirmed){
            _isConfirmed = true;
            _log.debug("set client confirmed");
        }

        return session;
    }

    public void removeSession(NFSv41Session session) {
        _sessions.remove(session.getSequence());
    }

    /**
     * Tells whether there are any session owned by the client.
     * @return true if and only if, client has at at least one session
     */
    public boolean hasSessions() {
        return !_sessions.isEmpty();
    }

    public Principal principal() {
        return _principal;
    }

    public boolean hasState() {
        return !_clientStates.isEmpty();
    }

    /**
     * Bind a state to the client.
     * @param state to bind
     */
    public void bindState(NFS4State state) {
        _clientStates.put(state.stateid(), state);
    }

    /**
     * Notify client that the allocated resources are not needed any more.
     */
    public void dispose() {
       for(NFS4State state: _clientStates.values()) {
           state.dispose();
       }
    }
}
