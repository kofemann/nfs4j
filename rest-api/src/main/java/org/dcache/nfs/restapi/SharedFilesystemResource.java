package org.dcache.nfs.restapi;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.dcache.nfs.ExportDB;
import org.dcache.nfs.FsExport;
import org.json.JSONArray;
import org.json.JSONObject;

import static javax.ws.rs.core.Response.Status;

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

        // create a dummy entry to simulate 'create share'
        try {
            FsExport export = new FsExport.FsExportBuilder()
                    .forClient("127.0.0.1")
                    .build(path);

            exportDB.addExport(path, export);
            return Response.status(Status.CREATED).build();
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
                .put("root_squash", export.isTrusted());
    }

}
