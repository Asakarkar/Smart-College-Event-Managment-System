package com.college.event.patterns.bridge;

public class SmsNotification implements NotificationChannel {
    public void send(String recipient, String message) {
        System.out.println("[SMS] To: " + recipient + " | " + message);
    }
    public String getName() { return "SMS"; }
}