package com.college.event.patterns.memento;

import com.college.event.model.BaseEvent;
import com.college.event.model.Event;

/**
 * Originator in Memento Pattern:
 * Creates mementos containing snapshots of its current state,
 * and uses mementos to restore its state.
 */
public class EventOriginator {
    private int eventId;
    private String name;
    private String category;
    private String description;
    private String venue;
    private String date;
    private int capacity;
    private String status;

    public EventOriginator() {}

    public EventOriginator(Event event) {
        setFromEvent(event);
    }

    public void setFromEvent(Event event) {
        if (event != null) {
            this.eventId = event.getId();
            this.name = event.getName();
            this.category = event.getCategory();
            this.description = event.getDescription();
            this.venue = event.getVenue();
            this.date = event.getDate();
            this.capacity = event.getCapacity();
            this.status = event.getStatus();
        }
    }

    public Event toEvent() {
        return new BaseEvent(eventId, name, category, description, date, venue, capacity, status);
    }

    /**
     * Creates a Memento snapshot with the specified version and change note.
     */
    public EventMemento saveToMemento(int version, String changeSummary, String author) {
        return new EventMemento(
                version,
                eventId,
                name,
                category,
                description,
                venue,
                date,
                capacity,
                status,
                changeSummary,
                author
        );
    }

    /**
     * Restores internal state from a Memento snapshot.
     */
    public void restoreFromMemento(EventMemento memento) {
        if (memento == null) return;
        this.eventId = memento.getEventId();
        this.name = memento.getName();
        this.category = memento.getCategory();
        this.description = memento.getDescription();
        this.venue = memento.getVenue();
        this.date = memento.getDate();
        this.capacity = memento.getCapacity();
        this.status = memento.getStatus();
    }

    // Getters and Setters for mutable state
    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
