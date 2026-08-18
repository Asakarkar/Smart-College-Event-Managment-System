package com.college.event.patterns.bridge;

public class MobileAppNotification implements NotificationChannel {
    public void send(String recipient, String message) {
        System.out.println("[MOBILE APP] To: " + recipient + " | " + message);
    }
    public String getName() { return "MOBILE_APP"; }
}