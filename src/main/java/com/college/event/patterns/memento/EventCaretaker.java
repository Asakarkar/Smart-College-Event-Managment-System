package com.college.event.patterns.memento;

import com.college.event.model.Event;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caretaker in Memento Pattern:
 * Manages saved mementos without inspecting or modifying their contents.
 * Provides version history, checkpoint creation, and rollback/restore capabilities.
 */
public class EventCaretaker {
    private static final EventCaretaker INSTANCE = new EventCaretaker();

    // Map: eventId -> List of Mementos (ordered by version ascending)
    private final Map<Integer, List<EventMemento>> historyMap = new ConcurrentHashMap<>();

    public static EventCaretaker getInstance() {
        return INSTANCE;
    }

    public synchronized EventMemento saveCheckpoint(Event event, String changeSummary, String author) {
        if (event == null) return null;
        int eventId = event.getId();

        List<EventMemento> list = historyMap.computeIfAbsent(eventId, k -> new ArrayList<>());
        int nextVersion = list.size() + 1;

        EventOriginator originator = new EventOriginator(event);
        EventMemento memento = originator.saveToMemento(nextVersion, changeSummary, author);
        list.add(memento);
        return memento;
    }

    public synchronized List<EventMemento> getHistory(int eventId) {
        List<EventMemento> list = historyMap.get(eventId);
        if (list == null) return Collections.emptyList();
        return new ArrayList<>(list);
    }

    public synchronized EventMemento getMementoByVersion(int eventId, int version) {
        List<EventMemento> list = historyMap.get(eventId);
        if (list == null) return null;
        for (EventMemento m : list) {
            if (m.getVersion() == version) {
                return m;
            }
        }
        return null;
    }

    public synchronized EventMemento getLatestMemento(int eventId) {
        List<EventMemento> list = historyMap.get(eventId);
        if (list == null || list.isEmpty()) return null;
        return list.get(list.size() - 1);
    }

    public synchronized EventMemento rollbackPrevious(int eventId) {
        List<EventMemento> list = historyMap.get(eventId);
        if (list == null || list.size() <= 1) return null;
        // The one before the current latest
        return list.get(list.size() - 2);
    }

    public synchronized void clearHistory(int eventId) {
        historyMap.remove(eventId);
    }
}
