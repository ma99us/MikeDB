package org.maggus.mikedb.data;

import lombok.Data;

@Data
public class ErrorEvent {
    public enum Type {INFO, WARN, ERROR}

    private final String event = Type.ERROR.toString();
    private String sessionId;
    private String exception;
    private String message;
}