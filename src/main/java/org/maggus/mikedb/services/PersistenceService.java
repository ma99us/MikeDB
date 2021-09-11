package org.maggus.mikedb.services;

import lombok.extern.java.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Log
public class PersistenceService {

    private final DbService db;
    private Map<String, File> valuesFiles = new HashMap<>();

    protected PersistenceService(DbService db) {
        this.db = db;
    }

    private File getDbDir() throws IOException {
        String homeDir = System.getProperty("user.home");
        Path path = Paths.get(homeDir, ".mikedb", db.dbName);
        File dir = path.toFile();
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Can not create storage directory " + dir.getAbsolutePath());
        }
        return dir;
    }

    private File[] getAllDbFiles() throws IOException {
        File dbDir = getDbDir();
        return dbDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".db");
            }
        });
    }

    private File getDbValueFile(String name) throws IOException {
        File file = valuesFiles.get(name);
        if (file != null) {
            return file;
        }
        file = new File(getDbDir(), name + ".db");
        valuesFiles.put(name, file);
        return file;
    }

    /**
     * Load database content from the file system
     *
     * @throws IOException
     */
    public void loadAll() throws IOException {
        File[] files = getAllDbFiles();

        final Map<String, Object> items = db.getItems();
        if (files != null) {
            for (File file : files) {
                String fName = file.getName();
                if (!file.isFile() || !fName.endsWith(".db")) {
                    continue;
                }
                String key = fName.substring(0, fName.length() - 3);
                items.put(key, JsonUtils.fileToObject(file));
            }
        }
    }

    /**
     * Process the latest database change. Store what's updated to the file system.
     *
     * @param key
     * @param value
     * @throws IOException
     */
    public void store(String key, Object value) throws IOException {
        if (value == null) {
            //delete old value files if any
            File file = getDbValueFile(key);
            if (file.isFile()) {
                file.delete();
            }
        } else {
            String json = JsonUtils.objectToString(value);
            File file = getDbValueFile(key);
            JsonUtils.objectToFile(value, file);
        }
    }

    /**
     * Delete all files related to the database.
     * Assume database is empty. All records were already properly deleted.
     */
    public void delete() throws IOException {
        File[] files = getAllDbFiles();
        if (files == null || files.length == 0) {
            log.warning("No database files");
            return;
        }
        for (File file : files) {
            file.delete();
        }
        deleteDir(getDbDir());
    }

    private void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteDir(file);
            }
        }
        dir.delete();
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
