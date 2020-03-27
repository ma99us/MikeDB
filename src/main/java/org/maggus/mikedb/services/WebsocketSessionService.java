package org.maggus.mikedb.services;

import lombok.Data;
import lombok.extern.java.Log;

import javax.websocket.MessageHandler;
import javax.websocket.Session;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

@Log
public class WebsocketSessionService {

    private Map<String, List<SessionHandler>> sessions = new LinkedHashMap<>();

    private static WebsocketSessionService instance;

    private WebsocketSessionService() {
        // singleton
    }

    protected static WebsocketSessionService getInstance() {
        if (instance == null) {
            instance = new WebsocketSessionService();
        }
        return instance;
    }

    public static synchronized SessionHandler openSession(Session session, String dbName, String apiKey) throws IllegalArgumentException {
        DbService db = DbService.getDb(dbName); // just to test dbName
        SessionHandler handler = new SessionHandler(session, dbName, apiKey);
        List<SessionHandler> dbSessions = getInstance().getDbSessions(dbName);
        if (dbSessions == null) {
            dbSessions = new ArrayList<>();
            getInstance().sessions.put(dbName, dbSessions);
        }
        if(dbSessions.contains(handler)){
            throw new IllegalArgumentException("Session id: " + session.getId() + " is already attached to the database \"" + dbName + "\"");
        }
        dbSessions.add(handler);
        handler.onOpen();
        return handler;
    }

    public static synchronized void closeSession(Session session, String dbName){
        SessionHandler handler = getSession(session, dbName);
        if (handler != null) {
            handler.onClose();
            getInstance().getDbSessions(dbName).remove(handler);
        }
    }

    public static SessionHandler getSession(Session session, String dbName){
        List<SessionHandler> dbSessions = getInstance().getDbSessions(dbName);
        return dbSessions != null ? dbSessions.stream().filter(s -> s.getSession() == session).findAny().orElse(null) : null;
    }

    public static void notifySessions(String dbName, String key, Object value) {
        List<SessionHandler> dbSessions = getInstance().getDbSessions(dbName);
        if(dbSessions != null){
            for(SessionHandler handler : dbSessions){
                handler.sendMessage("> Database Key updated: \"" + key + "\"");
            }
        }
    }

    protected List<SessionHandler> getDbSessions(String dbName) {
        return sessions.get(dbName);
    }

    @Log
    @Data
    public static class SessionHandler {
        private final Session session;
        private final String apiKey;
        private final String dbName;

        public SessionHandler(Session session, String dbName, String apiKey) {
            this.session = session;
            this.dbName = dbName;
            this.apiKey = apiKey;
        }

        public void onOpen() {
            log.info("Session "+session.getId()+" has opened database \"" + dbName + "\"");
            sendMessage("> Session "+session.getId()+" has opened database \"" + dbName + "\"");
        }

        public void onClose() {
            log.info("Session " + session.getId() + " has closed");
        }

        public void onMessage(String message) {
            log.info("Message from " + session.getId() + ": " + message);
            sendMessage("> " + message);
        }

        public void sendMessage(String message) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException ex) {
                log.log(Level.SEVERE, "sendMessage", ex);
            }
        }

        public void sendObject(Object message) {
            sendMessage(JsonUtils.objectToString(message));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SessionHandler that = (SessionHandler) o;
            return Objects.equals(getSession(), that.getSession());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getSession());
        }
    }
}

