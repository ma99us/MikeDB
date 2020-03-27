package org.maggus.mikedb.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

@Log
public class JsonUtils {

    private static ObjectMapper mapper = null;

    private static ObjectMapper getObjectMapper() {
        if (mapper == null) {
            mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
        return mapper;
    }

    public static boolean objectToFile(Object obj, File file) {
        try {
            getObjectMapper().writeValue(file, obj);
            return true;
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error storing object " + obj + " to file " + file.getAbsolutePath() + " - " + ex.getMessage());
            return false;
        }
    }

    public static Object fileToObject(File file) {
        try {
            return getObjectMapper().readValue(file, Object.class);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error reading object from file " + file.getAbsolutePath() + " - " + ex.getMessage());
            return null;
        }
    }

    public static <T> T fileToObject(File file, Class<T> clazz) {
        try {
            return getObjectMapper().readValue(file, clazz);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error reading " + clazz + " from file " + file.getAbsolutePath() + " - " + ex.getMessage());
            return null;
        }
    }

    public static String objectToString(Object obj) {
        try {
            return getObjectMapper().writeValueAsString(obj);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error storing " + obj + " to string - " + ex.getMessage());
            return null;
        }
    }

    public static Object stringToObject(String json) {
        try {
            return getObjectMapper().readValue(json, Object.class);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error reading object from string " + json + " - " + ex.getMessage());
            return null;
        }
    }

    public static <T> T stringToObject(String json, Class<T> clazz) {
        try {
            return getObjectMapper().readValue(json, clazz);
        } catch (IOException ex) {
            log.log(Level.WARNING, "Error reading " + clazz + " from string " + json + " - " + ex.getMessage());
            return null;
        }
    }
}
