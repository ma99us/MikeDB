package org.maggus.mikedb.services;

import lombok.extern.java.Log;
import org.maggus.mikedb.data.FileItem;
import org.maggus.mikedb.data.FileItemStream;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.maggus.mikedb.services.JsonUtils.MAX_SAFE_INTEGER;

@Log
public class DbService {

    private static Map<String, DbService> dbs = new LinkedHashMap<>();
    public static final String CONFIG_DB_NAME = ".config";
    public static final String IN_MEMORY_DB_NAME_PREFIX = ":memory:";
    public static final String PRIVATE_DB_NAME_PREFIX = ".";

    private final PersistenceService storage = new PersistenceService(this);
    private final Map<String, Object> items = new LinkedHashMap<>();
    public final String dbName;
    public final boolean inMemory;

    private Date lastUpdated;


    private DbService(String dbName) {
        this.dbName = dbName;
        this.inMemory = dbName.startsWith(IN_MEMORY_DB_NAME_PREFIX);
        load();
    }

    protected static DbService getConfig() {
        return _getDb(CONFIG_DB_NAME);
    }

    public synchronized static DbService getDb(String dbName) throws IllegalArgumentException {
        if (CONFIG_DB_NAME.equalsIgnoreCase(dbName) ||
                (!dbName.startsWith(IN_MEMORY_DB_NAME_PREFIX) && !PersistenceService.isValidName(dbName))) {
            throw new IllegalArgumentException("Illegal database name \"" + dbName + "\"");
        }
        return _getDb(dbName);
    }

    public static synchronized boolean dropDb(String dbName, String sessionId) {
        DbService dbService = dbs.get(dbName);
        if (dbService == null) {
            return false;
        }
        log.warning("Dropping " + (dbService.inMemory ? "in-memory" : "") + " database: \"" + dbName + "\"");
        boolean allRemoved = dbService.removeAllItems(sessionId);
        if (allRemoved) {
            dbs.remove(dbName);
        }
        return allRemoved;
    }

    private static DbService _getDb(String dbName) {
        DbService dbService = dbs.get(dbName);
        if (dbService == null) {
            dbService = new DbService(dbName);
            dbs.put(dbName, dbService);
            log.info("Opened " + (dbService.inMemory ? "in-memory" : "") + " database: \"" + dbName + "\"");
        }
        return dbService;
    }

    public Object getItem(String key) {
        return getItem(key, null);
    }

    public Object getItem(String key, String[] fieldNames) {
        Object value = items.get(key);
        if (value == null || fieldNames == null) {
            return value;
        }

        // filter value object or list of objects by the field names
        if (value instanceof List) {
            List<Object> filtered = new ArrayList<>();
            for (Object val : (List) value) {
                filtered.add(JsonUtils.filterObjectFields(val, fieldNames));
            }
            return filtered;
        } else {
            return JsonUtils.filterObjectFields(value, fieldNames);
        }
    }

    public synchronized boolean putItem(String key, Object value, String sessionId, Object val) throws IllegalArgumentException {
        if (!PersistenceService.isValidName(key)) {
            throw new IllegalArgumentException("Illegal key \"" + key + "\"");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value can not be null");
        }

        populateObjectId(key, value);   // augment a Map Object with the generated "id" field, if missing

        items.put(key, (value instanceof FileItemStream) ? ((FileItemStream) value).getFileItem() : value);
        return store(key, value, sessionId, val);
    }

    public synchronized boolean removeItem(String key, String sessionId, Object val) throws IllegalArgumentException {
        if (!PersistenceService.isValidName(key)) {
            throw new IllegalArgumentException("Illegal key \"" + key + "\"");
        }
        Object prevVal = items.remove(key);
        return store(key, null, sessionId, val) && prevVal != null;
    }

    protected synchronized boolean removeAllItems(String sessionId) {
        Set<String> keys = new LinkedHashSet<>(items.keySet());
        boolean allRemoved = true;
        for (String key : keys) {
            allRemoved &= removeItem(key, sessionId, null);
        }
        log.warning("removeAllItems; allRemoved=" + allRemoved);
        if (allRemoved && !inMemory) {
            try {
                storage.delete();
            } catch (IOException ex) {
                log.log(Level.SEVERE, dbName + " delete failed", ex);
                return false;
            }
        }
        return allRemoved;
    }

    protected Map<String, Object> getItems() {
        return items;
    }

    private Long populateObjectId(String key, Object value) {
        if (value instanceof List) {
            ((List) value).stream().forEach(v -> populateObjectId(key, v));
            return null; // lists have no ids themselves
        } else if (value instanceof Map) {
            Long tryId = getIdValue(value);
            if (tryId != null) {
                return tryId; // Map value already has some id
            }

            // populate the map with a new id
            Long id = generateDbId(key, value);
            ((Map) value).put("id", id);
            return id;  // new id
        } else if (value instanceof FileItemStream) {
            Long tryId = getIdValue(value);
            if (tryId != null) {
                return tryId; // FileItem value already has some id
            }

            // populate the FileItem with a new id
            Long id = generateDbId(key, value);
            ((FileItemStream) value).getFileItem().setId(id);
            return id;
        } else {
            // nothing to add to this Value
            return generateDbId(key, value);
        }
    }

