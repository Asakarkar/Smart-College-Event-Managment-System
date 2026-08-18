package com.college.event.patterns.chain;

public class ApprovalRequest {
    private final String eventName;
    private final String studentName;
    private final String category;
    private final boolean majorEvent;

    public ApprovalRequest(String eventName, String studentName,
                           String category, boolean majorEvent) {
        this.eventName = eventName;
        this.studentName = studentName;
        this.category = category;
        this.majorEvent = majorEvent;
    }

    public String getEventName() { return eventName; }
    public String getStudentName() { return studentName; }
    public String getCategory() { return category; }
    public boolean isMajorEvent() { return majorEvent; }
}