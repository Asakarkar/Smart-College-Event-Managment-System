package com.college.event.patterns.templatemethod;

import com.college.event.model.Event;

/**
 * Concrete Template Method implementation for Technical Events.
 * Implements domain prerequisites, workstation/cloud provisioning, and coding evaluation rubrics.
 */
public class TechnicalEventWorkflow extends EventWorkflowTemplate {

    @Override
    public String getWorkflowName() {
        return "Technical Event Deployment Pipeline";
    }

    @Override
    public String getCategory() {
        return "TECHNICAL";
    }

    @Override
    protected void setupDomainPrerequisites(Event event, WorkflowExecutionReport report) {
        report.addStep(
                "Step 3: Tech Stack & Sandbox Setup",
                "Created GitHub Classroom organization, initialized starter repos, and configured Dockerized judge runners.",
                true
        );
    }

    @Override
    protected void provisionEquipmentAndResources(Event event, WorkflowExecutionReport report) {
        report.addStep(
                "Step 4: Infrastructure Provisioning",
                "Allocated high-speed 1Gbps dual-redundant Wi-Fi SSID, 50 LAN ports, and cloud GPU cluster credits.",
                true
        );
    }

    @Override
    protected void configureJudgingAndEvaluation(Event event, WorkflowExecutionReport report) {
        report.addStep(
                "Step 5: Automated Testing & Rubric",
                "Configured CI/CD test harness (Code Quality 30%, Test Cases 40%, Innovation & Architecture 30%).",
                true
        );
    }
}
