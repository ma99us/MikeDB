package org.maggus.mikedb;

import lombok.Data;
import lombok.extern.java.Log;
import org.maggus.mikedb.services.ApiKeysService;
import org.maggus.mikedb.services.JsonUtils;
import org.maggus.mikedb.services.WebsocketSessionService;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

@ServerEndpoint("/websocket/{dbName}")
@Log
public class WebSocketResource {

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
            log.info(session.getId() + " sent message: " + message);
            String dbName = session.getRequestParameterMap().get("dbName").get(0);
            WebsocketSessionService.SessionHandler handler = WebsocketSessionService.getSession(session, dbName);
            if(handler == null) {
                // expect API_KEY message first
                String apiKey = decodeApiKey(message);
                if (apiKey == null || !ApiKeysService.isValidApiKey(apiKey, ApiKeysService.Access.READ, dbName)) {
                    throw new IllegalArgumentException("Session is not opened. Bad or missing API_KEY message");
                }
                handler = WebsocketSessionService.openSession(session, dbName, apiKey);
            } else {
                handler.onMessage(message);
            }
        } catch (IllegalArgumentException ex) {
            log.warning(ex.getMessage());
            sendError(ex, session);
        } catch (Exception ex) {
            log.log(Level.WARNING, "onMessage error", ex);
            sendError(ex, session);
        }
    }

    @OnError
    public void onError(Throwable ex) {
        log.log(Level.SEVERE, "onError", ex);
    }

    protected void sendError(Throwable ex, Session session) {
        try {
            ErrorObject err = new ErrorObject();
            err.setId(session.getId());
            err.setException(ex.getClass().getSimpleName());
            err.setMessage(ex.getMessage());
            session.getBasicRemote().sendText(JsonUtils.objectToString(err));
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

@Data
class ErrorObject {
    private String exception;
    private String message;
    private String id;
}



