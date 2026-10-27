package com.college.event;

import com.college.event.database.DatabaseManager;
import com.college.event.model.BaseEvent;
import com.college.event.model.Event;
import com.college.event.model.Student;
import com.college.event.patterns.mediator.CollegeEventMediator;
import com.college.event.patterns.mediator.MediatedRegistrationResult;
import com.college.event.patterns.memento.EventCaretaker;
import com.college.event.patterns.memento.EventMemento;
import com.college.event.patterns.mvc.controller.EventController;
import com.college.event.patterns.mvc.model.EventModel;
import com.college.event.patterns.strategy.*;
import com.college.event.patterns.templatemethod.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatternExtensionTest {

    private DatabaseManager db;

    @BeforeEach
    void setUp() {
        db = new DatabaseManager();
    }

    @Test
    void testPrototypePatternClonesEventAsDraft() {
        BaseEvent original = new BaseEvent(1, "RoboWars 2026", "TECHNICAL",
                "Combat robot competition", "2026-11-01", "Arena", 50, "OPEN");

        BaseEvent draftClone = original.cloneDraft();

        assertNotNull(draftClone);
        assertEquals("[Draft] RoboWars 2026", draftClone.getName());
        assertEquals("DRAFT", draftClone.getStatus());
        assertEquals(0, draftClone.getId());
        assertEquals("Arena", draftClone.getVenue());
        assertEquals(50, draftClone.getCapacity());

        // Test clone with customizations
        BaseEvent customClone = original.cloneWithCustomizations("RoboWars Spring Edition", "2027-02-15", "Outdoor Field");
        assertEquals("RoboWars Spring Edition", customClone.getName());
        assertEquals("2027-02-15", customClone.getDate());
        assertEquals("Outdoor Field", customClone.getVenue());
        assertEquals("DRAFT", customClone.getStatus());
    }

    @Test
    void testStrategyPatternFeeCalculations() {
        BaseEvent techEvent = new BaseEvent("CodeFest", "TECHNICAL", "Coding", "2026-10-10", "Lab", 100);
        Student student = new Student(1, "Alice", "alice", "pass", "alice@edu", "CSE");

        FeeStrategyContext context = new FeeStrategyContext(new FreeFeeStrategy());
        FeeBreakdown freeBreakdown = context.executeStrategy(techEvent, student);
        assertEquals(0.0, freeBreakdown.getFinalFee());
        assertEquals(100.0, freeBreakdown.getDiscountPercentage());

        context.setStrategy(new StandardFeeStrategy());
        FeeBreakdown stdBreakdown = context.executeStrategy(techEvent, student);
        assertEquals(150.0, stdBreakdown.getFinalFee());
        assertEquals(0.0, stdBreakdown.getDiscountPercentage());

        context.setStrategy(new EarlyBirdFeeStrategy(30.0));
        FeeBreakdown earlyBreakdown = context.executeStrategy(techEvent, student);
        assertEquals(105.0, earlyBreakdown.getFinalFee()); // 150 - 45 = 105
        assertEquals(30.0, earlyBreakdown.getDiscountPercentage());
    }

    @Test
    void testTemplateMethodPatternWorkflows() {
        BaseEvent techEvent = new BaseEvent("AI Conclave", "TECHNICAL", "Keynotes", "2026-11-20", "Auditorium", 200);
        BaseEvent cultEvent = new BaseEvent("Dance Fiesta", "CULTURAL", "Dance", "2026-11-22", "Main Stage", 300);

        EventWorkflowTemplate techWorkflow = new TechnicalEventWorkflow();
        WorkflowExecutionReport techReport = techWorkflow.executeWorkflow(techEvent);

        assertTrue(techReport.isCompleted());
        assertEquals("Technical Event Deployment Pipeline", techReport.getWorkflowName());
        assertEquals(6, techReport.getSteps().size());
        assertTrue(techReport.getSteps().get(2).getDetail().contains("GitHub Classroom"));

        EventWorkflowTemplate cultWorkflow = new CulturalEventWorkflow();
        WorkflowExecutionReport cultReport = cultWorkflow.executeWorkflow(cultEvent);

        assertTrue(cultReport.isCompleted());
        assertEquals("Cultural Event Production Pipeline", cultReport.getWorkflowName());
        assertEquals(6, cultReport.getSteps().size());
        assertTrue(cultReport.getSteps().get(2).getDetail().contains("green rooms"));
    }

    @Test
    void testMementoPatternSaveAndRestore() {
        EventCaretaker caretaker = EventCaretaker.getInstance();
        BaseEvent event = new BaseEvent(999, "Hackathon v1", "TECHNICAL", "Initial", "2026-10-01", "Lab A", 40, "OPEN");

        // Save v1
        EventMemento v1 = caretaker.saveCheckpoint(event, "Initial Creation", "Coordinator 1");
        assertEquals(1, v1.getVersion());

        // Modify and save v2
        BaseEvent modified = new BaseEvent(999, "Hackathon v2 Final", "TECHNICAL", "Expanded venue", "2026-10-01", "Lab A & B", 80, "OPEN");
        EventMemento v2 = caretaker.saveCheckpoint(modified, "Expanded capacity to 80", "Coordinator 2");
        assertEquals(2, v2.getVersion());

        List<EventMemento> history = caretaker.getHistory(999);
        assertEquals(2, history.size());

        // Restore to v1
        EventMemento restored = caretaker.getMementoByVersion(999, 1);
        assertEquals("Hackathon v1", restored.getName());
        assertEquals(40, restored.getCapacity());
        assertEquals("Lab A", restored.getVenue());
    }

    @Test
    void testMediatorPatternCoordinatesRegistration() {
        CollegeEventMediator mediator = new CollegeEventMediator(db);
        // Create isolated test student and event
        String user = "testuser_" + System.currentTimeMillis();
        Student testStudent = db.registerStudent("Test Student", user, "pass", user + "@college.edu", "CSE");
        assertNotNull(testStudent);

        BaseEvent testEvent = new BaseEvent("Mediator Test Conclave", "TECHNICAL", "Test event", "2026-12-01", "Lab", 100);
        int eventId = db.createEvent(testEvent);
        assertTrue(eventId > 0);

        MediatedRegistrationResult result = mediator.mediateRegistration(testStudent.getId(), eventId, "EARLY_BIRD", "EMAIL");

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Mediated registration should succeed");
        assertEquals("APPROVED", result.getStatus());
        assertNotNull(result.getCoordinationAuditLog());
        assertTrue(result.getCoordinationAuditLog().size() >= 8, "Expected at least 8 coordination steps");
        assertNotNull(result.getFeeBreakdown());
        assertEquals(105.0, result.getFeeBreakdown().getFinalFee());
    }

    @Test
    void testMvcPatternControllerModelView() {
        EventController controller = new EventController(db);
        List<Event> events = db.getEvents();
        int eventId = events.get(0).getId();

        EventModel model = controller.loadEventModel(eventId, "STANDARD");
        assertNotNull(model);
        assertNotNull(model.getEvent());
        assertNotNull(model.getCurrentFeeBreakdown());

        String renderedCard = controller.getView().renderConsoleCard(model);
        assertNotNull(renderedCard);
        assertTrue(renderedCard.contains(model.getEvent().getName()));
        assertTrue(renderedCard.contains("Category"));
    }
}
