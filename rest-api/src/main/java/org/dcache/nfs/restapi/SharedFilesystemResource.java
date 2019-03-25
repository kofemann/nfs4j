package org.dcache.nfs.restapi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


/**
 *
 */
@Path("v1")
public class SharedFilesystemResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/exports")
    public String getHello() {
        return "hello";
    }
}
