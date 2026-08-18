package com.college.event.model;

public class BaseEvent implements Event {
    private final String name;
    private final String category;
    private final String description;
    private final String date;
    private final String venue;
    private final int capacity;

    public BaseEvent(String name, String category, String description,
                     String date, String venue, int capacity) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.date = date;
        this.venue = venue;
        this.capacity = capacity;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getDate() { return date; }
    public String getVenue() { return venue; }
    public int getCapacity() { return capacity; }

    @Override
    public String toString() {
        return name + " [" + category + "] - " + date + " @ " + venue;
    }
}