    /**
     *  generate some "unique" id for the new Value map
     * @return Long id number
     */
    private Long generateDbId(String key, Object value) {
        return (System.nanoTime() + value.hashCode() + key.hashCode() + dbName.hashCode()) % MAX_SAFE_INTEGER;
    }

    protected boolean store(String key, Object value, String sessionId, Object val) {
        notifyWebsocketSessions(key, value, sessionId, val);
        lastUpdated = new Date();

        if (inMemory) {
            return true;
        }
        try {
            storage.store(key, value);
            if (value != null && value instanceof FileItemStream) {
                log.info("File '" + key + "' => " + ((FileItemStream)value).getFileItem().getName() + " added to \"" + dbName + "\" database;"); //#DEBUG
            } else if (value != null) {
                log.info("Record '" + key + "' => " + value.getClass().getSimpleName() + " added to \"" + dbName + "\" database; total records: " + items.size()); //#DEBUG
            } else {
                log.info("Key '" + key + "' removed from \"" + dbName + "\" database; total records:" + items.size()); //#DEBUG
            }
        } catch (IOException ex) {
            log.log(Level.SEVERE, dbName + " store failed", ex);
        }
        return true;
    }

    protected void load() {
        if (inMemory) {
            return;
        }
        try {
            storage.loadAll();
            log.info("Database \"" + dbName + "\" loaded with " + getItems().size() + " records");
        } catch (IOException ex) {
            log.log(Level.SEVERE, dbName + " loadAll failed", ex);
        }
    }

    /**
     * Checks if the DB did not have any updates for at least an hour
     *
     * @return true if DB had no updates recently
     */
    protected boolean isAbandoned() {
        if (lastUpdated == null) {
            return false;
        }
        long millis = Math.abs(new Date().getTime() - lastUpdated.getTime());
        long hours = TimeUnit.HOURS.convert(millis, TimeUnit.MILLISECONDS);
        return hours >= 1;
    }

    protected boolean cleanup() {
        if (this.inMemory && !WebsocketSessionService.hasOpenSessions(this.dbName) && isAbandoned()) {
            // If a memory DB has no open sessions (no one is listening on it), and was not updated for an hour,
            // then just drop the whole DB!
            log.warning("Cleaning up DB \"" + dbName + "\"");
            return DbService.dropDb(this.dbName, "cleanup");
        }
        return false;
    }

    protected void notifyWebsocketSessions(String key, Object value, String sessionId, Object val) {
        if (isPrivateDb() || isPrivateKey(key)) {
            return; // do ont send notifications
        }
        WebsocketSessionService.notifySessionsDbEvent(dbName, key, value, sessionId, val);
    }

    /**
     * Private DB has all keys private, so no DB actions websocket notifications will be sent
     *
     * @return
     */
    public boolean isPrivateDb() {
        return dbName.startsWith(PRIVATE_DB_NAME_PREFIX);
    }

    /**
     * Actions on private keys do not send websocket notifications
     *
     * @param key
     * @return
     */
    public boolean isPrivateKey(String key) {
        return key.startsWith(PRIVATE_DB_NAME_PREFIX);
    }

    public static Long getIdValue(Object value) {
        if (value instanceof Map) {
            Object valId = ((Map) value).get("id");
            if (valId instanceof Long) {
                return (Long) valId;
            } else if (valId instanceof Integer) {
                return ((Integer) valId).longValue();
            } else if (valId instanceof String) {
                try {
                    return Long.parseLong((String) valId);  // % MAX_SAFE_INTEGER;
                } catch (NumberFormatException ex) {
                    log.warning("Unexpected 'id' value: " + valId);
                    return null;
                }
            } else if (valId != null) {
                log.warning("Unexpected 'id' type: " + valId);
            }
        } else if (value instanceof FileItemStream) {
            return ((FileItemStream) value).getFileItem().getId();
        } else if (value instanceof FileItem) {
            return ((FileItem) value).getId();
        }
        return null;
    }

    protected static List<String> getOpenedDbNames(boolean inMemoryOnly) {
        return dbs.entrySet()
                .stream()
                .filter(map -> (map.getValue().inMemory && inMemoryOnly) || !inMemoryOnly)
                .map(map -> map.getKey())
                .collect(Collectors.toList());
    }

    protected static boolean cleanupOpenedDb(String dbName) {
        DbService dbService = dbs.get(dbName);
        if (dbService == null) {
            return false;
        }

        return dbService.cleanup();
    }
}
