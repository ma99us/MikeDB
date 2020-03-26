package org.maggus.mikedb;

import lombok.extern.java.Log;
import org.maggus.mikedb.annotations.PATCH;
import org.maggus.mikedb.services.ApiKeysService;
import org.maggus.mikedb.services.DbService;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
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
    public Response countObjects(@HeaderParam("API_KEY") String apiKey,
                                 @PathParam("dbName") String dbName, @PathParam("key") String key) {
        try {
            if (!ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.READ, dbName)) {
                throw new IllegalArgumentException("Bad or missing API_KEY header");
            }
            Object value = DbService.getDb(dbName).getItem(key);
            int num = 0;
            if (value != null) {
                num = 1;
                if (value instanceof List) {
                    num = ((List) value).size();
                }
            }
            return Response.ok().header(HttpHeaders.CONTENT_LENGTH, Integer.toString(num)).build();
        } catch (IllegalArgumentException ex) {
            log.warning(ex.getMessage());
            return Response.serverError().entity(ex.getMessage()).build();
        } catch (Exception ex) {
            log.log(Level.WARNING, "countObjects error", ex);
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
            Object value = DbService.getDb(dbName).getItem(key);
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
            log.log(Level.WARNING, "getItem error", ex);
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
            if (value == null) {
                throw new IllegalArgumentException("Value can not be null. Use DELETE instead");
            }

            DbService db = DbService.getDb(dbName);

            if (db.putItem(key, value)) {
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

    @PUT
    @Path("/{key}")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.TEXT_PLAIN})
    public Response setString(@HeaderParam("API_KEY") String apiKey,
                              @PathParam("dbName") String dbName, @PathParam("key") String key, String value) {
        return setObject(apiKey, dbName, key, value);
    }

    @POST
    @Path("/{key}")
    public Response addObjects(@HeaderParam("API_KEY") String apiKey,
                               @PathParam("dbName") String dbName, @PathParam("key") String key,
                               @QueryParam("index") @DefaultValue("-1") int index, Object value) {
        try {
            if (!ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.WRITE, dbName)) {
                throw new IllegalArgumentException("Bad or missing API_KEY header");
            }
            if (value == null) {
                throw new IllegalArgumentException("Value can not be null. Use DELETE instead");
            }

            DbService db = DbService.getDb(dbName);
            Object object = db.getItem(key);
            List valList = new ArrayList();
            if (object instanceof List) {
                valList = (List) object;
            } else if (object != null) {
                valList.add(object);
            }

            if (value instanceof List && (index < 0 || index >= valList.size())) {
                valList.addAll(((List) value));
            } else if (value instanceof List) {
                valList.addAll(index, ((List) value));
            } else if (index < 0 || index >= valList.size()) {
                valList.add(value);
            } else {
                valList.add(index, value);
            }

            if (DbService.getDb(dbName).putItem(key, valList)) {
                if(object == null){
                    final URI processIdUri = UriBuilder.fromResource(KeyValuePairsApi.class).path("/{key}").build(dbName, key);
                    return Response.created(processIdUri).type(prepareMediaType(value)).entity(value).build();
                } else {
                    return Response.ok().type(prepareMediaType(value)).entity(value).build();
                }
            } else {
                return Response.noContent().build();
            }
        } catch (IllegalArgumentException ex) {
            log.warning(ex.getMessage());
            return Response.serverError().entity(ex.getMessage()).build();
        } catch (Exception ex) {
            log.log(Level.WARNING, "addObjects error", ex);
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

    @POST
    @Path("/{key}")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.TEXT_PLAIN})
    public Response addStrings(@HeaderParam("API_KEY") String apiKey,
                               @PathParam("dbName") String dbName, @PathParam("key") String key,
                               @QueryParam("index") @DefaultValue("-1") int index, String value) {
        return addObjects(apiKey, dbName, key, index, value);
    }

    @DELETE
    @Path("/{key}")
    public Response deleteKey(@HeaderParam("API_KEY") String apiKey,
                              @PathParam("dbName") String dbName, @PathParam("key") String key,
                              @QueryParam("index") @DefaultValue("-1") int index) {
        try {
            if (!ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.WRITE, dbName)) {
                throw new IllegalArgumentException("Bad or missing API_KEY header");
            }

            DbService db = DbService.getDb(dbName);
            Object object = db.getItem(key);
            if (index < 0) {
                // remove the Key completely
                if (DbService.getDb(dbName).removeItem(key)) {
                    return Response.ok().build();
                } else {
                    return Response.noContent().build();
                }
            } else if (!(object instanceof List)) {
                throw new IllegalArgumentException("Unexpected index " + index + ". Value is not a collection");
            } else {
                List valList = (List) object;
                if (index >= valList.size()) {
                    throw new IllegalArgumentException("Bad index " + index);
                }
                valList.remove(index);
                if (DbService.getDb(dbName).putItem(key, valList)) {
                    return Response.ok().build();
                } else {
                    return Response.noContent().build();
                }
            }
        } catch (IllegalArgumentException ex) {
            log.warning(ex.getMessage());
            return Response.serverError().entity(ex.getMessage()).build();
        } catch (Exception ex) {
            log.log(Level.WARNING, "deleteKey error", ex);
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

    @DELETE
    @Path("/")
    public Response dropDb(@HeaderParam("API_KEY") String apiKey,
                              @PathParam("dbName") String dbName) {
        try {
            if (!ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.WRITE, dbName)) {
                throw new IllegalArgumentException("Bad or missing API_KEY header");
            }

            if (DbService.dropDb(dbName)) {
                return Response.ok().build();
            } else {
                return Response.noContent().build();
            }

        } catch (IllegalArgumentException ex) {
            log.warning(ex.getMessage());
            return Response.serverError().entity(ex.getMessage()).build();
        } catch (Exception ex) {
            log.log(Level.WARNING, "dropDb error", ex);
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

    @PATCH
    @Path("/{key}")
    public Response updateObjects(@HeaderParam("API_KEY") String apiKey,
                                  @PathParam("dbName") String dbName, @PathParam("key") String key,
                                  @QueryParam("index") @DefaultValue("-1") int index, Object value) {
        try {
            if (!ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.WRITE, dbName)) {
                throw new IllegalArgumentException("Bad or missing API_KEY header");
            }
            if (value == null) {
                throw new IllegalArgumentException("Value can not be null. Use DELETE instead");
            }

            DbService db = DbService.getDb(dbName);
            Object object = db.getItem(key);
            if (index < 0) {
                return setObject(apiKey, dbName, key, value);
            } else if (!(object instanceof List)) {
                throw new IllegalArgumentException("Unexpected index " + index + ". Value is not a collection");
            } else {
                List valList = (List) object;
                if (index >= valList.size()) {
                    throw new IllegalArgumentException("Bad index " + index);
                }
                valList.set(index, value);
                if (DbService.getDb(dbName).putItem(key, valList)) {
                    return Response.ok().type(prepareMediaType(value)).entity(value).build();
                } else {
                    return Response.noContent().build();
                }
            }
        } catch (IllegalArgumentException ex) {
            log.warning(ex.getMessage());
            return Response.serverError().entity(ex.getMessage()).build();
        } catch (Exception ex) {
            log.log(Level.WARNING, "updateObjects error", ex);
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

    @PATCH
    @Path("/{key}")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.TEXT_PLAIN})
    public Response updateStrings(@HeaderParam("API_KEY") String apiKey,
                                  @PathParam("dbName") String dbName, @PathParam("key") String key,
                                  @QueryParam("index") @DefaultValue("-1") int index, String value) {
        return updateObjects(apiKey, dbName, key, index, value);
    }

    /**
     * Use Media Type TEXT_PLAIN for regular strings, APPLICATION_JSON_TYPE for everything else
     */
    protected MediaType prepareMediaType(Object value) {
        if (value instanceof String) {
            return MediaType.TEXT_PLAIN_TYPE;
        } else {
            return MediaType.APPLICATION_JSON_TYPE;
        }
    }
}