package com.college.event.model;

public class Registration {
    private final int id;
    private final int studentId;
    private final int eventId;
    private String status;

    public Registration(int id, int studentId, int eventId, String status) {
        this.id = id;
        this.studentId = studentId;
        this.eventId = eventId;
        this.status = status;
    }

    public int getId() { return id; }
    public int getStudentId() { return studentId; }
    public int getEventId() { return eventId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}