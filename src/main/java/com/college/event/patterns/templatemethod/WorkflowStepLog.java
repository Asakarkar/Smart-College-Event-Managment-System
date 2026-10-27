package com.college.event.patterns.templatemethod;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WorkflowStepLog {
    private final String stepName;
    private final String detail;
    private final boolean success;
    private final String timestamp;

    public WorkflowStepLog(String stepName, String detail, boolean success) {
        this.stepName = stepName;
        this.detail = detail;
        this.success = success;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }

    public String getStepName() { return stepName; }
    public String getDetail() { return detail; }
    public boolean isSuccess() { return success; }
    public String getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + (success ? "✓ " : "✗ ") + stepName + ": " + detail;
    }
}
