package com.college.event.patterns.chain;

public class HOD extends ApprovalHandler {
    public boolean approve(ApprovalRequest request) {
        System.out.println("HOD reviewed registration for " + request.getEventName() + ".");
        System.out.println("HOD approved the registration.");
        return true;
    }
}