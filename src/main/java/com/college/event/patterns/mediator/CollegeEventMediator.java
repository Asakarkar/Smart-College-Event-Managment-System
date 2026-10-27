package com.college.event.patterns.mediator;

import com.college.event.database.DatabaseManager;
import com.college.event.model.BaseEvent;
import com.college.event.model.Event;
import com.college.event.model.Registration;
import com.college.event.model.Student;
import com.college.event.patterns.bridge.*;
import com.college.event.patterns.chain.*;
import com.college.event.patterns.memento.EventCaretaker;
import com.college.event.patterns.observer.*;
import com.college.event.patterns.strategy.FeeBreakdown;
import com.college.event.patterns.strategy.FeeStrategyContext;
import com.college.event.patterns.templatemethod.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete Mediator in Mediator Pattern:
 * Centralizes and coordinates all cross-cutting workflow interactions
 * so individual components do not need to be tightly coupled.
 */
public class CollegeEventMediator implements WorkflowMediator {
    private final DatabaseManager db;
    private final EventCaretaker caretaker;

    public CollegeEventMediator(DatabaseManager db) {
        this.db = db;
        this.caretaker = EventCaretaker.getInstance();
    }

    @Override
    public MediatedRegistrationResult mediateRegistration(
            int studentId,
            int eventId,
            String feeTier,
            String notificationChannel) {

        List<String> audit = new ArrayList<>();
        audit.add("1. Mediator received registration request: Student ID=" + studentId + ", Event ID=" + eventId);

        // Fetch Student
        Student student = findStudentById(studentId);
        if (student == null) {
            audit.add("✗ Collaborator Error: Student not found with ID=" + studentId);
            return new MediatedRegistrationResult(false, "FAILED", "Student not found", null, null, null, null, audit);
        }
        audit.add("2. Mediator validated Student: " + student.getName() + " (" + student.getDepartment() + ")");

        // Fetch Event
        Event event = db.getEventById(eventId);
        if (event == null) {
            audit.add("✗ Collaborator Error: Event not found with ID=" + eventId);
            return new MediatedRegistrationResult(false, "FAILED", "Event not found", null, null, student, null, audit);
        }
        audit.add("3. Mediator verified Event: '" + event.getName() + "' [" + event.getCategory() + "]");

        // Check Capacity
        int registeredCount = db.getRegistrationCount(eventId);
        if (registeredCount >= event.getCapacity()) {
            audit.add("✗ Collaborator Error: Event is full (" + registeredCount + "/" + event.getCapacity() + ")");
            return new MediatedRegistrationResult(false, "CAPACITY_FULL", "Event is full", null, event, student, null, audit);
        }
        audit.add("4. Capacity check passed: " + registeredCount + "/" + event.getCapacity() + " occupied");

        // Check Existing Registration
        if (db.isStudentRegistered(studentId, eventId)) {
            audit.add("✗ Collaborator Error: Student already registered for this event");
            return new MediatedRegistrationResult(false, "DUPLICATE", "Already registered", null, event, student, null, audit);
        }

        // Collaborator: Strategy Pattern (Calculate Fee)
        FeeStrategyContext feeContext = new FeeStrategyContext(feeTier);
        FeeBreakdown feeBreakdown = feeContext.executeStrategy(event, student);
        audit.add("5. Mediator invoked Strategy Pattern (" + feeContext.getStrategy().getTierCode() + "): Final Fee = $" +
                String.format("%.2f", feeBreakdown.getFinalFee()) + " (" + feeBreakdown.getDescription() + ")");

        // Create Pending Registration in DB
        Registration registration = db.createRegistration(studentId, eventId);
        if (registration == null) {
            audit.add("✗ Collaborator Error: DB registration creation failed");
            return new MediatedRegistrationResult(false, "FAILED", "Could not create registration", null, event, student, feeBreakdown, audit);
        }
        audit.add("6. Created database registration entry #" + registration.getId() + " [PENDING]");

        // Collaborator: Chain of Responsibility (Approval Chain)
        ApprovalHandler coordinator = new EventCoordinator();
        ApprovalHandler faculty = new FacultyCoordinator();
        ApprovalHandler hod = new HOD();
        coordinator.setNext(faculty);
        faculty.setNext(hod);

        boolean major = "TECHNICAL".equalsIgnoreCase(event.getCategory())
                || "WORKSHOP".equalsIgnoreCase(event.getCategory());

        audit.add("7. Mediator invoked Chain of Responsibility: Coordinator -> Faculty -> HOD (Major=" + major + ")");
        boolean approved = coordinator.approve(new ApprovalRequest(
                event.getName(), student.getName(), event.getCategory(), major));

        String finalStatus = approved ? "APPROVED" : "REJECTED";
        db.updateRegistrationStatus(registration.getId(), finalStatus);
        audit.add("8. Chain decision resolved: " + finalStatus);

        // Collaborator: Observer Pattern
        EventCoordinatorNotifier subject = new EventCoordinatorNotifier();
        subject.registerObserver(new StudentObserver());
        subject.registerObserver(new FacultyObserver());
        subject.registerObserver(new CoordinatorObserver());
        subject.notifyObservers(registration.getId(), event.getName(), finalStatus);
        audit.add("9. Mediator dispatched Observer notifications to Student, Faculty & Coordinator observers");

        // Collaborator: Bridge Pattern (Notification Channel)
        NotificationChannel channel = resolveChannel(notificationChannel);
        EventNotifier notifier = new EventNotifier(channel);
        String notifMsg = "Your registration #" + registration.getId() + " for " + event.getName()
                + " has been " + finalStatus + ". Fee: $" + String.format("%.2f", feeBreakdown.getFinalFee());
        notifier.notify(student.getEmail(), notifMsg);
        db.addNotification(registration.getId(), student.getEmail(), notifier.getChannelName(), notifMsg);
        audit.add("10. Mediator invoked Bridge Pattern: Dispatched " + notifier.getChannelName() + " to " + student.getEmail());

        audit.add("✓ Mediator workflow execution finished with status: " + finalStatus);
        return new MediatedRegistrationResult(true, finalStatus, "Registration successfully processed through Mediator",
                registration, event, student, feeBreakdown, audit);
    }

