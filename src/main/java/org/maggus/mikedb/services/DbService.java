package org.maggus.mikedb.services;

import lombok.extern.java.Log;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

@Log
public class DbService {

    private static Map<String, DbService> dbs = new LinkedHashMap<>();
    public static final String CONFIG_DB_NAME = ".config";
    public static final String IN_MEMORY_DB_NAME_PREFIX = ":memory:";

    private final PersistenceService storage = new PersistenceService(this);
    private final Map<String, Object> items = new LinkedHashMap<>();
    public final String dbName;
    public final boolean inMemory;

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

    public static synchronized boolean dropDb(String dbName) {
        DbService dbService = dbs.get(dbName);
        if (dbService == null) {
            return false;
        }
        log.warning("Dropping " + (dbService.inMemory ? "in-memory" : "") + " database: \"" + dbName + "\"");
        boolean allRemoved = dbService.removeAllItems();
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
        return items.get(key);
    }

    public synchronized boolean putItem(String key, Object value) throws IllegalArgumentException {
        if (!PersistenceService.isValidName(key)) {
            throw new IllegalArgumentException("Illegal key \"" + key + "\"");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value can not be null");
        }
        items.put(key, value);
        return store(key, value);
    }

    public synchronized boolean removeItem(String key) throws IllegalArgumentException {
        if (!PersistenceService.isValidName(key)) {
            throw new IllegalArgumentException("Illegal key \"" + key + "\"");
        }
        Object prevVal = items.remove(key);
        return store(key, null) && prevVal != null;
    }

    protected synchronized boolean removeAllItems() {
        Set<String> keys = new LinkedHashSet<>(items.keySet());
        boolean allRemoved = true;
        for (String key : keys) {
            allRemoved &= removeItem(key);
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

    protected boolean store(String key, Object value) {
        notifyWebsocketSessions(key, value);

        if(inMemory){
            return true;
        }
        try {
            storage.store(key, value);
            if (value != null) {
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
        if(inMemory){
            return;
        }
        try {
            storage.load();
            log.info("Database \"" +dbName + "\" loaded with " + getItems().size() + " records");
        } catch (IOException ex) {
            log.log(Level.SEVERE, dbName + " load failed", ex);
        }
    }

    protected void notifyWebsocketSessions(String key, Object value){
       WebsocketSessionService.notifySessions(dbName, key, value);
    }
}
