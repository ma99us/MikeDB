package org.maggus.mikedb;

import lombok.extern.java.Log;
import org.maggus.mikedb.services.ApiKeysService;
import org.maggus.mikedb.services.DbService;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

@Path("/{dbName}")
@Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
@Consumes({MediaType.APPLICATION_JSON})
@Log
public class KeyValuePairsApi {

    @Context
    private UriInfo uriInfo;

    @HEAD
    @Path("/{key}")
    public Response countValues(@HeaderParam("API_KEY") String apiKey,
                                @PathParam("dbName") String dbName, @PathParam("key") String key) {
        try {
            if (!ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.READ, dbName)) {
                throw new IllegalArgumentException("Bad or missing API_KEY header");
            }
            Object value = DbService.getDb(dbName).getObject(key);
            int num = 0;
            if (value != null) {
                num = 1;
                if (value instanceof Collection) {
                    num = ((Collection) value).size();
                }
            }
            return Response.ok().header(HttpHeaders.CONTENT_LENGTH, Integer.toString(num)).build();
        } catch (IllegalArgumentException ex) {
            log.warning(ex.getMessage());
            return Response.serverError().entity(ex.getMessage()).build();
        } catch (Exception ex) {
            log.log(Level.WARNING, "countValues error", ex);
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }


    @GET
    @Path("/{key}")
    public Response getObject(@HeaderParam("API_KEY") String apiKey,
                              @PathParam("dbName") String dbName, @PathParam("key") String key,
                              @QueryParam("firstResult") @DefaultValue("0") int firstResult,
                              @QueryParam("maxResults") @DefaultValue("-1") int maxResults) {
        try {
            if (!ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.READ, dbName)) {
                throw new IllegalArgumentException("Bad or missing API_KEY header");
            }
            Object value = DbService.getDb(dbName).getObject(key);
            if (value != null) {
                if (value instanceof List && (firstResult > 0 || maxResults >= 0)) {
                    List list = (List) value;
                    value = list.subList(firstResult, maxResults >= 0 ? firstResult + maxResults : list.size());
                }
                return Response.ok().type(prepareMediaType(value)).entity(value).build();
            } else {
                //return Response.status(Response.Status.NOT_FOUND).build();
                return Response.noContent().build();
            }
        } catch (IllegalArgumentException ex) {
            log.warning(ex.getMessage());
            return Response.serverError().entity(ex.getMessage()).build();
        } catch (Exception ex) {
            log.log(Level.WARNING, "getObject error", ex);
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

    @PUT
    @Path("/{key}")
    public Response setObject(@HeaderParam("API_KEY") String apiKey,
                              @PathParam("dbName") String dbName, @PathParam("key") String key, Object value) {
        try {
            if (!ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.WRITE, dbName)) {
                throw new IllegalArgumentException("Bad or missing API_KEY header");
            }
            DbService db = DbService.getDb(dbName);
            if (db.putObject(key, value)) {
                final URI processIdUri = UriBuilder.fromResource(KeyValuePairsApi.class).path("/{key}").build(dbName, key);
                return Response.created(processIdUri).type(prepareMediaType(value)).entity(value).build();
            } else {
                return Response.noContent().build();
            }
        } catch (IllegalArgumentException ex) {
            log.warning(ex.getMessage());
            return Response.serverError().entity(ex.getMessage()).build();
        } catch (Exception ex) {
            log.log(Level.WARNING, "setObject error", ex);
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

    @POST
    @Path("/{key}")
    public Response addObject(@HeaderParam("API_KEY") String apiKey,
                              @PathParam("dbName") String dbName, @PathParam("key") String key, Object value) {
        try {
            if (!ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.WRITE, dbName)) {
                throw new IllegalArgumentException("Bad or missing API_KEY header");
            }
            DbService db = DbService.getDb(dbName);
            Object object = db.getObject(key);
            if(object != null && object instanceof List && value instanceof List){
                ((List)object).addAll(((List)value));
            } else {
                object = value;
            }
            if (DbService.getDb(dbName).putObject(key, object)) {
                final URI processIdUri = UriBuilder.fromResource(KeyValuePairsApi.class).path("/{key}").build(dbName, key);
                return Response.created(processIdUri).entity(value).build();
            } else {
                return Response.noContent().build();
            }
        } catch (IllegalArgumentException ex) {
            log.warning(ex.getMessage());
            return Response.serverError().entity(ex.getMessage()).build();
        } catch (Exception ex) {
            log.log(Level.WARNING, "addObject error", ex);
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

//    @GET
//    @Path("/{key}")
//    public Response getString(@HeaderParam("API_KEY") String apiKey,
//                              @PathParam("dbName") String dbName, @PathParam("key") String key) {
//        return getObject(apiKey, dbName, key, 0, -1);
//    }
//
    @PUT
    @Path("/{key}")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.TEXT_PLAIN})
    public Response setString(@HeaderParam("API_KEY") String apiKey,
                              @PathParam("dbName") String dbName, @PathParam("key") String key, String value) {
        return setObject(apiKey, dbName, key, value);
    }

    @DELETE
    @Path("/{key}")
    public Response deleteKey(@HeaderParam("API_KEY") String apiKey,
                              @PathParam("dbName") String dbName, @PathParam("key") String key) {
        try {
            if (!ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.WRITE, dbName)) {
                throw new IllegalArgumentException("Bad or missing API_KEY header");
            }
            DbService db = DbService.getDb(dbName);
            if (DbService.getDb(dbName).putObject(key, null)) {
                return Response.ok().build();
            } else {
                return Response.noContent().build();
            }
        } catch (IllegalArgumentException ex) {
            log.warning(ex.getMessage());
            return Response.serverError().entity(ex.getMessage()).build();
        } catch (Exception ex) {
            log.log(Level.WARNING, "deleteKey error", ex);
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

    /**
     * Use Media Type TEXT_PLAIN for regular strings, APPLICATION_JSON_TYPE for everything else
     */
    protected MediaType prepareMediaType(Object value){
        if(value instanceof String){
            return MediaType.TEXT_PLAIN_TYPE;
        } else{
            return MediaType.APPLICATION_JSON_TYPE;
        }
    }
}