package com.college.event.patterns.mediator;

import com.college.event.model.Event;
import com.college.event.patterns.templatemethod.WorkflowExecutionReport;

/**
 * Mediator Pattern: Centralizes communication among collaborators.
 * Coordinates workflow interactions between Student, Event, Fee Strategy,
 * Approval Chain, Memento, and Notification subsystems.
 */
public interface WorkflowMediator {

    MediatedRegistrationResult mediateRegistration(
            int studentId,
            int eventId,
            String feeTier,
            String notificationChannel
    );

    WorkflowExecutionReport mediateEventWorkflow(int eventId);

    Event mediateEventClone(int eventId, String customName);
}
