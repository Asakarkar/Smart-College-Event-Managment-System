package com.college.event.patterns.chain;

public abstract class ApprovalHandler {
    protected ApprovalHandler next;

    public void setNext(ApprovalHandler next) {
        this.next = next;
    }

    public abstract boolean approve(ApprovalRequest request);
}