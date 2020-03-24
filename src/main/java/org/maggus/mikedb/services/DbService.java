package org.maggus.mikedb.services;

import lombok.extern.java.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

@Log
public class DbService {

    private static Map<String, DbService> dbs = new HashMap<String, DbService>();
    public static final String CONFIG_DB_NAME = ".config";

    private final PersistenceService storage = new PersistenceService(this);
    private final Map<String, Object> items = new HashMap<String, Object>();
    public final String dbName;

    private DbService(String dbName) {
        this.dbName = dbName;
        load();
    }

    protected static DbService getConfig() {
        return _getDb(CONFIG_DB_NAME);
    }

    public synchronized static DbService getDb(String dbName) throws IllegalArgumentException {
        if (CONFIG_DB_NAME.equalsIgnoreCase(dbName) || !PersistenceService.isValidName(dbName)) {
            throw new IllegalArgumentException("Illegal database name \"" + dbName + "\"");
        }
        return _getDb(dbName);
    }

    private static DbService _getDb(String dbName) {
        DbService dbService = dbs.get(dbName);
        if (dbService == null) {
            dbService = new DbService(dbName);
            dbs.put(dbName, dbService);
            log.info("Opened database: \"" + dbName + "\"");
        }
        return dbService;
    }

    public Object getObject(String key) {
        return items.get(key);
    }

    public boolean putObject(String key, Object value) throws IllegalArgumentException {
        if (!PersistenceService.isValidName(key)) {
            throw new IllegalArgumentException("Illegal key \"" + key + "\"");
        }
        Object prevVal = null;
        if (value == null) {
            prevVal = items.remove(key);
        } else {
            prevVal = items.put(key, value);
        }
        boolean res = store(key, value);
        if (!res) {
            return false;
        }
        return prevVal != null || value != null;
    }

    public Map<String, Object> getItems() {
        return items;
    }

    protected boolean store(String key, Object value) {
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
        try {
            storage.load();
            log.info("Database \"" +dbName + "\" loaded with " + getItems().size() + " records");
        } catch (IOException ex) {
            log.log(Level.SEVERE, dbName + " load failed", ex);
        }
    }
}
