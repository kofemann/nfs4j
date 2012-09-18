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
package org.dcache.chimera.nfs;

import java.net.InetAddress;
import org.dcache.chimera.nfs.ExportClient.Root;

public class FsExport {

    private final String _path;
    private final ExportClient _client;

    /**
     * NFS clients may be specified in a number of ways:<br>
     * <p>
     *
     * <b>single host</b>
     * <p>
     * This is the most common format. You may specify a host either by an
     * abbreviated name recognized be the resolver, the fully qualified domain
     * name, or an IP address.
     * <p>
     *
     * <b>wildcards</b>
     * <p>
     * Machine names may contain the wildcard characters * and ?. This can be
     * used to make the exports file more compact; for instance, .cs.foo.edu
     * matches all hosts in the domain cs.foo.edu. As these characters also
     * match the dots in a domain name, the given pattern will also match all
     * hosts within any subdomain of cs.foo.edu.
     * <p>
     *
     * <b>IP networks</b>
     * <p>
     * You can also export directories to all hosts on an IP (sub-) network
     * simultaneously. This is done by specifying an IP address and netmask pair
     * as address/netmask where the netmask can be specified in dotted-decimal
     * format, or as a contiguous mask length (for example, either
     * `/255.255.252.0' or `/22' appended to the network base address result in
     * identical subnetworks with 10 bits of host). Wildcard characters
     * generally do not work on IP addresses, though they may work by accident
     * when reverse DNS lookups fail.
     * <p>
     *
     *
     * @param path
     * @param clients list of {@link ExportClient} which allowed to mount this export.
     */
    public FsExport(String path, ExportClient client) {

        _path = path;
        _client = client;

    }

    public String getPath() {
        return _path;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(_path).append(":");

        sb.append(" ").append(_client.ip()).append("(").append(
                _client.io()).append(",").append(_client.trusted())
                .append(")");

        return sb.toString();

    }

    public boolean isAllowed(InetAddress client) {

        // localhost always allowed
        return client.isLoopbackAddress() || IPMatcher.match(_client.ip(), client);
    }

    public boolean isTrusted(InetAddress client) {

        // localhost always allowed
        return isAllowed(client) && _client.trusted() == Root.TRUSTED;
    }

    public String client() {
        return _client.ip();
    }
}
