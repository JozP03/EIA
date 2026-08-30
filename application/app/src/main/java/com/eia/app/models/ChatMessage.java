package com.eia.app.models;

public class ChatMessage {
    public enum Type { USER, AI }

    private String text;
    private Type type;

    public ChatMessage(String text, Type type) {
        this.text = text;
        this.type = type;
    }

    public String getText() { return text; }
    public Type getType() { return type; }
}
