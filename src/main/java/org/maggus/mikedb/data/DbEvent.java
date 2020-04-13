package org.maggus.mikedb.data;

import lombok.Data;

@Data
public class DbEvent {
    public enum Type {INSERTED, UPDATED, DELETED, DROPPED}

    private String event;
    private String sessionId;
    private String dbName;
    private String key;
    private Object value;
}
