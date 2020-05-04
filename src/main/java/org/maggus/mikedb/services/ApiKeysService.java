package org.maggus.mikedb.services;

import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Log
public class ApiKeysService {

    public enum Access {READ, WRITE}

    private final String TEST_DB_API_KEY = "5up3r53cr3tK3y";
    private final String TEST_WC_DB_API_KEY = "T3st53cr3tK3y";

    private DbService config;

    private static ApiKeysService instance;

    private ApiKeysService(){
        // singleton
    }

    protected DbService getConfig() {
        if (config == null) {
            config = DbService.getConfig();
            if (config.getItem(TEST_DB_API_KEY) == null || config.getItem(TEST_WC_DB_API_KEY) == null) {
                populateDefaultKeys();
            }
            log.info("ApiKeysService found " + config.getItems().size() + " API keys");
        }
        return config;
    }

    private void populateDefaultKeys() {
        // TEST_DB_API_KEY
        Map<String, Object> apiKey = new LinkedHashMap<String, Object>();
        apiKey.put("key", TEST_DB_API_KEY);
        ArrayList<Map<String, Object>> dbs = new ArrayList<>();
        Map<String, Object> db = new LinkedHashMap<String, Object>();
        db.put("access", Access.WRITE.toString());
        db.put("dbName", ":memory:testDB");
        dbs.add(db);
        db = new LinkedHashMap<String, Object>();
        db.put("access", Access.WRITE.toString());
        db.put("dbName", "testDB");
        dbs.add(db);
        apiKey.put("dbs", dbs);
        config.putItem(TEST_DB_API_KEY, apiKey, null, apiKey);

        // TEST_WC_DB_API_KEY
        apiKey = new LinkedHashMap<String, Object>();
        apiKey.put("key", TEST_WC_DB_API_KEY);
        dbs = new ArrayList<>();
        db = new LinkedHashMap<String, Object>();
        db.put("access", Access.READ.toString());
        db.put("dbName", ":memory:.test*");
        dbs.add(db);
        apiKey.put("dbs", dbs);
        config.putItem(TEST_WC_DB_API_KEY, apiKey, null, apiKey);
    }

    protected boolean _isValidApiKey(String apiKey, Access access, String dbName) {
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }
        Object val = getConfig().getItem(apiKey);
        if (val == null) {
            return false;
        }
        Map<String, Object> key = (Map<String, Object>) val;
        Object dbs = key.get("dbs");
        if (dbs == null) {
            return false;
        }
        for (Map<String, Object> db : (List<Map<String, Object>>) dbs) {
            String keyDbName = (String) db.get("dbName");
            boolean nameMatches = false;
            String keyAccess = (String) db.get("access");
            if (keyDbName.equals(dbName) || keyDbName.equals("*")) {
                nameMatches = true;
            } else if (keyDbName.endsWith("*")) {
                keyDbName = keyDbName.substring(0, keyDbName.length() - 1);
                nameMatches = dbName.startsWith(keyDbName);
            }
            if (nameMatches && (keyAccess.equals(access.toString()) || keyAccess.equals(Access.WRITE.toString()))) {
                return true;    // good key
            }
        }
        return false;
    }

    public static boolean isValidApiKey(String apiKey, Access access, String dbName) {
        if (instance == null) {
            instance = new ApiKeysService();
        }
        return instance._isValidApiKey(apiKey, access, dbName);
    }
}


