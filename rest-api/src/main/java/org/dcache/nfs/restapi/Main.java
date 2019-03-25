package org.dcache.nfs.restapi;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import org.dcache.nfs.ExportDB;
import org.dcache.nfs.ExportFile;

/**
 *
 */
public class Main {

    // Base URI the Grizzly HTTP server will listen on
    public static final String BASE_URI = "http://localhost:8080/";

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this
     * application.
     *
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer() throws IOException {

        ExportFile e = new ExportFile(new File("/tmp/exports"));

        ExportDB exportDB = new ExportDB(new File("/tmp"));

        e.exports().forEach(i -> exportDB.addExport(i.getPath(), i));

        final ResourceConfig rc = new ResourceConfig()
                .packages(Main.class.getPackage().getName())
                .register(new ExportDbBinder(exportDB));

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    /**
     * Main method.
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        final HttpServer server = startServer();
        System.out.println(String.format("Started at %s. Hit enter to stop it...", BASE_URI));
        System.in.read();
        server.shutdown();
    }

}
