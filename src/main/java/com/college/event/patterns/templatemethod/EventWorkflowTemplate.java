package com.college.event.patterns.templatemethod;

import com.college.event.model.Event;

/**
 * Template Method Pattern: Defines the workflow skeleton in a method,
 * deferring certain variable steps to subclasses.
 * Provides specialized technical and cultural workflow templates.
 */
public abstract class EventWorkflowTemplate {

    /**
     * The Template Method defining the immutable workflow skeleton.
     */
    public final WorkflowExecutionReport executeWorkflow(Event event) {
        WorkflowExecutionReport report = new WorkflowExecutionReport(
                getWorkflowName(),
                event != null ? event.getName() : "Unknown Event",
                getCategory()
        );

        try {
            // Step 1: Common Invariant Step
            validateEventDetails(event, report);

            // Step 2: Common Invariant Step
            reserveVenueAndSchedule(event, report);

            // Step 3: Abstract Variable Step (Subclass defines)
            setupDomainPrerequisites(event, report);

            // Step 4: Abstract Variable Step (Subclass defines)
            provisionEquipmentAndResources(event, report);

            // Step 5: Abstract Variable Step (Subclass defines)
            configureJudgingAndEvaluation(event, report);

            // Step 6: Hook / Final Invariant Step
            publishAndNotifyStakeholders(event, report);

            report.finish(true, getWorkflowName() + " completed successfully for: " + event.getName());
        } catch (Exception e) {
            report.addStep("Error Exception", "Workflow interrupted: " + e.getMessage(), false);
            report.finish(false, "Workflow failed: " + e.getMessage());
        }

        return report;
    }

    public abstract String getWorkflowName();
    public abstract String getCategory();

    // Variable step 1
    protected abstract void setupDomainPrerequisites(Event event, WorkflowExecutionReport report);

    // Variable step 2
    protected abstract void provisionEquipmentAndResources(Event event, WorkflowExecutionReport report);

    // Variable step 3
    protected abstract void configureJudgingAndEvaluation(Event event, WorkflowExecutionReport report);

    // Invariant step 1
    protected void validateEventDetails(Event event, WorkflowExecutionReport report) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getName() == null || event.getName().isBlank()) {
            throw new IllegalArgumentException("Event name is required");
        }
        if (event.getCapacity() <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        report.addStep(
                "Step 1: Event Validation",
                "Validated event '" + event.getName() + "' with capacity " + event.getCapacity() + " and date " + event.getDate(),
                true
        );
    }

    // Invariant step 2
    protected void reserveVenueAndSchedule(Event event, WorkflowExecutionReport report) {
        report.addStep(
                "Step 2: Venue & Schedule Lock",
                "Reserved venue '" + event.getVenue() + "' on " + event.getDate() + " with campus facilities team",
                true
        );
    }

    // Hook / Invariant final step
    protected void publishAndNotifyStakeholders(Event event, WorkflowExecutionReport report) {
        report.addStep(
                "Step 6: Official Publication",
                "Broadcasted event listing to College Portal and notified faculty coordinators",
                true
        );
    }
}
