package com.college.event.patterns.chain;

public class FacultyCoordinator extends ApprovalHandler {
    public boolean approve(ApprovalRequest request) {
        System.out.println("Faculty Coordinator reviewed registration.");
        if ("SPORTS".equalsIgnoreCase(request.getCategory())) {
            System.out.println("Faculty Coordinator approved the registration.");
            return true;
        }
        System.out.println("Forwarding to HOD.");
        return next != null && next.approve(request);
    }
}