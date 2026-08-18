package com.college.event.patterns.chain;

public class EventCoordinator extends ApprovalHandler {
    public boolean approve(ApprovalRequest request) {
        System.out.println("Event Coordinator reviewed registration.");
        if (!request.isMajorEvent()) {
            System.out.println("Coordinator approved the registration.");
            return true;
        }
        System.out.println("Major event: forwarding to Faculty Coordinator.");
        return next != null && next.approve(request);
    }
}