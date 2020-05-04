package org.maggus.mikedb;

import lombok.extern.java.Log;
import org.maggus.mikedb.data.ErrorEvent;
import org.maggus.mikedb.services.ApiKeysService;
import org.maggus.mikedb.services.JsonUtils;
import org.maggus.mikedb.services.WebsocketSessionService;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

//@ServerEndpoint(value = "/subscribe/{dbName}",
//        configurator = SimpleWebSocketConfigurator.class)
@ServerEndpoint("/subscribe/{dbName}")
@Log
public class DbWebSocketResource {

    @OnOpen
    public void onOpen(Session session) {
        log.info(session.getId() + " has opened a connection");
    }

    @OnClose
    public void onClose(Session session) {
        log.info(session.getId() + " has closed a connection");
        String dbName = session.getRequestParameterMap().get("dbName").get(0);
        WebsocketSessionService.closeSession(session, dbName);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            if ("PING".equalsIgnoreCase(message)) {
                // keep-alive heartbeat
                synchronized (session) {
                    session.getBasicRemote().sendText("PONG");
                }
                return;
            }
            log.info(session.getId() + " sent message: " + message);
            String dbName = session.getRequestParameterMap().get("dbName").get(0);
            WebsocketSessionService.SessionHandler handler = WebsocketSessionService.getInstance().getSession(session, dbName);
            if(handler == null) {
                // expect API_KEY message first
                String apiKey = decodeApiKey(message);
                if (apiKey == null || !ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.READ, dbName)) {
                    //throw new IllegalArgumentException("Session is not opened. Bad or missing API_KEY message");
                    sendError(session, new IllegalArgumentException("Bad or missing API_KEY. Session terminated."));
                    session.close();
                    return;
                }
                handler = WebsocketSessionService.openSession(session, dbName, apiKey);
            } else {
                //parse the rest of possible messages
                handler.onMessage(message);
            }
        } catch (IllegalArgumentException ex) {
            log.warning("onMessage IllegalArgumentException" + ex.getMessage());
            onError(session, ex);
        } catch (Exception ex) {
            log.log(Level.WARNING, "onMessage error", ex);
            onError(session, ex);
        }
    }

    @OnError
    public void onError(Session session, Throwable ex) {
        log.log(Level.SEVERE, "onError", ex);
        String dbName = session.getRequestParameterMap().get("dbName").get(0);
        WebsocketSessionService.SessionHandler handler = WebsocketSessionService.getInstance().getSession(session, dbName);
        if (handler != null) {
            handler.onError(ex);
        } else {
            sendError(session, ex);
        }
    }

    protected void sendError(Session session, Throwable ex) {
        try {
            ErrorEvent err = new ErrorEvent();
            err.setException(ex.getClass().getSimpleName());
            err.setMessage(ex.getMessage());
            synchronized (session) {
                session.getBasicRemote().sendText(JsonUtils.objectToString(err));
            }
        } catch (IOException exx) {
            log.log(Level.SEVERE, "sendError", exx);
        }
    }

    protected String decodeApiKey(String message){
        Object object = JsonUtils.stringToObject(message);
        if(!(object instanceof Map)){
            return null;
        }
        Object value = ((Map) object).get("API_KEY");
        return (String)value;
    }
}