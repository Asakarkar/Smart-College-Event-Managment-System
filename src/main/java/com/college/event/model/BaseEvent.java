package com.college.event.model;

import com.college.event.patterns.prototype.EventPrototype;

public class BaseEvent implements Event, EventPrototype, Cloneable {
    private final int id;
    private final String name;
    private final String category;
    private final String description;
    private final String date;
    private final String venue;
    private final int capacity;
    private final String status;

    public BaseEvent(String name, String category, String description,
                     String date, String venue, int capacity) {
        this(0, name, category, description, date, venue, capacity, "OPEN");
    }

    public BaseEvent(int id, String name, String category, String description,
                     String date, String venue, int capacity, String status) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.date = date;
        this.venue = venue;
        this.capacity = capacity;
        this.status = (status != null && !status.isBlank()) ? status : "OPEN";
    }

    @Override
    public int getId() { return id; }

    @Override
    public String getName() { return name; }

    @Override
    public String getCategory() { return category; }

    @Override
    public String getDescription() { return description; }

    @Override
    public String getDate() { return date; }

    @Override
    public String getVenue() { return venue; }

    @Override
    public int getCapacity() { return capacity; }

    @Override
    public String getStatus() { return status; }

    /**
     * Prototype Pattern: Clones an existing event into a separate draft.
     */
    @Override
    public BaseEvent cloneDraft() {
        String draftName = name.startsWith("[Draft] ") ? name : "[Draft] " + name;
        return new BaseEvent(0, draftName, category, description, date, venue, capacity, "DRAFT");
    }

    /**
     * Prototype Pattern: Clones with customized parameters.
     */
    @Override
    public BaseEvent cloneWithCustomizations(String newName, String newDate, String newVenue) {
        return new BaseEvent(
                0,
                (newName != null && !newName.isBlank()) ? newName : "[Draft] " + name,
                category,
                description,
                (newDate != null && !newDate.isBlank()) ? newDate : date,
                (newVenue != null && !newVenue.isBlank()) ? newVenue : venue,
                capacity,
                "DRAFT"
        );
    }

    @Override
    public BaseEvent clone() {
        return new BaseEvent(id, name, category, description, date, venue, capacity, status);
    }

    @Override
    public String toString() {
        return name + " [" + category + "] (" + status + ") - " + date + " @ " + venue;
    }
}