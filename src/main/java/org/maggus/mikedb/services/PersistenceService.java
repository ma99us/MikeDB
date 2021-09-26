package org.maggus.mikedb.services;

import lombok.extern.java.Log;
import org.maggus.mikedb.data.FileItem;
import org.maggus.mikedb.data.FileItemStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Log
public class PersistenceService {
    public static final String DB_VALUE_EXT = ".db";
    public static final String DB_FILE_EXT = ".dbfile";

    private final DbService db;
    private Map<String, File> valuesFiles = new HashMap<>();
    private Map<String, File> binaryFiles = new HashMap<>();

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
                return name.endsWith(DB_VALUE_EXT) || name.endsWith(DB_FILE_EXT);
            }
        });
    }

    private File getDbValueFile(String name) throws IOException {
        File file = valuesFiles.get(name);
        if (file != null) {
            return file;
        }
        file = new File(getDbDir(), name + DB_VALUE_EXT);
        valuesFiles.put(name, file);
        return file;
    }

    private File getDbBinaryFile(String name, String type) throws IOException {
        File file = binaryFiles.get(name);
        if (file != null) {
            return file;
        }
        String fName = name;
        if (type != null && !fName.endsWith("." + type)) {
            fName += "." + type;
        }
        file = new File(getDbDir(), fName + DB_FILE_EXT);
        binaryFiles.put(name, file);
        return file;
    }

    private File findDbBinaryFile(String name) throws IOException {
        return binaryFiles.get(name);
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
                if (file.isFile() && fName.endsWith(DB_VALUE_EXT)) {
                    String key = fName.substring(0, fName.length() - DB_VALUE_EXT.length());
                    items.put(key, JsonUtils.fileToObject(file));
                    valuesFiles.put(key, file);
                } else if (file.isFile() && fName.endsWith(DB_FILE_EXT)) {
                    FileItem fileItem = new FileItem(file);
                    String key = fileItem.getName();    // name is a key
                    items.put(key, fileItem);
                    binaryFiles.put(key, file);
                }
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
            if (file != null && file.isFile()) {
                file.delete();
            }
            File binaryFile = findDbBinaryFile(key);
            if (binaryFile != null && binaryFile.isFile()) {
                binaryFile.delete();
            }
        } else if (value instanceof FileItemStream) {
            FileItemStream fi = (FileItemStream) value;
            FileItem fileItem = fi.getFileItem();
            // augment the key with the file extension to get the final db file name
            File file = getDbBinaryFile(key, fileItem.getType());
            // store full local file path as file name
            fileItem.setFileName(file.getAbsolutePath());
            // finally write to a db file
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(file);
                int read = 0;
                byte[] bytes = new byte[1024];
                while ((read = fi.getFileInputStream().read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
            } finally {
                if (out != null) {
                    out.flush();
                    out.close();
                }
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
        if (name.contains("\\") || name.contains("/")) {
            return false;
        }
        File f = new File(name);
        try {
            f.getCanonicalPath();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