    @Override
    public WorkflowExecutionReport mediateEventWorkflow(int eventId) {
        Event event = db.getEventById(eventId);
        if (event == null) {
            WorkflowExecutionReport failedReport = new WorkflowExecutionReport("Workflow Execution", "Event #" + eventId, "UNKNOWN");
            failedReport.finish(false, "Event not found");
            return failedReport;
        }

        EventWorkflowTemplate workflow = switch (event.getCategory().toUpperCase()) {
            case "TECHNICAL" -> new TechnicalEventWorkflow();
            case "CULTURAL" -> new CulturalEventWorkflow();
            case "SPORTS" -> new SportsEventWorkflow();
            default -> new TechnicalEventWorkflow();
        };

        WorkflowExecutionReport report = workflow.executeWorkflow(event);

        if (report.isCompleted()) {
            db.updateEventStatus(eventId, "OPEN");
            caretaker.saveCheckpoint(event, "Template Method executed: " + workflow.getWorkflowName(), "Mediator Workflow Runner");
        }

        return report;
    }

    @Override
    public Event mediateEventClone(int eventId, String customName) {
        Event source = db.getEventById(eventId);
        if (source == null) return null;

        BaseEvent base = (source instanceof BaseEvent be) ? be : new BaseEvent(
                source.getId(), source.getName(), source.getCategory(), source.getDescription(),
                source.getDate(), source.getVenue(), source.getCapacity(), source.getStatus());

        BaseEvent draftClone = (customName != null && !customName.isBlank())
                ? base.cloneWithCustomizations(customName, base.getDate(), base.getVenue())
                : base.cloneDraft();

        int newId = db.createEvent(draftClone);
        Event createdClone = db.getEventById(newId);

        caretaker.saveCheckpoint(createdClone, "Cloned from Event #" + eventId + " (" + source.getName() + ") via Prototype pattern", "Mediator Prototype Coordinator");

        return createdClone;
    }

    private Student findStudentById(int id) {
        try (var c = java.sql.DriverManager.getConnection("jdbc:sqlite:college_events.db");
             var p = c.prepareStatement("SELECT * FROM students WHERE id=?")) {
            p.setInt(1, id);
            try (var r = p.executeQuery()) {
                if (r.next()) {
                    return new Student(r.getInt("id"), r.getString("name"),
                            r.getString("username"), r.getString("password"),
                            r.getString("email"), r.getString("department"));
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private NotificationChannel resolveChannel(String channelStr) {
        if (channelStr == null) return new EmailNotification();
        return switch (channelStr.trim().toUpperCase()) {
            case "SMS" -> new SmsNotification();
            case "MOBILE_APP", "APP" -> new MobileAppNotification();
            default -> new EmailNotification();
        };
    }
}
