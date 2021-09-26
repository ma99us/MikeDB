package org.maggus.mikedb;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;
import java.io.InputStream;

/**
 * The class extends Application and declares root resource and provider classes
 */

@ApplicationPath("/api/")   //Defines the base URI for all resource URIs.
public class App extends ResourceConfig {

    public App() {
        super(DbHttpApiResource.class);

        // register providers
        register(MultiPartFeature.class);
//        register(InputStream.class);
    }
}