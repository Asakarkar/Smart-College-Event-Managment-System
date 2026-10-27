package com.college.event.patterns.mvc.view;

import com.college.event.model.Event;
import com.college.event.patterns.memento.EventMemento;
import com.college.event.patterns.mvc.model.EventModel;

import java.util.HashMap;
import java.util.Map;

/**
 * View in MVC:
 * Responsible purely for presentation formatting (console text, structured DTO maps, HTML badges)
 * without making business logic or storage decisions.
 */
public class EventView {

    public String renderConsoleCard(EventModel model) {
        if (model == null || model.getEvent() == null) {
            return "[Empty Event View]";
        }
        Event e = model.getEvent();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("====================================================%n"));
        sb.append(String.format("  EVENT #%d: %s%n", e.getId(), e.getName()));
        sb.append(String.format("====================================================%n"));
        sb.append(String.format("  Category     : %s%n", e.getCategory()));
        sb.append(String.format("  Status       : %s%n", e.getStatus()));
        sb.append(String.format("  Date & Venue : %s @ %s%n", e.getDate(), e.getVenue()));
        sb.append(String.format("  Capacity     : %d/%d seats filled (%d remaining)%n",
                model.getRegisteredSeats(), e.getCapacity(), model.getAvailableSeats()));
        sb.append(String.format("  Description  : %s%n", e.getDescription()));

        if (model.getCurrentFeeBreakdown() != null) {
            sb.append(String.format("  Active Fee   : %s%n", model.getCurrentFeeBreakdown().toString()));
        }

        if (model.getVersionHistory() != null && !model.getVersionHistory().isEmpty()) {
            sb.append(String.format("  Revisions    : %d checkpoints on record%n", model.getVersionHistory().size()));
            for (EventMemento m : model.getVersionHistory()) {
                sb.append(String.format("    - v%d [%s]: %s (%s)%n",
                        m.getVersion(), m.getTimestamp(), m.getChangeSummary(), m.getAuthor()));
            }
        }
        sb.append(String.format("====================================================%n"));
        return sb.toString();
    }

    public Map<String, Object> renderViewModelMap(EventModel model) {
        Map<String, Object> map = new HashMap<>();
        if (model == null || model.getEvent() == null) return map;

        Event e = model.getEvent();
        map.put("id", e.getId());
        map.put("name", e.getName());
        map.put("category", e.getCategory());
        map.put("description", e.getDescription());
        map.put("date", e.getDate());
        map.put("venue", e.getVenue());
        map.put("capacity", e.getCapacity());
        map.put("status", e.getStatus());
        map.put("registeredSeats", model.getRegisteredSeats());
        map.put("availableSeats", model.getAvailableSeats());
        map.put("isFull", model.isFull());
        map.put("feeBreakdown", model.getCurrentFeeBreakdown());
        map.put("versionHistory", model.getVersionHistory());
        return map;
    }
}
