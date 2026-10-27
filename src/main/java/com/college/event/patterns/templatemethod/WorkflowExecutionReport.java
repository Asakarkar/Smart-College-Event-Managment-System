package com.college.event.patterns.templatemethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkflowExecutionReport {
    private final String workflowName;
    private final String eventName;
    private final String category;
    private final List<WorkflowStepLog> steps;
    private boolean completed;
    private String summary;

    public WorkflowExecutionReport(String workflowName, String eventName, String category) {
        this.workflowName = workflowName;
        this.eventName = eventName;
        this.category = category;
        this.steps = new ArrayList<>();
        this.completed = false;
        this.summary = "";
    }

    public void addStep(String stepName, String detail, boolean success) {
        steps.add(new WorkflowStepLog(stepName, detail, success));
    }

    public void finish(boolean completed, String summary) {
        this.completed = completed;
        this.summary = summary;
    }

    public String getWorkflowName() { return workflowName; }
    public String getEventName() { return eventName; }
    public String getCategory() { return category; }
    public List<WorkflowStepLog> getSteps() { return Collections.unmodifiableList(steps); }
    public boolean isCompleted() { return completed; }
    public String getSummary() { return summary; }
}
