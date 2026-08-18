package com.college.event.patterns.bridge;

public class EmailNotification implements NotificationChannel {
    public void send(String recipient, String message) {
        System.out.println("[EMAIL] To: " + recipient + " | " + message);
    }
    public String getName() { return "EMAIL"; }
}