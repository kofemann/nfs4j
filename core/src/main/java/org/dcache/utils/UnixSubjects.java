/*
 * Copyright (c) 2019 Deutsches Elektronen-Synchroton,
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
package org.dcache.utils;

import javax.security.auth.Subject;
import com.sun.security.auth.UnixNumericGroupPrincipal;
import com.sun.security.auth.UnixNumericUserPrincipal;
/**
 *
 */
public class UnixSubjects {


    /**
     * The subject representing the root user, that is, a user that is empowered
     * to do everything.
     */
    public static final Subject ROOT;
    public static final Subject NOBODY;

    static {
        ROOT = new Subject();
        ROOT.getPrincipals().add(new UnixNumericUserPrincipal(0));
        ROOT.getPrincipals().add(new UnixNumericGroupPrincipal(0, true));
        ROOT.setReadOnly();

        NOBODY = new Subject();
        NOBODY.setReadOnly();
    }

    /**
     * Create a subject for UNIX based user record.
     *
     * @param uid
     * @param gid
     * @param gids
     */
    public static Subject of(int uid, int gid, int... gids) {

        Subject subject = new Subject();
        subject.getPrincipals().add(new UnixNumericUserPrincipal(uid));
        subject.getPrincipals().add(new UnixNumericGroupPrincipal(gid, true));
        for (int g : gids) {
            subject.getPrincipals().add(new UnixNumericGroupPrincipal(g, false));
        }
        return subject;
    }


        /**
     * Returns true if and only if the subject is root, that is, has the user ID
     * 0.
     */
    public static boolean isRoot(Subject subject) {
        return hasUid(subject, 0);
    }

    /**
     * Returns true if and only if the subject is nobody, i.e., does not
     * have a UID.
     *
     */
    public static boolean isNobody(Subject subject) {
        return !subject.getPrincipals().stream()
                .anyMatch(UnixNumericUserPrincipal.class::isInstance);
    }

    /**
     * Returns true if and only if the subject has the given user ID.
     */
    public static boolean hasUid(Subject subject, long uid) {

        return subject.getPrincipals().stream()
                .filter(UnixNumericUserPrincipal.class::isInstance)
                .map(UnixNumericUserPrincipal.class::cast)
                .anyMatch(s -> s.longValue() == uid);
    }


    /**
     * Returns true if and only if the subject has the given group ID.
     */
    public static boolean hasGid(Subject subject, long gid) {
        return subject.getPrincipals().stream()
                .filter(UnixNumericGroupPrincipal.class::isInstance)
                .map(UnixNumericGroupPrincipal.class::cast)
                .anyMatch(s -> s.longValue() == gid);
    }
}
