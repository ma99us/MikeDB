package org.maggus.mikedb.services;

import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Log
public class ApiKeysService {

    public enum Access {READ, WRITE}

    private final String DEFAULT_API_KEY = "5up3r53cr3tK3y";

    private DbService config;

    private static ApiKeysService instance;

    private ApiKeysService(){
        // singleton
    }

    protected DbService getConfig() {
        if (config == null) {
            config = DbService.getConfig();
            if (config.getItem(DEFAULT_API_KEY) == null) {
                populateDefaultKey();
            }
            log.info("ApiKeysService found " + config.getItems().size() + " API keys");
        }
        return config;
    }

    private void populateDefaultKey() {
        Map<String, Object> apiKey = new LinkedHashMap<String, Object>();
        apiKey.put("key", DEFAULT_API_KEY);
        Map<String, Object> db = new LinkedHashMap<String, Object>();
        db.put("access", Access.WRITE.toString());
        db.put("dbName", "*");
        ArrayList<Map<String, Object>> dbs = new ArrayList<>();
        dbs.add(db);
        apiKey.put("dbs", dbs);
        config.putItem(DEFAULT_API_KEY, apiKey);
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
            if ((db.get("dbName").equals(dbName) || db.get("dbName").equals("*"))
                    && (db.get("access").equals(access.toString()) || db.get("access").equals(Access.WRITE.toString()))) {
                return true;
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


