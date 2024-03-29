package org.maggus.mikedb;

import lombok.extern.java.Log;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.maggus.mikedb.annotations.PATCH;
import org.maggus.mikedb.data.FileItem;
import org.maggus.mikedb.data.FileItemStream;
import org.maggus.mikedb.services.ApiKeysService;
import org.maggus.mikedb.services.DbService;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;

/**
 * Database REST API service
 */

@Path("/{dbName}")
@Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
@Consumes({MediaType.APPLICATION_JSON})
@Log
public class DbHttpApiResource {

    @Context
    private UriInfo uriInfo;

    @HEAD
    @Path("/{key}")
    public Response countObjects(@HeaderParam("API_KEY") String apiKey, @HeaderParam("SESSION_ID") String sessionId,
                                 @PathParam("dbName") String dbName, @PathParam("key") String key) {
        try {
            if (!ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.READ, dbName)) {
                throw new IllegalArgumentException("Bad or missing API_KEY header");
            }
            Object value = DbService.getDb(dbName).getItem(key);
            long num = 0;
            if (value != null) {
                num = 1;
                if (value instanceof List) {
                    num = ((List) value).size();
                } else if (value instanceof FileItem) {
                    num = ((FileItem) value).getFileSize();
                }
            }
            return Response.ok().header(HttpHeaders.CONTENT_LENGTH, Long.toString(num)).build();
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
    @Produces({"image/*", "video/*", "audio/*", "application/x-binary", MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Response getObject(@HeaderParam("API_KEY") String apiKey, @HeaderParam("SESSION_ID") String sessionId,
                              @PathParam("dbName") String dbName, @PathParam("key") String key,
                              @QueryParam("firstResult") @DefaultValue("0") int firstResult,
                              @QueryParam("maxResults") @DefaultValue("-1") int maxResults,
                              @QueryParam("fields") String fieldNames,
                              @QueryParam("id") Long id) {
        try {
            String[] fields = null;
            if (fieldNames != null && !fieldNames.isEmpty()) {
                fields = fieldNames.trim().split("\\s*[,;]\\s*");
            } else if (fieldNames != null) {
                fields = new String[]{"id"};
            }

            // no security to download images, so that simple links could be shared
            Object value = DbService.getDb(dbName).getItem(key, fields);
            if (!(value instanceof FileItem) && !ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.READ, dbName)) {
                throw new IllegalArgumentException("Bad or missing API_KEY header");
            }

            if (value != null) {
                if (value instanceof List && id != null) {
                    // get a single item from a list
                    value = ((List) value).stream().filter(val -> id.equals(DbService.getIdValue(val))).findAny().orElse(null);
                    if (value == null) {
                        return Response.noContent().build();
                    }
                    return Response.ok().type(prepareMediaType(value)).entity(value).build();
                } else if (value instanceof List && (firstResult > 0 || maxResults >= 0)) {
                    List list = (List) value;
                    value = list.subList(firstResult, maxResults >= 0 ? firstResult + maxResults : list.size());
                    return Response.ok().type(prepareMediaType(value)).entity(value).build();
                } else if (value instanceof FileItem) {
                    FileItem fi = (FileItem) value;
                    FileInputStream in = new FileInputStream(fi.getFileName());
                    return Response.ok().type(prepareMediaType(value)).entity(in).lastModified(fi.getFileTimestamp()).build();
                } else {
                    return Response.ok().type(prepareMediaType(value)).entity(value).build();
                }
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

    @GET
    @Path("/{key}/{id}")
    public Response getObjectById(@HeaderParam("API_KEY") String apiKey, @HeaderParam("SESSION_ID") String sessionId,
                                  @PathParam("dbName") String dbName, @PathParam("key") String key, @PathParam("id") Long id,
                                  @QueryParam("firstResult") @DefaultValue("0") int firstResult,
                                  @QueryParam("maxResults") @DefaultValue("-1") int maxResults,
                                  @QueryParam("fields") String fieldNames) {
        return getObject(apiKey, sessionId, dbName, key, firstResult, maxResults, fieldNames, id);
    }

//    @GET
//    @Path("/{key}")
//    @Produces({"image/*", "video/*", "audio/*", "application/x-binary"})
//    public Response getFile(@HeaderParam("API_KEY") String apiKey, @HeaderParam("SESSION_ID") String sessionId,
//                            @PathParam("dbName") String dbName, @PathParam("key") String key) {
//        try {
//            // no security to download images, so that simple links could be shared
////            if (!ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.READ, dbName)) {
////                throw new IllegalArgumentException("Bad or missing API_KEY header");
////            }
//            Object value = DbService.getDb(dbName).getItem(key, null);
//            if (value instanceof FileItem) {
//                FileItem fi = (FileItem) value;
//                FileInputStream in = new FileInputStream(fi.getFileName());
////                return Response.ok(in).lastModified(fi.getFileTimestamp()).build();
//                return Response.ok().type(prepareMediaType(value)).entity(in).build();
//            } else {
////                return Response.status(Response.Status.NOT_FOUND).build();
//                return Response.noContent().build();
//            }
//        } catch (IllegalArgumentException ex) {
//            log.warning(ex.getMessage());
//            return Response.serverError().entity(ex.getMessage()).build();
//        } catch (Exception ex) {
//            log.log(Level.WARNING, "getFile error", ex);
//            return Response.serverError().entity(ex.getMessage()).build();
//        }
//    }

    @PUT
    @Path("/{key}")
    public Response setObject(@HeaderParam("API_KEY") String apiKey, @HeaderParam("SESSION_ID") String sessionId,
                              @PathParam("dbName") String dbName, @PathParam("key") String key, Object value) {
        try {
            if (!ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.WRITE, dbName)) {
                throw new IllegalArgumentException("Bad or missing API_KEY header");
            }
            if (value == null) {
                throw new IllegalArgumentException("Value can not be null. Use DELETE instead");
            }

            DbService db = DbService.getDb(dbName);
            Object object = db.getItem(key);

            if (db.putItem(key, value, sessionId, value)) {
                if (object == null) {
                    //final URI processIdUri = UriBuilder.fromResource(DbHttpApiResource.class).path(key).build(dbName);
                    return Response.created(new URI("/" + dbName + "/" + key)).type(prepareMediaType(value)).entity(value).build();
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
            log.log(Level.WARNING, "setObject error", ex);
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

    @PUT
    @Path("/{key}")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.TEXT_PLAIN})
    public Response setString(@HeaderParam("API_KEY") String apiKey, @HeaderParam("SESSION_ID") String sessionId,
                              @PathParam("dbName") String dbName, @PathParam("key") String key, String value) {
        return setObject(apiKey, sessionId, dbName, key, value);
    }

    @PUT
    @Path("/{key}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response setFile(@HeaderParam("API_KEY") String apiKey, @HeaderParam("SESSION_ID") String sessionId,
                            @PathParam("dbName") String dbName, @PathParam("key") String key,
                            @FormDataParam("file") InputStream uploadedInputStream,
                            @FormDataParam("file") FormDataContentDisposition fileDetail
    ) {
        try {
            if (!ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.WRITE, dbName)) {
                throw new IllegalArgumentException("Bad or missing API_KEY header");
            }
            if (uploadedInputStream == null) {
                throw new IllegalArgumentException("File can not be null. Use DELETE instead");
            }

            DbService db = DbService.getDb(dbName);
            Object object = db.getItem(key);

            FileItemStream value = new FileItemStream(fileDetail, uploadedInputStream);
            FileItem fileItem = value.getFileItem();
            fileItem.setName(key);  // use 'key' as a file name

            if (db.putItem(key, value, sessionId, fileItem)) {
                if (object == null) {
                    //final URI processIdUri = UriBuilder.fromResource(DbHttpApiResource.class).path(key).build(dbName);
                    return Response.created(new URI("/" + dbName + "/" + key)).type(MediaType.APPLICATION_JSON_TYPE).entity(fileItem.clone()).build();
                } else {
                    return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(fileItem.clone()).build();
                }
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
    public Response addObjects(@HeaderParam("API_KEY") String apiKey, @HeaderParam("SESSION_ID") String sessionId,
                               @PathParam("dbName") String dbName, @PathParam("key") String key,
                               @QueryParam("index") Integer index, Object value) {
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

            if (value instanceof List && index == null) {
                valList.addAll(((List) value));
            } else if (value instanceof List) {
                if (index < 0 || index > valList.size()) {
                    throw new IllegalArgumentException("Bad index " + index);
                }
                valList.addAll(index, ((List) value));
            } else if (index == null) {
                valList.add(value);
            } else {
                if (index < 0 || index > valList.size()) {
                    throw new IllegalArgumentException("Bad index " + index);
                }
                valList.add(index, value);
            }

            if (DbService.getDb(dbName).putItem(key, valList, sessionId, value)) {
                if (object == null) {
                    //final URI processIdUri = UriBuilder.fromResource(DbHttpApiResource.class).path(key).build(dbName);
                    return Response.created(new URI("/" + dbName + "/" + key)).type(prepareMediaType(value)).entity(value).build();
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
    public Response addStrings(@HeaderParam("API_KEY") String apiKey, @HeaderParam("SESSION_ID") String sessionId,
                               @PathParam("dbName") String dbName, @PathParam("key") String key,
                               @QueryParam("index") Integer index, String value) {
        return addObjects(apiKey, sessionId, dbName, key, index, value);
    }

    @POST
    @Path("/{key}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addFile(@HeaderParam("API_KEY") String apiKey, @HeaderParam("SESSION_ID") String sessionId,
                            @PathParam("dbName") String dbName, @PathParam("key") String key,
                            @FormDataParam("file") InputStream uploadedInputStream,
                            @FormDataParam("file") FormDataContentDisposition fileDetail
    ) {
        return setFile(apiKey, sessionId, dbName, key, uploadedInputStream, fileDetail);
    }

    @PATCH
    @Path("/{key}")
    public Response updateObjects(@HeaderParam("API_KEY") String apiKey, @HeaderParam("SESSION_ID") String sessionId,
                                  @PathParam("dbName") String dbName, @PathParam("key") String key,
                                  @QueryParam("index") Integer index, Object value) {
        try {
            if (!ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.WRITE, dbName)) {
                throw new IllegalArgumentException("Bad or missing API_KEY header");
            }
            if (value == null) {
                throw new IllegalArgumentException("Value can not be null. Use DELETE instead");
            }

            DbService db = DbService.getDb(dbName);
            Object object = db.getItem(key);
            if (object == null) {
                return setObject(apiKey, sessionId, dbName, key, value);
            }
            Long valId = DbService.getIdValue(value);
            if (index == null && valId == null) {
                return setObject(apiKey, sessionId, dbName, key, value);
            } else if (index != null && valId == null) {
                // update item based on index only
                if (!(object instanceof List)) {
                    throw new IllegalArgumentException("Unexpected index " + index + ". Value is not a collection");
                }
                List valList = (List) object;
                if (index >= valList.size()) {
                    throw new IllegalArgumentException("Bad index " + index);
                }
                valList.set(index, value);
                if (DbService.getDb(dbName).putItem(key, valList, sessionId, value)) {
                    return Response.ok().type(prepareMediaType(value)).entity(value).build();
                }
            } else if (valId != null) {
                // update item based on object id
                if (!(object instanceof List)) {
                    if (valId.equals(DbService.getIdValue(object))) {
                        // set the whole key
                        return setObject(apiKey, sessionId, dbName, key, value);
                    } else {
                        // add new item to the list of values
                        return addObjects(apiKey, sessionId, dbName, key, index, value);
                    }
                } else {
                    List valList = (List) object;
                    if (index != null) {
                        // both index and object id are found. Assume we need to re-arrange the item in the list
                        Object oldVal = valList.stream().filter(val -> valId.equals(DbService.getIdValue(val))).findAny().orElse(null);
                        if (oldVal != null) {
                            // remove old one
                            valList.remove(oldVal);
                            if (index < valList.size()) {
                                valList.add(index, value);
                            } else {
                                valList.add(value);
                            }
                        } else {
                            // no such item, insert it
                            return addObjects(apiKey, sessionId, dbName, key, index, value);
                        }
                    } else {
                        // update an item in the list based on it's id
                        ListIterator iter = valList.listIterator();
                        boolean modified = false;
                        while (iter.hasNext()) {
                            Object val = iter.next();
                            if (valId.equals(DbService.getIdValue(val))) {
                                // replace the value item
                                iter.set(value);
                                modified = true;
                            }
                        }
                        if (!modified) {
                            // add new item to list values
                            valList.add(value);
                        }
                    }

                    if (DbService.getDb(dbName).putItem(key, valList, sessionId, value)) {
                        return Response.ok().type(prepareMediaType(value)).entity(value).build();
                    }
                }
            }
            return Response.noContent().build();
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
    public Response updateStrings(@HeaderParam("API_KEY") String apiKey, @HeaderParam("SESSION_ID") String sessionId,
                                  @PathParam("dbName") String dbName, @PathParam("key") String key,
                                  @QueryParam("index") Integer index, String value) {
        return updateObjects(apiKey, sessionId, dbName, key, index, value);
    }

    @PATCH
    @Path("/{key}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response updateFile(@HeaderParam("API_KEY") String apiKey, @HeaderParam("SESSION_ID") String sessionId,
                               @PathParam("dbName") String dbName, @PathParam("key") String key,
                               @FormDataParam("file") InputStream uploadedInputStream,
                               @FormDataParam("file") FormDataContentDisposition fileDetail) {
        return setFile(apiKey, sessionId, dbName, key, uploadedInputStream, fileDetail);
    }

    @DELETE
    @Path("/{key}")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response deleteObject(@HeaderParam("API_KEY") String apiKey, @HeaderParam("SESSION_ID") String sessionId,
                                 @PathParam("dbName") String dbName, @PathParam("key") String key,
                                 @QueryParam("index") Integer index, @QueryParam("id") Long id,
                                 Object value) {
        try {
            if (!ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.WRITE, dbName)) {
                throw new IllegalArgumentException("Bad or missing API_KEY header");
            }
            DbService db = DbService.getDb(dbName);
            Object object = db.getItem(key);
            if (object == null) {
                return Response.noContent().build();
            }
            Long valId = DbService.getIdValue(value);
            valId = valId == null ? id : valId;
            if (index == null && valId == null && isNullOrEmptyValue(value)) {
                // remove the Key completely
                if (DbService.getDb(dbName).removeItem(key, sessionId, value)) {
                    return Response.ok().build();
                } else {
                    return Response.noContent().build();
                }
            } else if (index != null) {
                // delete by index
                if (!(object instanceof List)) {
                    throw new IllegalArgumentException("Unexpected index " + index + ". Value is not a collection");
                }
                List valList = (List) object;
                if (index >= valList.size()) {
                    throw new IllegalArgumentException("Bad index " + index);
                }
                valList.remove(index.intValue());
                if (DbService.getDb(dbName).putItem(key, valList, sessionId, value)) {
                    return Response.ok().build();
                } else {
                    return Response.noContent().build();
                }
            } else if (valId != null) {
                if (!(object instanceof List)) {
                    if (valId.equals(DbService.getIdValue(object))) {
                        // remove the whole key
                        if (DbService.getDb(dbName).removeItem(key, sessionId, value)) {
                            return Response.ok().build();
                        } else {
                            return Response.noContent().build();
                        }
                    }
                } else {
                    List valList = (List) object;
                    ListIterator iter = valList.listIterator();
                    boolean modified = false;
                    while (iter.hasNext()) {
                        Object val = iter.next();
                        if (valId.equals(DbService.getIdValue(val))) {
                            // remove this value item
                            iter.remove();
                            modified = true;
                        }
                    }
                    if (modified && DbService.getDb(dbName).putItem(key, valList, sessionId, value)) {
                        return Response.ok().build();
                    } else {
                        return Response.noContent().build();
                    }
                }
            }
            return Response.noContent().build();
        } catch (IllegalArgumentException ex) {
            log.warning(ex.getMessage());
            return Response.serverError().entity(ex.getMessage()).build();
        } catch (Exception ex) {
            log.log(Level.WARNING, "deleteObject error", ex);
            return Response.serverError().entity(ex.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{key}")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.TEXT_PLAIN})
    public Response deleteString(@HeaderParam("API_KEY") String apiKey, @HeaderParam("SESSION_ID") String sessionId,
                                 @PathParam("dbName") String dbName, @PathParam("key") String key,
                                 @QueryParam("index") Integer index, @QueryParam("id") Long id,
                                 String value) {
        return deleteObject(apiKey, sessionId, dbName, key, index, id, value);
    }

    @DELETE
//    @Path("/")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response dropDb(@HeaderParam("API_KEY") String apiKey, @HeaderParam("SESSION_ID") String sessionId,
                           @PathParam("dbName") String dbName) {
        try {
            if (!ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.WRITE, dbName)) {
                throw new IllegalArgumentException("Bad or missing API_KEY header");
            }

            if (DbService.dropDb(dbName, sessionId)) {
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

    /**
     * Use Media Type TEXT_PLAIN for regular strings, APPLICATION_JSON_TYPE for everything else
     */
    protected String prepareMediaType(Object value) {
        if (value instanceof String) {
            return MediaType.TEXT_PLAIN_TYPE.toString();
        } else if (value instanceof FileItemStream) {
            return ((FileItemStream) value).getFileItem().guessMimeType();
        } else if (value instanceof FileItem) {
            return ((FileItem) value).guessMimeType();
        } else {
            return MediaType.APPLICATION_JSON_TYPE.toString();
        }
    }

    protected boolean isNullOrEmptyValue(Object value) {
        if (value == null) {
            return true;
        } else if (value instanceof List && ((List) value).isEmpty()) {
            return true;
        } else if (value instanceof Map && ((Map) value).isEmpty()) {
            return true;
        } else if (value instanceof String && ((String) value).isEmpty()) {
            return true;
        }
        return false;
    }
}