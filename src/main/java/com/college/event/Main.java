package com.college.event;

import com.college.event.database.DatabaseManager;
import com.college.event.model.Event;
import com.college.event.model.Registration;
import com.college.event.model.Student;
import com.college.event.patterns.abstractfactory.EventFamilyFactory;
import com.college.event.patterns.bridge.*;
import com.college.event.patterns.chain.*;
import com.college.event.patterns.factory.*;
import com.college.event.patterns.observer.*;
import com.college.event.patterns.proxy.EventSystemProxy;
import com.college.event.patterns.mediator.CollegeEventMediator;
import com.college.event.patterns.mediator.MediatedRegistrationResult;
import com.college.event.patterns.memento.EventCaretaker;
import com.college.event.patterns.memento.EventMemento;
import com.college.event.patterns.mvc.controller.EventController;
import com.college.event.patterns.mvc.model.EventModel;
import com.college.event.patterns.strategy.*;
import com.college.event.patterns.templatemethod.*;
import com.college.event.server.WebServer;
import com.college.event.patterns.singleton.EventManagementSystem;

import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);
    private static final EventManagementSystem SYSTEM = EventManagementSystem.getInstance();
    private static final DatabaseManager DB = SYSTEM.getDatabase();

    public static void main(String[] args) {
        printHeader();

        while (true) {
            System.out.println("\n================ MAIN MENU ================");
            System.out.println("1. Student Registration");
            System.out.println("2. Student Login");
            System.out.println("3. Browse Events");
            System.out.println("4. Coordinator Panel");
            System.out.println("5. Design Pattern Lab (Prototype, Mediator, Template Method, Strategy, Memento, MVC)");
            System.out.println("6. Launch Fullstack Web Server (http://localhost:8085)");
            System.out.println("7. Exit");

            int choice = readInt("Enter choice: ");

            switch (choice) {
                case 1 -> studentRegistration();
                case 2 -> studentLogin();
                case 3 -> showEvents();
                case 4 -> coordinatorPanel();
                case 5 -> patternLabMenu();
                case 6 -> launchWebServer();
                case 7 -> {
                    System.out.println("\nThank you for using the College Event Management System.");
                    return;
                }
                default -> System.out.println("Invalid choice. Please select 1-7.");
            }
        }
    }

    private static void printHeader() {
        System.out.println("======================================================");
        System.out.println("        SMART COLLEGE EVENT MANAGEMENT SYSTEM");
        System.out.println("======================================================");
        System.out.println(" Register for events, track applications and receive");
        System.out.println(" notifications through one centralized system.");
    }

    private static void studentRegistration() {
        System.out.println("\n--------------- NEW STUDENT ACCOUNT ---------------");

        String name = readNonEmpty("Full name: ");
        String username = readNonEmpty("Choose username: ");
        String password = readNonEmpty("Create password: ");
        String confirm = readNonEmpty("Confirm password: ");

        if (!password.equals(confirm)) {
            System.out.println("Passwords do not match. Registration cancelled.");
            return;
        }

        String email = readNonEmpty("Email: ");
        String department = readNonEmpty("Department: ");

        Student student = DB.registerStudent(name, username, password, email, department);

        if (student == null) {
            System.out.println("\nUsername already exists. Please choose another username.");
            return;
        }

        System.out.println("\nRegistration successful!");
        System.out.println("Student ID : " + student.getId());
        System.out.println("Username   : " + student.getUsername());
        System.out.println("You can now use Student Login.");
    }

    private static void studentLogin() {
        EventSystemProxy proxy = new EventSystemProxy(DB);

        System.out.println("\n---------------- STUDENT LOGIN ----------------");
        String username = readNonEmpty("Username: ");
        String password = readNonEmpty("Password: ");

        Student student = proxy.login(username, password);

        if (student == null) {
            System.out.println("Invalid username or password. Access denied.");
            return;
        }

        System.out.println("\nLogin successful!");
        System.out.println("Welcome, " + student.getName() + " (" + student.getDepartment() + ")");
        studentMenu(proxy, student);
    }

    private static void studentMenu(EventSystemProxy proxy, Student student) {
        while (true) {
            System.out.println("\n--------------- STUDENT DASHBOARD ---------------");
            System.out.println("1. Browse Events");
            System.out.println("2. Register for Event");
            System.out.println("3. My Registrations");
            System.out.println("4. Cancel Registration");
            System.out.println("5. Logout");

            int choice = readInt("Enter choice: ");

            switch (choice) {
                case 1 -> showEvents();
                case 2 -> registerForEvent(proxy, student);
                case 3 -> showMyRegistrations(student);
                case 4 -> deleteRegistration(student);
                case 5 -> {
                    System.out.println("Logged out successfully.");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void showEvents() {
        List<Event> events = DB.getEvents();
        List<Integer> ids = DB.getEventIds();

        System.out.println("\n================ AVAILABLE EVENTS ================");
        if (events.isEmpty()) {
            System.out.println("No events are currently available.");
            return;
        }

        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            int registered = DB.getRegistrationCount(ids.get(i));
            int seatsLeft = Math.max(0, e.getCapacity() - registered);

            System.out.printf(
                    "%n[%d] %s%n   Category : %s%n   Date     : %s%n   Venue    : %s%n   Seats    : %d/%d available%n",
                    i + 1, e.getName(), e.getCategory(), e.getDate(),
                    e.getVenue(), seatsLeft, e.getCapacity()
            );
            System.out.println("   Details  : " + e.getDescription());
        }
    }

    private static void registerForEvent(EventSystemProxy proxy, Student student) {
        List<Event> events = DB.getEvents();
        List<Integer> ids = DB.getEventIds();

        if (events.isEmpty()) {
            System.out.println("No events are available for registration.");
            return;
        }

        showEvents();
        int selection = readInt("\nSelect event number: ");

        if (selection < 1 || selection > events.size()) {
            System.out.println("Invalid event selection.");
            return;
        }

        int eventId = ids.get(selection - 1);
        Event event = events.get(selection - 1);

        if (DB.getRegistrationCount(eventId) >= event.getCapacity()) {
            System.out.println("Sorry, this event is full.");
            return;
        }

        if (DB.isStudentRegistered(student.getId(), eventId)) {
            System.out.println("You are already registered for this event.");
            return;
        }

        Registration registration;
        try {
            registration = proxy.register(eventId);
        } catch (Exception e) {
            System.out.println("Unable to register: " + e.getMessage());
            return;
        }

        if (registration == null) {
            System.out.println("Registration could not be created.");
            return;
        }

        System.out.println("\nRegistration submitted successfully.");
        System.out.println("Registration ID: #" + registration.getId());
        System.out.println("Event: " + event.getName());

        // Chain of Responsibility
        ApprovalHandler coordinator = new EventCoordinator();
        ApprovalHandler faculty = new FacultyCoordinator();
        ApprovalHandler hod = new HOD();

        coordinator.setNext(faculty);
        faculty.setNext(hod);

        boolean major = "TECHNICAL".equals(event.getCategory())
                || "WORKSHOP".equals(event.getCategory());

        boolean approved = coordinator.approve(new ApprovalRequest(
                event.getName(), student.getName(), event.getCategory(), major));

        String status = approved ? "APPROVED" : "REJECTED";
        DB.updateRegistrationStatus(registration.getId(), status);

        // Observer
        EventCoordinatorNotifier subject = new EventCoordinatorNotifier();
        subject.registerObserver(new StudentObserver());
        subject.registerObserver(new FacultyObserver());
        subject.registerObserver(new CoordinatorObserver());
        subject.notifyObservers(registration.getId(), event.getName(), status);

        System.out.println("\nApplication status: " + status);

        // Bridge
        chooseNotification(student, registration.getId(), event.getName(), status);
    }

    private static void chooseNotification(Student student, int registrationId,
                                           String eventName, String status) {
        System.out.println("\n------------- NOTIFICATION -------------");
        System.out.println("Choose your notification channel:");
        System.out.println("1. Email");
        System.out.println("2. SMS");
        System.out.println("3. Mobile App");

        int choice = readInt("Enter choice: ");

        NotificationChannel channel = switch (choice) {
            case 1 -> new EmailNotification();
            case 2 -> new SmsNotification();
            case 3 -> new MobileAppNotification();
            default -> null;
        };

        if (channel == null) {
            System.out.println("Invalid channel. Notification was not sent.");
            return;
        }

        EventNotifier notifier = new EventNotifier(channel);
        String message = "Registration #" + registrationId + " for "
                + eventName + " is " + status + ".";

        notifier.notify(student.getEmail(), message);
        DB.addNotification(registrationId, student.getEmail(),
                notifier.getChannelName(), message);
    }

    private static void showMyRegistrations(Student student) {
        List<String[]> rows = DB.getRegistrationsForStudent(student.getId());

        System.out.println("\n================ MY REGISTRATIONS ================");
        if (rows.isEmpty()) {
            System.out.println("You have not registered for any event.");
            return;
        }

        for (String[] r : rows) {
            System.out.printf("#%s | %s | %s | %s%n",
                    r[0], r[1], r[2], r[3]);
        }
    }

    private static void deleteRegistration(Student student) {
        List<String[]> rows = DB.getRegistrationsForStudent(student.getId());

        if (rows.isEmpty()) {
            System.out.println("You have no registrations to cancel.");
            return;
        }

        showMyRegistrations(student);
        int id = readInt("Enter Registration ID to cancel: ");

        if (DB.deleteRegistration(id, student.getId())) {
            System.out.println("Registration cancelled successfully.");
        } else {
            System.out.println("Registration not found or it does not belong to you.");
        }
    }

    private static void coordinatorPanel() {
        System.out.println("\n--------------- COORDINATOR PANEL ---------------");
        String code = readNonEmpty("Coordinator access code: ");

        if (!"admin123".equals(code)) {
            System.out.println("Invalid access code.");
            return;
        }

        while (true) {
            System.out.println("\n------------- COORDINATOR MENU -------------");
            System.out.println("1. Create Event");
            System.out.println("2. System Demonstrations");
            System.out.println("3. Back");

            int choice = readInt("Enter choice: ");

            switch (choice) {
                case 1 -> createEvent();
                case 2 -> systemDemonstrations();
                case 3 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void createEvent() {
        System.out.println("\n---------------- CREATE EVENT ----------------");
        System.out.println("1. Technical");
        System.out.println("2. Cultural");
        System.out.println("3. Sports");
        System.out.println("4. Workshop");

        int type = readInt("Select category: ");

        if (type < 1 || type > 4) {
            System.out.println("Invalid category.");
            return;
        }

        String name = readNonEmpty("Event name: ");
        String description = readNonEmpty("Description: ");
        String date = readNonEmpty("Date (YYYY-MM-DD): ");
        String venue = readNonEmpty("Venue: ");
        int capacity = readInt("Capacity: ");

        if (capacity <= 0) {
            System.out.println("Capacity must be greater than zero.");
            return;
        }

        EventFactory factory = switch (type) {
            case 1 -> new TechnicalEventFactory();
            case 2 -> new CulturalEventFactory();
            case 3 -> new SportsEventFactory();
            case 4 -> new WorkshopEventFactory();
            default -> null;
        };

        Event event = factory.createEvent(name, description, date, venue, capacity);
        int id = DB.createEvent(event);

        System.out.println("\nEvent created successfully.");
        System.out.println("Event ID: #" + id);
    }

    private static void systemDemonstrations() {
        while (true) {
            System.out.println("\n------------- SYSTEM DEMONSTRATIONS -------------");
            System.out.println("1. Create Sample Event");
            System.out.println("2. Show Event Category Services");
            System.out.println("3. Verify Central System");
            System.out.println("4. Back");

            int choice = readInt("Enter choice: ");

            switch (choice) {
                case 1 -> createEvent();
                case 2 -> abstractFactoryDemo();
                case 3 -> singletonDemo();
                case 4 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void abstractFactoryDemo() {
        System.out.println("\n------------- EVENT CATEGORY SERVICES -------------");

        EventFamilyFactory technical =
                new com.college.event.patterns.abstractfactory.TechnicalEventFactory();
        EventFamilyFactory cultural =
                new com.college.event.patterns.abstractfactory.CulturalEventFactory();
        EventFamilyFactory sports =
                new com.college.event.patterns.abstractfactory.SportsEventFactory();

        System.out.println(technical.createRegistrationLabel());
        System.out.println(technical.createCertificateLabel());

        System.out.println(cultural.createRegistrationLabel());
        System.out.println(cultural.createCertificateLabel());

        System.out.println(sports.createRegistrationLabel());
        System.out.println(sports.createCertificateLabel());
    }

    private static void singletonDemo() {
        EventManagementSystem a = EventManagementSystem.getInstance();
        EventManagementSystem b = EventManagementSystem.getInstance();

        System.out.println("\n------------- CENTRAL SYSTEM CHECK -------------");
        System.out.println("Single instance active: " + (a == b));
        System.out.println("Instance A: " + System.identityHashCode(a));
        System.out.println("Instance B: " + System.identityHashCode(b));
    }

    private static void patternLabMenu() {
        while (true) {
            System.out.println("\n============= DESIGN PATTERN LAB =============");
            System.out.println("1. Prototype       - Clone Event as a Separate Draft");
            System.out.println("2. Mediator        - Coordinated Registration & Approval Workflow");
            System.out.println("3. Template Method - Technical vs Cultural Workflow Pipelines");
            System.out.println("4. Strategy        - Fee Calculation (FREE, STANDARD, EARLY_BIRD)");
            System.out.println("5. Memento         - Event Version History & 1-Click Restore");
            System.out.println("6. MVC             - Model-View-Controller Architecture Inspection");
            System.out.println("7. Back to Main Menu");

            int choice = readInt("Enter pattern selection: ");
            switch (choice) {
                case 1 -> prototypeDemo();
                case 2 -> mediatorDemo();
                case 3 -> templateMethodDemo();
                case 4 -> strategyDemo();
                case 5 -> mementoDemo();
                case 6 -> mvcDemo();
                case 7 -> { return; }
                default -> System.out.println("Invalid selection. Please choose 1-7.");
            }
        }
    }

    private static void prototypeDemo() {
        System.out.println("\n---------------- PROTOTYPE PATTERN ----------------");
        System.out.println("Concept: Copies an existing object to create another.");
        System.out.println("Usage  : Clone an event as a separate draft.");

        List<Event> events = DB.getEvents();
        if (events.isEmpty()) {
            System.out.println("No events exist to clone.");
            return;
        }

        showEvents();
        int sel = readInt("\nSelect event number to clone as draft: ");
        if (sel < 1 || sel > events.size()) {
            System.out.println("Invalid selection.");
            return;
        }

        Event source = events.get(sel - 1);
        CollegeEventMediator mediator = new CollegeEventMediator(DB);
        String customName = readNonEmpty("Enter draft title (or type 'default' to auto-prefix): ");
        if ("default".equalsIgnoreCase(customName)) {
            customName = null;
        }

        Event draft = mediator.mediateEventClone(source.getId(), customName);
        System.out.println("\n✓ Cloned Prototype Created Successfully!");
        System.out.println("Draft Event ID : #" + draft.getId());
        System.out.println("Draft Name     : " + draft.getName());
        System.out.println("Draft Status   : " + draft.getStatus());
        System.out.println("Category       : " + draft.getCategory());
        System.out.println("Capacity       : " + draft.getCapacity());
        System.out.println("Venue          : " + draft.getVenue());
    }

    private static void mediatorDemo() {
        System.out.println("\n----------------- MEDIATOR PATTERN -----------------");
        System.out.println("Concept: Centralizes communication among collaborators.");
        System.out.println("Usage  : Coordinates workflow interactions between Student,");
        System.out.println("         Event, Fee Strategy, Approval Chain & Notifications.");

        List<Event> events = DB.getEvents();
        if (events.isEmpty()) {
            System.out.println("No events available.");
            return;
        }

        showEvents();
        int sel = readInt("\nSelect event number: ");
        if (sel < 1 || sel > events.size()) return;
        Event event = events.get(sel - 1);

        System.out.println("\nSelect Fee Tier (Strategy):");
        System.out.println("1. FREE (College sponsored waiver)");
        System.out.println("2. STANDARD (Regular delegate pass)");
        System.out.println("3. EARLY_BIRD (30% discount)");
        int tierChoice = readInt("Enter choice: ");
        String tier = switch (tierChoice) {
            case 1 -> "FREE";
            case 3 -> "EARLY_BIRD";
            default -> "STANDARD";
        };

        CollegeEventMediator mediator = new CollegeEventMediator(DB);
        // Execute mediated flow using demo student (ID=1)
        MediatedRegistrationResult result = mediator.mediateRegistration(1, event.getId(), tier, "EMAIL");

        System.out.println("\nMediator Execution Timeline:");
        for (String step : result.getCoordinationAuditLog()) {
            System.out.println("  " + step);
        }
        System.out.println("\nFinal Outcome: " + result.getStatus() + " - " + result.getMessage());
    }

    private static void templateMethodDemo() {
        System.out.println("\n------------- TEMPLATE METHOD PATTERN -------------");
        System.out.println("Concept: Defines workflow skeleton with variable steps.");
        System.out.println("Usage  : Technical/Cultural/Sports deployment pipelines.");

        System.out.println("\nSelect Template to Execute:");
        System.out.println("1. Technical Event Workflow (GitHub, Docker, 1Gbps LAN, CI/CD Rubric)");
        System.out.println("2. Cultural Event Workflow  (AV Sound, Green Rooms, LED Stage, Jury QR)");
        System.out.println("3. Sports Event Workflow    (Pitch Turf, Red Cross Paramedics, Bracket)");

        int choice = readInt("Enter template choice: ");
        EventWorkflowTemplate workflow = switch (choice) {
            case 1 -> new TechnicalEventWorkflow();
            case 2 -> new CulturalEventWorkflow();
            case 3 -> new SportsEventWorkflow();
            default -> null;
        };

        if (workflow == null) {
            System.out.println("Invalid selection.");
            return;
        }

        Event sampleEvent = new BaseEvent("Campus Spring Conclave 2026", workflow.getCategory(),
                "Annual flagship symposium", "2026-11-15", "Auditorium & Labs", 250);

        WorkflowExecutionReport report = workflow.executeWorkflow(sampleEvent);
        System.out.println("\nTemplate Method Execution: " + report.getWorkflowName());
        System.out.println("Target Event: " + report.getEventName());
        System.out.println("==============================================================");
        for (WorkflowStepLog step : report.getSteps()) {
            System.out.println(step.toString());
        }
        System.out.println("==============================================================");
        System.out.println("Result: " + report.getSummary());
    }

    private static void strategyDemo() {
        System.out.println("\n---------------- STRATEGY PATTERN ----------------");
        System.out.println("Concept: Interchangeable algorithms behind one interface.");
        System.out.println("Usage  : Fee calculation: FREE, STANDARD, EARLY_BIRD.");

        List<Event> events = DB.getEvents();
        Event sample = events.isEmpty()
                ? new BaseEvent("CodeQuest", "TECHNICAL", "Coding", "2026-10-10", "Lab", 100)
                : events.get(0);

        System.out.println("\nEvent Selected: " + sample.getName() + " [" + sample.getCategory() + "]");

        FeeStrategyContext context = new FeeStrategyContext();

        System.out.println("\n1. Applying [FREE] Strategy:");
        context.setStrategy(new FreeFeeStrategy());
        System.out.println("   " + context.executeStrategy(sample, null));

        System.out.println("\n2. Applying [STANDARD] Strategy:");
        context.setStrategy(new StandardFeeStrategy());
        System.out.println("   " + context.executeStrategy(sample, null));

        System.out.println("\n3. Applying [EARLY_BIRD] Strategy (30% Discount):");
        context.setStrategy(new EarlyBirdFeeStrategy());
        System.out.println("   " + context.executeStrategy(sample, null));
    }

    private static void mementoDemo() {
        System.out.println("\n----------------- MEMENTO PATTERN -----------------");
        System.out.println("Concept: Captures state for later restore.");
        System.out.println("Usage  : Event version/history and restore flow.");

        List<Event> events = DB.getEvents();
        if (events.isEmpty()) {
            System.out.println("No events found.");
            return;
        }

        showEvents();
        int sel = readInt("\nSelect event to inspect version checkpoints: ");
        if (sel < 1 || sel > events.size()) return;
        Event event = events.get(sel - 1);

        EventController controller = new EventController(DB);
        EventModel model = controller.loadEventModel(event.getId(), "STANDARD");

        List<EventMemento> history = model.getVersionHistory();
        System.out.println("\nCheckpoint Revision History for Event #" + event.getId() + ":");
        if (history.isEmpty()) {
            System.out.println("  No revisions recorded yet. Creating initial checkpoint...");
            EventMemento v1 = EventCaretaker.getInstance().saveCheckpoint(event, "Initial Snapshot", "Coordinator");
            history = List.of(v1);
        }

        for (EventMemento m : history) {
            System.out.printf("  v%d [%s] - %s | Venue: %s | Cap: %d (%s)%n",
                    m.getVersion(), m.getTimestamp(), m.getChangeSummary(),
                    m.getVenue(), m.getCapacity(), m.getAuthor());
        }

        System.out.println("\nOptions:");
        System.out.println("1. Create new checkpoint with modified venue & capacity");
        System.out.println("2. Restore to earlier version");
        System.out.println("3. Back");

        int opt = readInt("Enter choice: ");
        if (opt == 1) {
            String newVenue = readNonEmpty("Enter new venue: ");
            int newCap = readInt("Enter new capacity: ");
            String note = readNonEmpty("Enter change reason: ");
            EventModel updated = controller.updateEventWithCheckpoint(
                    event.getId(), event.getName(), event.getCategory(),
                    event.getDescription(), event.getDate(), newVenue, newCap,
                    event.getStatus(), note, "Coordinator Admin");
            System.out.println("\n✓ Checkpoint recorded! Updated venue: " + updated.getEvent().getVenue());
        } else if (opt == 2) {
            int ver = readInt("Enter version number to restore: ");
            EventModel restored = controller.restoreFromVersion(event.getId(), ver);
            if (restored != null) {
                System.out.println("\n✓ State restored to v" + ver + "!");
                System.out.println(controller.getView().renderConsoleCard(restored));
            } else {
                System.out.println("Version not found.");
            }
        }
    }

    private static void mvcDemo() {
        System.out.println("\n------------------- MVC PATTERN -------------------");
        System.out.println("Concept: Separates Model, View and Controller.");
        System.out.println("Model     : EventModel encapsulates domain state & business rules.");
        System.out.println("View      : EventView formats presentation without business logic.");
        System.out.println("Controller: EventController handles user requests, updates Model & passes to View.");

        List<Event> events = DB.getEvents();
        if (events.isEmpty()) return;

        EventController controller = new EventController(DB);
        EventModel model = controller.loadEventModel(events.get(0).getId(), "EARLY_BIRD");

        System.out.println("\n[Rendering presentation via EventView]:");
        System.out.println(controller.getView().renderConsoleCard(model));
    }

    private static void launchWebServer() {
        try {
            WebServer server = new WebServer(8085);
            server.start();
            System.out.println("\n>>> Open your browser at: http://localhost:8085 <<<");
            System.out.println("Press Enter to return to main menu (server continues running)...");
            sc.nextLine();
        } catch (Exception e) {
            System.out.println("Web server started or already active on port 8085: " + e.getMessage());
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static String readNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = sc.nextLine().trim();
            if (!value.isEmpty()) return value;
            System.out.println("This field cannot be empty.");
        }
    }
}
