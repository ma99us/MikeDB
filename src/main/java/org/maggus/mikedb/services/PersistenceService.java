package org.maggus.mikedb.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.java.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

@Log
public class PersistenceService {

    private final DbService db;
    private Properties props = new Properties();
    private File itemsFile;

    protected PersistenceService(DbService db) {
        this.db = db;
    }

    private File getItemsFile() throws IOException {
        if (itemsFile == null) {
            String homeDir = System.getProperty("user.home");
            Path path = Paths.get(homeDir, ".mikedb", db.dbName, "items.db");
            File file = path.toFile();
            File dbDir = file.getParentFile();
            if (!dbDir.isDirectory() && !dbDir.mkdirs()) {
                throw new IOException("Can not create storage directory " + dbDir.getAbsolutePath());
            }
            itemsFile = file;
        }
        return itemsFile;
    }

    private File getValuesFile(String name) throws IOException {
        String homeDir = System.getProperty("user.home");
        Path path = Paths.get(homeDir, ".mikedb", db.dbName, name);
        return path.toFile();
    }

    /**
     * Load database content from the file system
     * @throws IOException
     */
    public void load() throws IOException {
        FileInputStream fi = null;
        try {
            File file = getItemsFile();
            if(!file.isFile()){
                log.warning("No such database file: " + file.getAbsolutePath());
                return;
            }
            fi = new FileInputStream(file);
            props.load(fi);
        } finally {
            if (fi != null) {
                fi.close();
            }
        }

        final Map<String, Object> items = db.getItems();
        for (String key : props.stringPropertyNames()) {
            String property = props.getProperty(key);
            if(property.startsWith("@v.") && property.endsWith(".db")){
                // read from external file
                File file = getValuesFile(property.substring(1));
                if (file.isFile()) {
                    items.put(key, fileToObject(file));
                }
            } else {
                items.put(key, stringToObject(property));
            }
        }
    }

    /**
     * Process the latest database change. Store what's updated to the file system.
     * @param key
     * @param value
     * @throws IOException
     */
    public void store(String key, Object value) throws IOException {
        if(value == null){
            //delete old value files if any
            String property = props.getProperty(key);
            if (property != null && property.startsWith("@v.") && property.endsWith(".db")) {
                // read from external file
                File file = getValuesFile(property.substring(1));
                if (file.isFile()) {
                    file.delete();
                }
            }
            props.remove(key);
        } else {
            String json = objectToString(value);
            if(json.length() > 1024){
                // store to external file
                String val = "v." + key + ".db";
                File file = getValuesFile(val);
                if(objectToFile(value, file)){
                    props.put(key, "@" + val);
                }
            } else {
                props.put(key, json);
            }
        }

        FileOutputStream fr = null;
        try {
            File file = getItemsFile();
            fr = new FileOutputStream(file);
            props.store(fr, "MikeDB database: " + db.dbName);
        } finally {
            if (fr != null) {
                fr.close();
            }
        }
    }

    /**
     * Delete all files related to the database.
     * Assume database is empty. All records were already properly deleted.
     */
    public void delete() throws IOException {
        if (itemsFile == null) {
            log.warning("No database files");
            return;
        }
        File dbDir = itemsFile.getParentFile();
        deleteDir(dbDir);
    }

    private void deleteDir(File dir){
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteDir(file);
            }
        }
        dir.delete();
    }

    private ObjectMapper mapper = null;
    private ObjectMapper getObjectMapper(){
        if(mapper == null) {
            mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
        return mapper;
    }

    private boolean objectToFile(Object obj, File file) {
        try {
            getObjectMapper().writeValue(file, obj);
            return true;
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error storing object to file", ex);
            return false;
        }
    }

    private Object fileToObject(File file) {
        try {
            return getObjectMapper().readValue(file, Object.class);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error reading object from file", ex);
            return null;
        }
    }

    private String objectToString(Object obj) {
        try {
            return getObjectMapper().writeValueAsString(obj);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error storing object to string", ex);
            return null;
        }
    }

    private Object stringToObject(String json) {
        try {
            return getObjectMapper().readValue(json, Object.class);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error reading object from string", ex);
            return null;
        }
    }

    public static boolean isValidName(String name) {
        File f = new File(name);
        try {
            f.getCanonicalPath();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
