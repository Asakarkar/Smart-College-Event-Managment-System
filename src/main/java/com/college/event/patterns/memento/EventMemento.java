package com.college.event.patterns.memento;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Memento Pattern: Stores an immutable snapshot of the internal state of an Event
 * for later inspection or restoration.
 */
public class EventMemento {
    private final int version;
    private final int eventId;
    private final String name;
    private final String category;
    private final String description;
    private final String venue;
    private final String date;
    private final int capacity;
    private final String status;
    private final String timestamp;
    private final String changeSummary;
    private final String author;

    public EventMemento(int version, int eventId, String name, String category,
                        String description, String venue, String date,
                        int capacity, String status, String changeSummary, String author) {
        this.version = version;
        this.eventId = eventId;
        this.name = name;
        this.category = category;
        this.description = description;
        this.venue = venue;
        this.date = date;
        this.capacity = capacity;
        this.status = status;
        this.changeSummary = changeSummary;
        this.author = (author != null && !author.isBlank()) ? author : "System Coordinator";
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public int getVersion() { return version; }
    public int getEventId() { return eventId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getVenue() { return venue; }
    public String getDate() { return date; }
    public int getCapacity() { return capacity; }
    public String getStatus() { return status; }
    public String getTimestamp() { return timestamp; }
    public String getChangeSummary() { return changeSummary; }
    public String getAuthor() { return author; }

    @Override
    public String toString() {
        return String.format("v%d [%s] %s (@ %s, Cap: %d, Status: %s) - %s by %s",
                version, timestamp, name, venue, capacity, status, changeSummary, author);
    }
}
