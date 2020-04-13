package org.maggus.mikedb.data;

import lombok.Data;

@Data
public class SessionEvent {
    public enum Type {NEW, OPENED, CLOSED}

    private String event;
    private String sessionId;
}
