package com.college.event.patterns.bridge;

public class EventNotifier {
    private final NotificationChannel channel;

    public EventNotifier(NotificationChannel channel) {
        this.channel = channel;
    }

    public void notify(String recipient, String message) {
        channel.send(recipient, message);
    }

    public String getChannelName() {
        return channel.getName();
    }
}