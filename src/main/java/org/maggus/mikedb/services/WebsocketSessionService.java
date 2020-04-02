package org.maggus.mikedb.services;

import lombok.Data;
import lombok.extern.java.Log;

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

    public static void notifySessionsDbEvent(String dbName, String key, Object value, String sessionId) {
        List<SessionHandler> dbSessions = getInstance().getDbSessions(dbName);
        if(dbSessions != null){
            DbEvent event = new DbEvent();
            event.setSessionId(sessionId);
            event.setEvent(value != null ? DbEvent.Type.UPDATED.toString() : DbEvent.Type.DELETED.toString());
            event.setKey(key);
            event.setValue(value);
            for(SessionHandler handler : dbSessions){
                handler.sendObject(event);
            }
        }
    }

    public static void notifySessionsMessage(String dbName, String message, String sessionId) {
        List<SessionHandler> dbSessions = getInstance().getDbSessions(dbName);
        if (dbSessions != null) {
            String msg = "\"" + sessionId + "\" > " + message;
            for (SessionHandler handler : dbSessions) {
                handler.sendMessage(msg);
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
        private String sessionId;

        public SessionHandler(Session session, String dbName, String apiKey) {
            this.session = session;
            this.dbName = dbName;
            this.apiKey = apiKey;
        }

        public void onOpen() {
            //sendMessage("> Session "+session.getId()+" has opened database \"" + dbName + "\"");
            setSessionId(dbName + "-" + session.getId());   // TODO: add some unique number here?
            log.info("Session \"" + getSessionId() + "\" has opened database \"" + dbName + "\"");

            DbEvent event = new DbEvent();
            event.setSessionId(getSessionId());
            event.setEvent(DbEvent.Type.OPENED.toString());
            sendObject(event);
        }

        public void onClose() {
            log.info("Session " + getSessionId() + " has closed");
        }

        public void onMessage(String message) {
            log.info("Message from " + getSessionId() + ": " + message);
            //sendMessage("> " + message);
            WebsocketSessionService.notifySessionsMessage(getDbName(), message, getSessionId());
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

@Data
class DbEvent {
    enum Type {OPENED, INSERTED, UPDATED, DELETED, DROPPED, CLOSED}

    private String sessionId;
    private String event;
    private String key;
    private Object value;
}

