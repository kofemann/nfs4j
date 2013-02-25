package org.dcache.utils;

import java.security.Principal;
import javax.security.auth.Subject;
import org.dcache.auth.GidPrincipal;
import org.dcache.auth.UidPrincipal;
import org.dcache.xdr.RpcLoginService;

/**
 *
 * @author tigran
 */


public class RpcLogin implements RpcLoginService {

    @Override
    public Subject login(Principal principal) {
        Subject s =  new Subject();
        s.getPrincipals().add(new UidPrincipal(17));
        s.getPrincipals().add(new GidPrincipal(17, true));
        return s;
    }
    
}
