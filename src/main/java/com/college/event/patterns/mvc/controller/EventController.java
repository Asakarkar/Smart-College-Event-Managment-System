package com.college.event.patterns.mvc.controller;

import com.college.event.database.DatabaseManager;
import com.college.event.model.BaseEvent;
import com.college.event.model.Event;
import com.college.event.patterns.mediator.CollegeEventMediator;
import com.college.event.patterns.mediator.MediatedRegistrationResult;
import com.college.event.patterns.memento.EventCaretaker;
import com.college.event.patterns.memento.EventMemento;
import com.college.event.patterns.memento.EventOriginator;
import com.college.event.patterns.mvc.model.EventModel;
import com.college.event.patterns.mvc.view.EventView;
import com.college.event.patterns.strategy.FeeBreakdown;
import com.college.event.patterns.strategy.FeeStrategyContext;
import com.college.event.patterns.templatemethod.WorkflowExecutionReport;

import java.util.List;

/**
 * Controller in MVC:
 * Coordinates user actions, mutates models, interfaces with Database and Patterns
 * (Prototype, Mediator, Template Method, Strategy, Memento), and passes Model to View.
 */
public class EventController {
    private final DatabaseManager db;
    private final EventView view;
    private final EventCaretaker caretaker;
    private final CollegeEventMediator mediator;

    public EventController(DatabaseManager db) {
        this.db = db;
        this.view = new EventView();
        this.caretaker = EventCaretaker.getInstance();
        this.mediator = new CollegeEventMediator(db);
    }

    public EventView getView() {
        return view;
    }

    public EventModel loadEventModel(int eventId, String feeTier) {
        Event event = db.getEventById(eventId);
        if (event == null) return null;

        int registered = db.getRegistrationCount(eventId);
        EventModel model = new EventModel(event, registered);

        // Apply Strategy Pattern
        if (feeTier != null && !feeTier.isBlank()) {
            FeeStrategyContext ctx = new FeeStrategyContext(feeTier);
            FeeBreakdown breakdown = ctx.executeStrategy(event, null);
            model.setCurrentFeeBreakdown(breakdown);
        }

        // Apply Memento Pattern history
        List<EventMemento> history = caretaker.getHistory(eventId);
        model.setVersionHistory(history);

        return model;
    }

    /**
     * Action: Clone an existing event as a draft (Prototype Pattern)
     */
    public EventModel cloneEventAsDraft(int sourceEventId, String customName) {
        Event cloned = mediator.mediateEventClone(sourceEventId, customName);
        if (cloned == null) return null;
        return loadEventModel(cloned.getId(), "STANDARD");
    }

    /**
     * Action: Edit an event and record a Memento checkpoint (Memento Pattern)
     */
    public EventModel updateEventWithCheckpoint(int eventId, String name, String category,
                                                String description, String date, String venue,
                                                int capacity, String status, String changeReason,
                                                String author) {
        Event current = db.getEventById(eventId);
        if (current == null) return null;

        // Save pre-change or post-change checkpoint in Caretaker
        BaseEvent updated = new BaseEvent(eventId, name, category, description, date, venue, capacity, status);
        db.updateEvent(eventId, updated);

        caretaker.saveCheckpoint(updated, changeReason != null ? changeReason : "Updated event properties", author);

        return loadEventModel(eventId, "STANDARD");
    }

    /**
     * Action: Restore an event state to a previous Memento snapshot (Memento Pattern)
     */
    public EventModel restoreFromVersion(int eventId, int targetVersion) {
        EventMemento memento = caretaker.getMementoByVersion(eventId, targetVersion);
        if (memento == null) return null;

        EventOriginator originator = new EventOriginator();
        originator.restoreFromMemento(memento);
        Event restored = originator.toEvent();

        db.updateEvent(eventId, restored);

        // Record that a rollback occurred
        caretaker.saveCheckpoint(restored, "Restored state to v" + targetVersion + ": " + memento.getChangeSummary(), "System Rollback");

        return loadEventModel(eventId, "STANDARD");
    }

    /**
     * Action: Execute category workflow pipeline (Template Method Pattern)
     */
    public WorkflowExecutionReport runWorkflow(int eventId) {
        return mediator.mediateEventWorkflow(eventId);
    }

    /**
     * Action: Mediate student registration (Mediator Pattern)
     */
    public MediatedRegistrationResult registerStudent(int studentId, int eventId, String feeTier, String channel) {
        return mediator.mediateRegistration(studentId, eventId, feeTier, channel);
    }
}
