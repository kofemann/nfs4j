package org.dcache.nfs.restapi;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.dcache.nfs.ExportDB;
import org.dcache.nfs.FsExport;
import org.json.JSONObject;

import static javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

/**
 *
 */
@Path("v1")
public class SharedFilesystemResource {

    @Inject
    private ExportDB exportDB;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/exports")
    public Response exports(
            @DefaultValue("") @QueryParam("client") String client,
            @DefaultValue("false") @QueryParam("pretty") boolean pretty) {

        try {

            Predicate<FsExport> clientFilter;

            if ("".equals(client)) {
                clientFilter = x -> true;
            } else {
                InetAddress clientAddress = InetAddress.getByName(client);
                clientFilter = x -> x.isAllowed(clientAddress);
            }

            List<JSONObject> exports = exportDB.exports()
                    .filter(clientFilter)
                    .map(SharedFilesystemResource::toJson)
                    .collect(Collectors.toList());

            JSONObject exportObject = new JSONObject()
                    .put("exports", exports);

            return Response.status(Status.OK).entity(exportObject.toString(pretty ? 2 : 0)).build();
        } catch (UnknownHostException e) {
            return Response.status(Status.BAD_REQUEST).build();
        }
    }

    @POST
    @Path("/exports/{path : .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createShare(@PathParam("path") String path) {

        // make it absolute
        String exportPath = "/" + path;

        // create a dummy entry to simulate 'create share'
        try {
            FsExport export = new FsExport.FsExportBuilder()
                    .forClient("127.0.0.1")
                    .build(exportPath);

            exportDB.addExport(export);
            return Response.status(Status.CREATED).build();
        } catch (UnknownHostException e) {
            // should never happen
            return Response.status(Status.SERVICE_UNAVAILABLE).build();
        }
    }
    @DELETE
    @Path("/exports/{path : .*}")
    public Response deleteShare(@PathParam("path") String path) {

        // make it absolute
        String exportPath = "/" + path;

        exportDB.removeExport(exportPath);
        return Response.status(Status.OK).build();
    }

    @DELETE
    @Path("/client/{client : .*}/{path : .*}")
    public Response deleteClient(@PathParam("client") String client, @PathParam("path") String path) {

        // make it absolute
        String exportPath = "/" + path;

        exportDB.removeExport(exportPath, client);
        return Response.status(Status.OK).build();
    }

    @POST
    @Path("/client/{client : .*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addClient(@PathParam("client") String client, @Context UriInfo uriInfo, Share share) {

        try {

            if (share.path == null) {
                return Response.status(Status.BAD_REQUEST).build();
            }

            // share for this path must exist
            if (!exportDB.exports()
                    .filter(e -> e.getPath().equals(share.path))
                    .findAny().isPresent()) {
                return Response.status(Status.NOT_FOUND).build();
            }

            FsExport.FsExportBuilder exportBuilder = new FsExport.FsExportBuilder()
                    .forClient(client);

            if (share.iomode.equalsIgnoreCase("rw")) {
                exportBuilder.rw();
            } else {
                exportBuilder.ro();
            }

            if (share.anonuid != null) {
                exportBuilder.withAnonUid(share.anonuid);
            }

            if (share.anongid != null) {
                exportBuilder.withAnonGid(share.anongid);
            }

            if (share.secure != null) {
                if (share.secure.booleanValue()) {
                    exportBuilder.withPrivilegedClientPort();
                } else {
                    exportBuilder.withoutPrivilegedClientPort();
                }
            }

            FsExport export = exportBuilder.build(share.path);
            exportDB.addExport(export);
            return Response.status(Status.OK).build();
        } catch (UnknownHostException e) {
            // should never happen
            return Response.status(Status.SERVICE_UNAVAILABLE).build();
        }
    }

    private static JSONObject toJson(FsExport export) {
        return new JSONObject()
                .put("path", export.getPath())
                .put("index", export.getIndex())
                .put("client", export.client())
                .put("sec", export.getSec())
                .put("anonuid", export.getAnonUid())
                .put("anongid", export.getAnonGid())
                .put("iomode", export.ioMode())
                .put("secure", export.isPrivilegedClientPortRequired())
                .put("root_squash", export.isTrusted());
    }


    public static class Share {

        private String path;
        private String iomode;
        private Integer anonuid;
        private Integer anongid;
        private Boolean secure;


        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Integer getAnonUid() {
            return anonuid;
        }

        public void setAnonuid(Integer anonUid) {
            this.anonuid = anonUid;
        }

        public Integer getAnonGid() {
            return anongid;
        }

        public void setAnongid(Integer anonGid) {
            this.anongid = anonGid;
        }

        public Boolean getSecure() {
            return secure;
        }

        public void setSecure(Boolean secure) {
            this.secure = secure;
        }

        public String getIomode() {
            return iomode;
        }

        public void setIomode(String iomode) {
            this.iomode = iomode;
        }

    }
}
