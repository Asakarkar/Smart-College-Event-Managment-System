package com.college.event.patterns.mvc.model;

import com.college.event.model.Event;
import com.college.event.patterns.memento.EventMemento;
import com.college.event.patterns.strategy.FeeBreakdown;

import java.util.ArrayList;
import java.util.List;

/**
 * Model in MVC:
 * Represents the event domain state, calculations, and rules.
 * Completely decoupled from views and user interface rendering.
 */
public class EventModel {
    private Event event;
    private int registeredSeats;
    private int availableSeats;
    private FeeBreakdown currentFeeBreakdown;
    private List<EventMemento> versionHistory;

    public EventModel(Event event, int registeredSeats) {
        this.event = event;
        this.registeredSeats = registeredSeats;
        this.availableSeats = Math.max(0, (event != null ? event.getCapacity() : 0) - registeredSeats);
        this.versionHistory = new ArrayList<>();
    }

    public Event getEvent() { return event; }
    public void setEvent(Event event) {
        this.event = event;
        this.availableSeats = Math.max(0, (event != null ? event.getCapacity() : 0) - this.registeredSeats);
    }

    public int getRegisteredSeats() { return registeredSeats; }
    public void setRegisteredSeats(int registeredSeats) {
        this.registeredSeats = registeredSeats;
        this.availableSeats = Math.max(0, (event != null ? event.getCapacity() : 0) - registeredSeats);
    }

    public int getAvailableSeats() { return availableSeats; }

    public boolean isFull() {
        return availableSeats <= 0;
    }

    public FeeBreakdown getCurrentFeeBreakdown() { return currentFeeBreakdown; }
    public void setCurrentFeeBreakdown(FeeBreakdown currentFeeBreakdown) {
        this.currentFeeBreakdown = currentFeeBreakdown;
    }

    public List<EventMemento> getVersionHistory() { return versionHistory; }
    public void setVersionHistory(List<EventMemento> versionHistory) {
        this.versionHistory = versionHistory != null ? versionHistory : new ArrayList<>();
    }
}
