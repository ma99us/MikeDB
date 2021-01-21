package org.maggus.mikedb.services;

import lombok.Data;
import lombok.extern.java.Log;
import org.maggus.mikedb.data.DbEvent;
import org.maggus.mikedb.data.ErrorEvent;
import org.maggus.mikedb.data.SessionEvent;

import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

@Log
public class WebsocketSessionService {

    private Map<String, List<SessionHandler>> sessions = new LinkedHashMap<>();

    private static WebsocketSessionService instance;

    private WebsocketSessionService() {
        // singleton
    }

    public static WebsocketSessionService getInstance() {
        if (instance == null) {
            instance = new WebsocketSessionService();
        }
        return instance;
    }

    public static SessionHandler openSession(Session session, String dbName, String apiKey) throws IllegalArgumentException {
        DbService db = DbService.getDb(dbName); // just to test dbName
        SessionHandler handler = new SessionHandler(session, dbName, apiKey);
        if (!getInstance().addSession(handler, dbName)) {
            throw new IllegalArgumentException("Session id: " + session.getId() + " is already attached to the database \"" + dbName + "\"");
        }
        handler.onOpen();
        return handler;
    }

    public static void closeSession(Session session, String dbName) {
        SessionHandler handler = getInstance().getSession(session, dbName);
        if (handler != null) {
            getInstance().removeSession(handler, dbName);
            handler.onClose();
        }
    }

    private static void notifySessionsEvent(String dbName, SessionEvent.Type type, String sessionId) {
        SessionEvent event = new SessionEvent();
        event.setSessionId(sessionId);
        event.setEvent(type.toString());
        getInstance().sendMessage(event, dbName);
    }

    public static void notifySessionsDbEvent(String dbName, String key, Object value, String sessionId, Object val) {
        DbEvent event = new DbEvent();
        event.setSessionId(sessionId);
        event.setEvent(value != null ? DbEvent.Type.UPDATED.toString() : DbEvent.Type.DELETED.toString());
        event.setDbName(dbName);
        event.setKey(key);
        event.setValue(val);
        getInstance().sendMessage(event, dbName);
    }

    public static void notifySessionsMessage(String dbName, String message, String sessionId) {
        Object object = JsonUtils.stringToObject(message);
        if (object instanceof Map) {
            Map msg = (Map) object;
            if (!msg.containsKey("sessionId")) {
                msg.put("sessionId", sessionId);
            }
            if (!msg.containsKey("dbName")) {
                msg.put("dbName", dbName);
            }
            getInstance().sendMessage(msg, dbName);
        } else {
            String msg = "> \"" + sessionId + "\" says: " + message;
            getInstance().sendMessage(msg, dbName);
        }
    }

    public static boolean hasOpenSessions(String dbName) {
        List<SessionHandler> dbSessions = getInstance().getDbSessions(dbName);
        if (dbSessions == null || dbSessions.isEmpty()) {
            return false;
        }
        return dbSessions.stream().anyMatch(s -> s.isOpen());
    }

    public SessionHandler getSession(Session session, String dbName) {
        List<SessionHandler> dbSessions = getDbSessions(dbName);
        return dbSessions != null ? dbSessions.stream().filter(s -> s.getSession() == session).findAny().orElse(null) : null;
    }

    protected synchronized List<SessionHandler> getDbSessions(String dbName) {
        List<SessionHandler> dbSessions = sessions.get(dbName);
        return dbSessions != null ? new ArrayList<SessionHandler>(dbSessions) : null; // return a copy for non-synchronized operations
    }

    protected synchronized boolean addSession(SessionHandler handler, String dbName) {
        List<SessionHandler> dbSessions = sessions.get(dbName);
        if (dbSessions == null) {
            dbSessions = new ArrayList<>();
            sessions.put(dbName, dbSessions);
        }
        if (dbSessions.contains(handler)) {
            return false;   // already have such session
        }
        return dbSessions.add(handler);
    }

    protected synchronized boolean removeSession(SessionHandler handler, String dbName) {
        List<SessionHandler> dbSessions = sessions.get(dbName);
        if (dbSessions == null) {
            return false;
        }
        return dbSessions.remove(handler);
    }

    protected void sendMessage(Object message, String dbName) {
        List<SessionHandler> dbSessions = getDbSessions(dbName);
        if (dbSessions != null) {
            for (SessionHandler handler : dbSessions) {
                handler.sendMessage(message);
                //handler.sendMessage(message, true);    //FIXME: always send async?
            }
        }
    }

    @Log
    @Data
    public static class SessionHandler {
        private final Session session;
        private final String apiKey;
        private final String dbName;
        private String sessionId;
        private final ExecutorService executorService = Executors.newSingleThreadExecutor();

        public SessionHandler(Session session, String dbName, String apiKey) {
            this.session = session;
            this.dbName = dbName;
            this.apiKey = apiKey;
        }

        public boolean isOpen() {
            return session.isOpen();
        }

        public void onOpen() {
            //sendMessage("> Session "+session.getId()+" has opened database \"" + dbName + "\"");
            setSessionId(dbName + "-" + session.getId());   // TODO: add some unique number here?
            log.info("Session \"" + getSessionId() + "\" has opened database \"" + dbName + "\"");

            DbEvent event = new DbEvent();
            event.setSessionId(getSessionId());
            event.setEvent(SessionEvent.Type.NEW.toString());
            sendMessage(event);  // send new session id to the client

            WebsocketSessionService.notifySessionsEvent(getDbName(), SessionEvent.Type.OPENED, getSessionId());
        }

        public void onClose() {
            log.info("Session " + getSessionId() + " has closed");
            executorService.shutdown();
            WebsocketSessionService.notifySessionsEvent(getDbName(), SessionEvent.Type.CLOSED, getSessionId());
        }

        public void onError(Throwable ex) {
            log.warning("Session " + getSessionId() + " had an error");
            ErrorEvent err = new ErrorEvent();
            err.setSessionId(getSessionId());
            err.setException(ex.getClass().getSimpleName());
            err.setMessage(ex.getMessage());
            sendMessage(err);
        }

        public void onMessage(String message) {
            //log.info("Message from " + getSessionId() + ": " + message);
            WebsocketSessionService.notifySessionsMessage(getDbName(), message, getSessionId());
        }

        public void sendMessage(final Object message) {
            executorService.execute(new Runnable() {
                public void run() {
                    sendMessage(message, false);
                }
            });
        }

        public void sendMessage(Object message, boolean async) {
            try {
                if (!(message instanceof String)) {
                    message = JsonUtils.objectToString(message);
                }
                if (message == null) {
                    throw new IllegalStateException("message can not be null");
                }
                synchronized (session) {
                    if (async) {
                        session.getAsyncRemote().sendText((String) message, new SendHandler() {
                            @Override
                            public void onResult(SendResult sendResult) {
                                if (!sendResult.isOK()) {
                                    log.warning("sendMessage can not async send to session " + getSessionId() + "; " + sendResult.getException().getMessage());
                                }
                            }
                        });
                    } else {
                        session.getBasicRemote().sendText((String) message);
                    }
                }
            } catch (java.lang.IllegalStateException ex) {
                log.warning("sendMessage can not send to session " + getSessionId() + "; " + ex.getMessage());
            } catch (IOException ex) {
                log.log(Level.SEVERE, "sendMessage", ex);
            }
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

