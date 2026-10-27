package com.college.event.server;

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
import com.college.event.patterns.proxy.EventSystemProxy;
import com.college.event.patterns.singleton.EventManagementSystem;
import com.college.event.patterns.strategy.EarlyBirdFeeStrategy;
import com.college.event.patterns.strategy.FeeBreakdown;
import com.college.event.patterns.strategy.FeeStrategyContext;
import com.college.event.patterns.strategy.FreeFeeStrategy;
import com.college.event.patterns.strategy.StandardFeeStrategy;
import com.college.event.patterns.templatemethod.WorkflowExecutionReport;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Fullstack REST API and Static File Server.
 * Exposes all 13 Design Patterns in the project via RESTful endpoints.
 */
public class WebServer {
    private static final int DEFAULT_PORT = 8085;
    private final HttpServer server;
    private final DatabaseManager db;
    private final EventController eventController;
    private final CollegeEventMediator mediator;
    private final EventCaretaker caretaker;
    private final Gson gson;
    private final Path staticDir;

    public WebServer(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.db = EventManagementSystem.getInstance().getDatabase();
        this.eventController = new EventController(db);
        this.mediator = new CollegeEventMediator(db);
        this.caretaker = EventCaretaker.getInstance();
        this.gson = new Gson();
        this.staticDir = Paths.get("frontend", "dist").toAbsolutePath();

        // Seed initial checkpoints for existing events if not already done
        seedInitialCheckpoints();

        setupRoutes();
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
    }

    private void seedInitialCheckpoints() {
        for (Event e : db.getEvents()) {
            if (caretaker.getHistory(e.getId()).isEmpty()) {
                caretaker.saveCheckpoint(e, "Initial Event Creation & System Seeding", "System Admin");
            }
        }
    }

    private void setupRoutes() {
        // API Endpoints
        server.createContext("/api/events", this::handleEvents);
        server.createContext("/api/fee/calculate", this::handleFeeCalculate);
        server.createContext("/api/registrations/mediate", this::handleMediatedRegistration);
        server.createContext("/api/registrations/student", this::handleStudentRegistrations);
        server.createContext("/api/registrations/cancel", this::handleCancelRegistration);
        server.createContext("/api/auth/login", this::handleAuthLogin);
        server.createContext("/api/auth/register", this::handleAuthRegister);
        server.createContext("/api/patterns/summary", this::handlePatternsSummary);

        // Static Frontend SPA Handlers
        server.createContext("/", this::handleStaticFiles);
    }

    public void start() {
        server.start();
        System.out.println("==================================================================");
        System.out.println("  SMART COLLEGE EVENT MANAGEMENT - FULLSTACK HTTP SERVER ACTIVE");
        System.out.println("  Web UI & REST API: http://localhost:" + server.getAddress().getPort());
        System.out.println("==================================================================");
    }

