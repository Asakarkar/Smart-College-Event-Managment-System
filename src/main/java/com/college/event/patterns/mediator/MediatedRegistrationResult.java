package com.college.event.patterns.mediator;

import com.college.event.model.Event;
import com.college.event.model.Registration;
import com.college.event.model.Student;
import com.college.event.patterns.strategy.FeeBreakdown;

import java.util.List;

public class MediatedRegistrationResult {
    private final boolean success;
    private final String status; // APPROVED, REJECTED, FAILED
    private final String message;
    private final Registration registration;
    private final Event event;
    private final Student student;
    private final FeeBreakdown feeBreakdown;
    private final List<String> coordinationAuditLog;

    public MediatedRegistrationResult(boolean success, String status, String message,
                                     Registration registration, Event event,
                                     Student student, FeeBreakdown feeBreakdown,
                                     List<String> coordinationAuditLog) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.registration = registration;
        this.event = event;
        this.student = student;
        this.feeBreakdown = feeBreakdown;
        this.coordinationAuditLog = coordinationAuditLog;
    }

    public boolean isSuccess() { return success; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public Registration getRegistration() { return registration; }
    public Event getEvent() { return event; }
    public Student getStudent() { return student; }
    public FeeBreakdown getFeeBreakdown() { return feeBreakdown; }
    public List<String> getCoordinationAuditLog() { return coordinationAuditLog; }
}
