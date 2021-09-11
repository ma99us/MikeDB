package org.maggus.mikedb.services;

import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Log
public class ApiKeysService {

    public enum Access {READ, WRITE}

    private final String API_KEYS_KEY = "api-keys";
    private final String TEST_DB_API_KEY = "5up3r53cr3tK3y";
    private final String TEST_WC_DB_API_KEY = "T3st53cr3tK3y";

    private DbService config;

    private static ApiKeysService instance;

    private ApiKeysService() {
        // singleton
        log.info("Initializing Mike-DB...");
    }

    protected DbService getConfig() {
        if (config == null) {
            config = DbService.getConfig();
            populateDefaultKeys();
        }
        return config;
    }

    private void populateDefaultKeys() {
        Map keysValues = (Map) config.getItem(API_KEYS_KEY);
        if (keysValues == null) {
            keysValues = new LinkedHashMap<String, Object>();
        }

        if (keysValues.get(TEST_DB_API_KEY) == null || keysValues.get(TEST_WC_DB_API_KEY) == null) {
            log.warning("Default 'testDB' keys are missing. Adding them.");

            // TEST_DB_API_KEY
            Map<String, Object> apiKey = new LinkedHashMap<String, Object>();
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
            keysValues.put(TEST_DB_API_KEY, apiKey);

            // TEST_WC_DB_API_KEY
            apiKey = new LinkedHashMap<String, Object>();
            dbs = new ArrayList<>();
            db = new LinkedHashMap<String, Object>();
            db.put("access", Access.READ.toString());
            db.put("dbName", ":memory:.test*");
            dbs.add(db);
            apiKey.put("dbs", dbs);
            keysValues.put(TEST_WC_DB_API_KEY, apiKey);

            config.putItem(API_KEYS_KEY, keysValues, null, keysValues);
        }

        log.info("ApiKeysService loaded " + keysValues.size() + " API keys records");
    }

    protected boolean _isValidApiKey(String apiKey, Access access, String dbName) {
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }
        Map apiKeys = (Map) getConfig().getItem(API_KEYS_KEY);
        if (apiKeys == null) {
            return false;
        }
        Map<String, Object> key = (Map<String, Object>) apiKeys.get(apiKey);
        if (key == null) {
            return false;
        }
        List<Map<String, Object>> dbs = (List<Map<String, Object>>) key.get("dbs");
        if (dbs == null) {
            return false;
        }
        for (Map<String, Object> db : dbs) {
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
        boolean isValid = instance._isValidApiKey(apiKey, access, dbName);
        if (!isValid) {
            log.warning("Invalid key \"" + apiKey + "\" for DB \"" + dbName + "\", access=" + access);
        }
        return isValid;
    }
}


