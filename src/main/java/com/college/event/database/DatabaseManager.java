package com.college.event.database;

import com.college.event.model.Event;
import com.college.event.model.Registration;
import com.college.event.model.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:college_events.db";

    public DatabaseManager() {
        initialize();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    private void initialize() {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS students(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    email TEXT NOT NULL,
                    department TEXT NOT NULL
                )
            """);
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS events(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    description TEXT,
                    event_date TEXT NOT NULL,
                    venue TEXT NOT NULL,
                    capacity INTEGER NOT NULL,
                    status TEXT NOT NULL DEFAULT 'OPEN'
                )
            """);
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS registrations(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id INTEGER NOT NULL,
                    event_id INTEGER NOT NULL,
                    registration_date TEXT NOT NULL,
                    status TEXT NOT NULL,
                    UNIQUE(student_id,event_id),
                    FOREIGN KEY(student_id) REFERENCES students(id),
                    FOREIGN KEY(event_id) REFERENCES events(id)
                )
            """);
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS notifications(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    registration_id INTEGER,
                    recipient TEXT NOT NULL,
                    channel TEXT NOT NULL,
                    message TEXT NOT NULL,
                    status TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
            """);
            seed();
        } catch (SQLException e) {
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private void seed() throws SQLException {
        try (Connection c = connect()) {
            if (count(c, "students") == 0) {
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO students(name,username,password,email,department) VALUES(?,?,?,?,?)")) {
                    insertStudent(p, "Aditya", "student1", "1234", "student1@college.edu", "CSE");
                    insertStudent(p, "Riya", "student2", "1234", "student2@college.edu", "ECE");
                }
            }
            if (count(c, "events") == 0) {
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO events(name,category,description,event_date,venue,capacity,status) VALUES(?,?,?,?,?,?,?)")) {
                    insertEvent(p, "TechFest Hackathon", "TECHNICAL", "24-hour coding competition",
                            "2026-09-10", "Innovation Lab", 100, "OPEN");
                    insertEvent(p, "Cultural Night", "CULTURAL", "Music, dance and drama",
                            "2026-09-15", "Auditorium", 300, "OPEN");
                    insertEvent(p, "Inter-College Football", "SPORTS", "Football tournament",
                            "2026-09-20", "Sports Ground", 200, "OPEN");
                    insertEvent(p, "AI Workshop", "WORKSHOP", "Hands-on AI workshop",
                            "2026-09-25", "Seminar Hall", 80, "OPEN");
                }
            }
        }
    }

    private int count(Connection c, String table) throws SQLException {
        try (Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return r.next() ? r.getInt(1) : 0;
        }
    }

    private void insertStudent(PreparedStatement p, String name, String user, String pass,
                               String email, String dept) throws SQLException {
        p.setString(1, name); p.setString(2, user); p.setString(3, pass);
        p.setString(4, email); p.setString(5, dept); p.executeUpdate();
    }

    private void insertEvent(PreparedStatement p, String name, String cat, String desc,
                             String date, String venue, int cap, String status) throws SQLException {
        p.setString(1, name); p.setString(2, cat); p.setString(3, desc);
        p.setString(4, date); p.setString(5, venue); p.setInt(6, cap);
        p.setString(7, status); p.executeUpdate();
    }

    public Student registerStudent(String name, String username, String password,
                                    String email, String department) {
        String sql = "INSERT INTO students(name,username,password,email,department) VALUES(?,?,?,?,?)";
        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            p.setString(1, name);
            p.setString(2, username);
            p.setString(3, password);
            p.setString(4, email);
            p.setString(5, department);
            p.executeUpdate();

            try (ResultSet r = p.getGeneratedKeys()) {
                int id = r.next() ? r.getInt(1) : -1;
                return new Student(id, name, username, password, email, department);
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                return null;
            }
            throw new RuntimeException(e);
        }
    }

    public Student authenticate(String username, String password) {
        String sql = "SELECT * FROM students WHERE username=? AND password=?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, username); p.setString(2, password);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    return new Student(r.getInt("id"), r.getString("name"),
                            r.getString("username"), r.getString("password"),
                            r.getString("email"), r.getString("department"));
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    public List<Event> getEvents() {
        List<Event> result = new ArrayList<>();
        String sql = "SELECT * FROM events ORDER BY event_date";
        try (Connection c = connect(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {
            while (r.next()) {
                result.add(new com.college.event.model.BaseEvent(
                        r.getInt("id"),
                        r.getString("name"), r.getString("category"),
                        r.getString("description"), r.getString("event_date"),
                        r.getString("venue"), r.getInt("capacity"),
                        r.getString("status")));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return result;
    }

    public List<Integer> getEventIds() {
        List<Integer> ids = new ArrayList<>();
        try (Connection c = connect(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT id FROM events ORDER BY event_date")) {
            while (r.next()) ids.add(r.getInt(1));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return ids;
    }

    public Event getEventById(int id) {
        String sql = "SELECT * FROM events WHERE id=?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    return new com.college.event.model.BaseEvent(
                            r.getInt("id"),
                            r.getString("name"), r.getString("category"),
                            r.getString("description"), r.getString("event_date"),
                            r.getString("venue"), r.getInt("capacity"),
                            r.getString("status"));
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    public int createEvent(Event e) {
        String sql = "INSERT INTO events(name,category,description,event_date,venue,capacity,status) VALUES(?,?,?,?,?,?,?)";
        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, e.getName()); p.setString(2, e.getCategory());
            p.setString(3, e.getDescription()); p.setString(4, e.getDate());
            p.setString(5, e.getVenue()); p.setInt(6, e.getCapacity());
            p.setString(7, (e.getStatus() != null && !e.getStatus().isBlank()) ? e.getStatus() : "OPEN");
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) { return r.next() ? r.getInt(1) : -1; }
        } catch (SQLException ex) { throw new RuntimeException(ex); }
    }

    public boolean updateEvent(int id, Event e) {
        String sql = "UPDATE events SET name=?, category=?, description=?, event_date=?, venue=?, capacity=?, status=? WHERE id=?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, e.getName());
            p.setString(2, e.getCategory());
            p.setString(3, e.getDescription());
            p.setString(4, e.getDate());
            p.setString(5, e.getVenue());
            p.setInt(6, e.getCapacity());
            p.setString(7, (e.getStatus() != null && !e.getStatus().isBlank()) ? e.getStatus() : "OPEN");
            p.setInt(8, id);
            return p.executeUpdate() > 0;
        } catch (SQLException ex) { throw new RuntimeException(ex); }
    }

    public boolean updateEventStatus(int id, String status) {
        String sql = "UPDATE events SET status=? WHERE id=?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, status);
            p.setInt(2, id);
            return p.executeUpdate() > 0;
        } catch (SQLException ex) { throw new RuntimeException(ex); }
    }

    public boolean isStudentRegistered(int studentId, int eventId) {
        String sql = "SELECT 1 FROM registrations WHERE student_id=? AND event_id=?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, studentId);
            p.setInt(2, eventId);
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getRegistrationCount(int eventId) {
        String sql = "SELECT COUNT(*) FROM registrations WHERE event_id=? AND status <> 'CANCELLED'";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, eventId);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Registration createRegistration(int studentId, int eventId) {
        String sql = "INSERT INTO registrations(student_id,event_id,registration_date,status) VALUES(?,?,datetime('now'),'PENDING')";
        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setInt(1, studentId); p.setInt(2, eventId); p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) {
                int id = r.next() ? r.getInt(1) : -1;
                return new Registration(id, studentId, eventId, "PENDING");
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) return null;
            throw new RuntimeException(e);
        }
    }

    public void updateRegistrationStatus(int registrationId, String status) {
        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement("UPDATE registrations SET status=? WHERE id=?")) {
            p.setString(1, status); p.setInt(2, registrationId); p.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<String[]> getRegistrationsForStudent(int studentId) {
        List<String[]> rows = new ArrayList<>();
        String sql = """
            SELECT r.id,e.name,e.category,r.status
            FROM registrations r JOIN events e ON e.id=r.event_id
            WHERE r.student_id=? ORDER BY r.id DESC
        """;
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, studentId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) rows.add(new String[]{
                        String.valueOf(r.getInt(1)), r.getString(2),
                        r.getString(3), r.getString(4)});
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return rows;
    }

    public void addNotification(int registrationId, String recipient, String channel, String message) {
        String sql = "INSERT INTO notifications(registration_id,recipient,channel,message,status,created_at) VALUES(?,?,?,?,?,datetime('now'))";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, registrationId); p.setString(2, recipient);
            p.setString(3, channel); p.setString(4, message); p.setString(5, "SENT");
            p.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean deleteRegistration(int id, int studentId) {
        String sql = "DELETE FROM registrations WHERE id=? AND student_id=?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            p.setInt(2, studentId);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}