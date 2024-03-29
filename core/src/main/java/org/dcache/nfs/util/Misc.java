/*
 * Copyright (c) 2009 - 2023 Deutsches Elektronen-Synchroton,
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import org.dcache.nfs.FsExport;
import org.dcache.nfs.v4.xdr.component4;
import org.dcache.nfs.v4.xdr.fattr4_fs_locations;
import org.dcache.nfs.v4.xdr.fs_location4;
import org.dcache.nfs.v4.xdr.fs_locations4;
import org.dcache.nfs.v4.xdr.pathname4;
import org.dcache.nfs.v4.xdr.utf8str_cis;

public class Misc {

    private Misc() {}

    /**
     * Get package build time. This method uses {@code Build-Time} attribute in the
     * jar Manifest file.
     * @return optional instant of package build time.
     */
    public static Optional<Instant> getBuildTime() {

        try {

            ProtectionDomain pd = Misc.class.getProtectionDomain();
            CodeSource cs = pd.getCodeSource();
            URL u = cs.getLocation();

            InputStream is = u.openStream();
            JarInputStream jis = new JarInputStream(is);
            Manifest m = jis.getManifest();

            if (m != null) {
                Attributes as = m.getMainAttributes();
                String buildTime = as.getValue("Build-Time");
                if (buildTime != null) {
                    return Optional.of(Instant.parse(buildTime));
                }
            }

        } catch (IOException | DateTimeParseException e) {
            // bad luck
        }

        return Optional.empty();
    }

    /**
     * Parse string form of remote filesystem location into an array of {@link fs_location4}.
     * The expected form:
     *
     * <pre>
     *     path@host[+host][:path@host[+host]]
     * </pre>
     *
     * @param locations the string form of locations
     * @return an array of fs_location
     */
    public static fs_location4[] parseFsLocations(String locations) {

        List<fs_location4> l = new ArrayList<>();

        // format: path@host[+host][:path@host[+host]]
        String[] referrals = locations.split(":");
        for(String referral: referrals) {
            var fsLocation = new fs_location4();
            // FIXME: handle multiple hosts
            String[] split = referral.split("@", 2);
            fsLocation.server = new utf8str_cis[]{new utf8str_cis(split[1])};
            fsLocation.rootpath = new pathname4(new component4[]{new component4(split[0])});
            l.add(fsLocation);
        }

        return l.toArray(fs_location4[]::new);
    }


}