    public void stop() {
        server.stop(0);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    // ==================== REST HANDLERS ====================

    private void handleEvents(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            // GET /api/events or /api/events/{id} or /api/events/{id}/memento/history
            if ("GET".equals(method)) {
                if (path.matches(".*/api/events/\\d+/memento/history.*")) {
                    int eventId = extractId(path, "/api/events/(\\d+)/memento/history");
                    List<EventMemento> history = caretaker.getHistory(eventId);
                    sendJsonResponse(exchange, 200, history);
                    return;
                }

                if (path.matches(".*/api/events/\\d+")) {
                    int eventId = extractId(path, "/api/events/(\\d+)");
                    EventModel model = eventController.loadEventModel(eventId, "STANDARD");
                    if (model == null) {
                        sendJsonResponse(exchange, 404, Map.of("error", "Event not found"));
                        return;
                    }
                    sendJsonResponse(exchange, 200, eventController.getView().renderViewModelMap(model));
                    return;
                }

                // List all events
                List<Event> events = db.getEvents();
                List<Map<String, Object>> responseList = new ArrayList<>();
                for (Event e : events) {
                    int registered = db.getRegistrationCount(e.getId());
                    EventModel model = new EventModel(e, registered);
                    model.setVersionHistory(caretaker.getHistory(e.getId()));
                    responseList.add(eventController.getView().renderViewModelMap(model));
                }
                sendJsonResponse(exchange, 200, responseList);
                return;
            }

            // POST /api/events (Create)
            if ("POST".equals(method)) {
                if (path.matches(".*/api/events/\\d+/clone.*")) {
                    // Prototype Pattern: Clone event as draft
                    int sourceId = extractId(path, "/api/events/(\\d+)/clone");
                    String body = readBody(exchange);
                    JsonObject json = parseJson(body);
                    String customName = json.has("customName") ? json.get("customName").getAsString() : null;

                    EventModel clonedModel = eventController.cloneEventAsDraft(sourceId, customName);
                    if (clonedModel == null) {
                        sendJsonResponse(exchange, 404, Map.of("error", "Source event not found"));
                        return;
                    }
                    sendJsonResponse(exchange, 201, Map.of(
                            "message", "Prototype cloned successfully as a separate draft",
                            "pattern", "Prototype",
                            "event", eventController.getView().renderViewModelMap(clonedModel)
                    ));
                    return;
                }

                if (path.matches(".*/api/events/\\d+/workflow.*")) {
                    // Template Method Pattern: Run workflow pipeline
                    int eventId = extractId(path, "/api/events/(\\d+)/workflow");
                    WorkflowExecutionReport report = eventController.runWorkflow(eventId);
                    sendJsonResponse(exchange, 200, Map.of(
                            "pattern", "Template Method",
                            "report", report
                    ));
                    return;
                }

                if (path.matches(".*/api/events/\\d+/memento/restore.*")) {
                    // Memento Pattern: Restore to previous version
                    int eventId = extractId(path, "/api/events/(\\d+)/memento/restore");
                    String body = readBody(exchange);
                    JsonObject json = parseJson(body);
                    int targetVersion = json.has("targetVersion") ? json.get("targetVersion").getAsInt() : 1;

                    EventModel restoredModel = eventController.restoreFromVersion(eventId, targetVersion);
                    if (restoredModel == null) {
                        sendJsonResponse(exchange, 404, Map.of("error", "Version snapshot not found"));
                        return;
                    }
                    sendJsonResponse(exchange, 200, Map.of(
                            "message", "State successfully restored to version " + targetVersion,
                            "pattern", "Memento",
                            "event", eventController.getView().renderViewModelMap(restoredModel)
                    ));
                    return;
                }

                // Standard Event Creation
                String body = readBody(exchange);
                JsonObject json = parseJson(body);
                String name = json.get("name").getAsString();
                String category = json.get("category").getAsString();
                String description = json.has("description") ? json.get("description").getAsString() : "";
                String date = json.get("date").getAsString();
                String venue = json.get("venue").getAsString();
                int capacity = json.get("capacity").getAsInt();
                String status = json.has("status") ? json.get("status").getAsString() : "OPEN";

                BaseEvent event = new BaseEvent(0, name, category, description, date, venue, capacity, status);
                int createdId = db.createEvent(event);
                Event saved = db.getEventById(createdId);
                caretaker.saveCheckpoint(saved, "Initial event creation", json.has("author") ? json.get("author").getAsString() : "Coordinator");

                EventModel model = eventController.loadEventModel(createdId, "STANDARD");
                sendJsonResponse(exchange, 201, Map.of(
                        "message", "Event created successfully",
                        "event", eventController.getView().renderViewModelMap(model)
                ));
                return;
            }

            // PUT /api/events/{id} (Update with Memento Checkpoint)
            if ("PUT".equals(method)) {
                int eventId = extractId(path, "/api/events/(\\d+)");
                String body = readBody(exchange);
                JsonObject json = parseJson(body);

                String name = json.get("name").getAsString();
                String category = json.get("category").getAsString();
                String description = json.has("description") ? json.get("description").getAsString() : "";
                String date = json.get("date").getAsString();
                String venue = json.get("venue").getAsString();
                int capacity = json.get("capacity").getAsInt();
                String status = json.has("status") ? json.get("status").getAsString() : "OPEN";
                String changeReason = json.has("changeReason") ? json.get("changeReason").getAsString() : "Updated details";
                String author = json.has("author") ? json.get("author").getAsString() : "Coordinator";

                EventModel updatedModel = eventController.updateEventWithCheckpoint(
                        eventId, name, category, description, date, venue, capacity, status, changeReason, author);

                if (updatedModel == null) {
                    sendJsonResponse(exchange, 404, Map.of("error", "Event not found"));
                    return;
                }

                sendJsonResponse(exchange, 200, Map.of(
                        "message", "Event updated and Memento snapshot recorded",
                        "pattern", "Memento",
                        "event", eventController.getView().renderViewModelMap(updatedModel)
                ));
                return;
            }

            sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500, Map.of("error", e.getMessage() != null ? e.getMessage() : "Internal server error"));
        }
    }

    private void handleFeeCalculate(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            String body = readBody(exchange);
            JsonObject json = parseJson(body);

            int eventId = json.has("eventId") ? json.get("eventId").getAsInt() : 0;
            String category = json.has("category") ? json.get("category").getAsString() : "TECHNICAL";
            String tier = json.has("tier") ? json.get("tier").getAsString() : "STANDARD";

            Event event = (eventId > 0) ? db.getEventById(eventId) : new BaseEvent("Sample", category, "", "2026-10-01", "Lab", 100);

            FeeStrategyContext ctx = new FeeStrategyContext(tier);
            FeeBreakdown breakdown = ctx.executeStrategy(event, null);

            // Also provide comparison with all three strategies
            FeeBreakdown freeComparison = new FreeFeeStrategy().calculateFee(event, null);
            FeeBreakdown stdComparison = new StandardFeeStrategy().calculateFee(event, null);
            FeeBreakdown earlyComparison = new EarlyBirdFeeStrategy().calculateFee(event, null);

            sendJsonResponse(exchange, 200, Map.of(
                    "pattern", "Strategy",
                    "selectedTier", tier,
                    "appliedBreakdown", breakdown,
                    "comparison", Map.of(
                            "FREE", freeComparison,
                            "STANDARD", stdComparison,
                            "EARLY_BIRD", earlyComparison
                    )
            ));
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleMediatedRegistration(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            String body = readBody(exchange);
            JsonObject json = parseJson(body);

            int studentId = json.get("studentId").getAsInt();
            int eventId = json.get("eventId").getAsInt();
            String feeTier = json.has("feeTier") ? json.get("feeTier").getAsString() : "STANDARD";
            String channel = json.has("notificationChannel") ? json.get("notificationChannel").getAsString() : "EMAIL";

            MediatedRegistrationResult result = mediator.mediateRegistration(studentId, eventId, feeTier, channel);

            int statusCode = result.isSuccess() ? 200 : 400;
            sendJsonResponse(exchange, statusCode, Map.of(
                    "pattern", "Mediator",
                    "success", result.isSuccess(),
                    "status", result.getStatus(),
                    "message", result.getMessage(),
                    "registration", result.getRegistration() != null ? result.getRegistration() : "",
                    "feeBreakdown", result.getFeeBreakdown() != null ? result.getFeeBreakdown() : "",
                    "auditTrail", result.getCoordinationAuditLog()
            ));
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleStudentRegistrations(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            int studentId = extractId(path, "/api/registrations/student/(\\d+)");
            List<String[]> rows = db.getRegistrationsForStudent(studentId);

            List<Map<String, String>> result = new ArrayList<>();
            for (String[] r : rows) {
                result.add(Map.of(
                        "registrationId", r[0],
                        "eventName", r[1],
                        "category", r[2],
                        "status", r[3]
                ));
            }
            sendJsonResponse(exchange, 200, result);
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleCancelRegistration(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            String body = readBody(exchange);
            JsonObject json = parseJson(body);
            int regId = json.get("registrationId").getAsInt();
            int studentId = json.get("studentId").getAsInt();

            boolean deleted = db.deleteRegistration(regId, studentId);
            if (deleted) {
                sendJsonResponse(exchange, 200, Map.of("success", true, "message", "Registration cancelled successfully"));
            } else {
                sendJsonResponse(exchange, 404, Map.of("success", false, "error", "Registration not found or unauthorized"));
            }
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleAuthLogin(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            String body = readBody(exchange);
            JsonObject json = parseJson(body);
            String username = json.get("username").getAsString();
            String password = json.get("password").getAsString();

            EventSystemProxy proxy = new EventSystemProxy(db);
            Student student = proxy.login(username, password);

            if (student != null) {
                sendJsonResponse(exchange, 200, Map.of(
                        "authenticated", true,
                        "student", student,
                        "role", "STUDENT"
                ));
            } else if ("coordinator".equals(username) && "admin123".equals(password)) {
                sendJsonResponse(exchange, 200, Map.of(
                        "authenticated", true,
                        "username", "coordinator",
                        "role", "COORDINATOR"
                ));
            } else {
                sendJsonResponse(exchange, 401, Map.of(
                        "authenticated", false,
                        "error", "Invalid username or password"
                ));
            }
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleAuthRegister(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            String body = readBody(exchange);
            JsonObject json = parseJson(body);

            String name = json.get("name").getAsString();
            String username = json.get("username").getAsString();
            String password = json.get("password").getAsString();
            String email = json.get("email").getAsString();
            String department = json.get("department").getAsString();

            Student student = db.registerStudent(name, username, password, email, department);
            if (student == null) {
                sendJsonResponse(exchange, 409, Map.of("error", "Username already exists"));
                return;
            }

            sendJsonResponse(exchange, 201, Map.of(
                    "success", true,
                    "student", student,
                    "message", "Account created successfully"
            ));
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handlePatternsSummary(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        List<Map<String, Object>> patterns = List.of(
                Map.of(
                        "name", "Prototype",
                        "category", "Creational",
                        "summary", "Copies an existing object to create another.",
                        "implementation", "EventPrototype interface & BaseEvent.cloneDraft() cloning events as separate drafts.",
                        "status", "ACTIVE"
                ),
                Map.of(
                        "name", "Mediator",
                        "category", "Behavioral",
                        "summary", "Centralizes communication among collaborators.",
                        "implementation", "CollegeEventMediator orchestrates Student, Event, Fee Strategy, Chain of Approval & Notification.",
                        "status", "ACTIVE"
                ),
                Map.of(
                        "name", "Template Method",
                        "category", "Behavioral",
                        "summary", "Defines workflow skeleton with variable steps.",
                        "implementation", "EventWorkflowTemplate with Technical, Cultural & Sports deployment pipeline subclasses.",
                        "status", "ACTIVE"
                ),
                Map.of(
                        "name", "Strategy",
                        "category", "Behavioral",
                        "summary", "Interchangeable algorithms behind one interface.",
                        "implementation", "FeeCalculationStrategy with FREE, STANDARD, and EARLY_BIRD pricing algorithms.",
                        "status", "ACTIVE"
                ),
                Map.of(
                        "name", "Memento",
                        "category", "Behavioral",
                        "summary", "Captures state for later restore.",
                        "implementation", "EventMemento, EventOriginator & EventCaretaker enabling revision history and 1-click restore.",
                        "status", "ACTIVE"
                ),
                Map.of(
                        "name", "MVC",
                        "category", "Architectural",
                        "summary", "Separates Model, View and Controller.",
                        "implementation", "EventModel (state/rules), EventView (UI/JSON presentation), EventController (coordination).",
                        "status", "ACTIVE"
                ),
                Map.of(
                        "name", "Singleton",
                        "category", "Creational",
                        "summary", "One centralized instance.",
                        "implementation", "EventManagementSystem.getInstance() singleton access point.",
                        "status", "ACTIVE"
                ),
                Map.of(
                        "name", "Factory Method",
                        "category", "Creational",
                        "summary", "Decouples event instantiation.",
                        "implementation", "EventFactory with TechnicalEventFactory, CulturalEventFactory, etc.",
                        "status", "ACTIVE"
                ),
                Map.of(
                        "name", "Abstract Factory",
                        "category", "Creational",
                        "summary", "Creates category-specific event families.",
                        "implementation", "EventFamilyFactory producing matching labels and certificates.",
                        "status", "ACTIVE"
                ),
                Map.of(
                        "name", "Proxy",
                        "category", "Structural",
                        "summary", "Controls and protects access to real system.",
                        "implementation", "EventSystemProxy providing authenticated student access control.",
                        "status", "ACTIVE"
                ),
                Map.of(
                        "name", "Bridge",
                        "category", "Structural",
                        "summary", "Decouples abstraction from implementation.",
                        "implementation", "EventNotifier bridging with Email, SMS, and MobileApp channels.",
                        "status", "ACTIVE"
                ),
                Map.of(
                        "name", "Observer",
                        "category", "Behavioral",
                        "summary", "One-to-many subscription notification.",
                        "implementation", "EventCoordinatorNotifier broadcasting to Student, Faculty, and Coordinator observers.",
                        "status", "ACTIVE"
                ),
                Map.of(
                        "name", "Chain of Responsibility",
                        "category", "Behavioral",
                        "summary", "Passes requests along a handler chain.",
                        "implementation", "ApprovalHandler linking EventCoordinator -> FacultyCoordinator -> HOD.",
                        "status", "ACTIVE"
                )
        );

        sendJsonResponse(exchange, 200, Map.of(
                "totalPatterns", patterns.size(),
                "patterns", patterns,
                "database", "SQLite JDBC (college_events.db)",
                "systemStatus", "ONLINE"
        ));
    }

    // ==================== STATIC FILE SERVING ====================

    private void handleStaticFiles(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.startsWith("/api/")) {
            sendJsonResponse(exchange, 404, Map.of("error", "API route not found: " + path));
            return;
        }

        // Clean path and protect against traversal
        String relativePath = path.equals("/") ? "/index.html" : path;
        Path targetFile = staticDir.resolve(relativePath.substring(1)).normalize();

        if (!targetFile.startsWith(staticDir) || !Files.exists(targetFile) || Files.isDirectory(targetFile)) {
            // SPA Fallback to index.html
            targetFile = staticDir.resolve("index.html");
        }

        if (Files.exists(targetFile) && !Files.isDirectory(targetFile)) {
            String mime = determineContentType(targetFile.getFileName().toString());
            byte[] bytes = Files.readAllBytes(targetFile);
            exchange.getResponseHeaders().set("Content-Type", mime);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            String welcomeHtml = getFallbackHtml();
            byte[] bytes = welcomeHtml.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private String determineContentType(String filename) {
        if (filename.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filename.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filename.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filename.endsWith(".json")) return "application/json; charset=UTF-8";
        if (filename.endsWith(".svg")) return "image/svg+xml";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".ico")) return "image/x-icon";
        return "text/plain; charset=UTF-8";
    }

    private String getFallbackHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <title>Smart College Event Management System</title>
              <style>
                body { font-family: system-ui, sans-serif; background: #0f172a; color: #f8fafc; padding: 40px; text-align: center; }
                h1 { color: #38bdf8; }
                a { color: #818cf8; text-decoration: none; font-weight: bold; }
                .box { max-width: 600px; margin: 40px auto; background: #1e293b; padding: 24px; border-radius: 12px; }
              </style>
            </head>
            <body>
              <div class="box">
                <h1>Smart College Event Management API</h1>
                <p>Backend REST API is running. Building frontend assets...</p>
                <p><a href="/api/patterns/summary" target="_blank">View All Design Patterns JSON &rarr;</a></p>
                <p><a href="/api/events" target="_blank">View Events JSON &rarr;</a></p>
              </div>
            </body>
            </html>
            """;
    }

    // ==================== UTILITY METHODS ====================

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private JsonObject parseJson(String body) {
        if (body == null || body.isBlank()) return new JsonObject();
        return JsonParser.parseString(body).getAsJsonObject();
    }

    private int extractId(String path, String regex) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher m = p.matcher(path);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return -1;
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }
        try {
            WebServer ws = new WebServer(port);
            ws.start();
        } catch (IOException e) {
            System.err.println("Failed to start WebServer on port " + port + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
