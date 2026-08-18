package com.college.event.patterns.bridge;

public interface NotificationChannel {
    void send(String recipient, String message);
    String getName();
}