package com.college.event;

import com.college.event.model.Event;
import com.college.event.patterns.factory.TechnicalEventFactory;
import com.college.event.patterns.singleton.EventManagementSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatternTest {

    @Test
    void singletonReturnsSameInstance() {
        assertSame(EventManagementSystem.getInstance(),
                EventManagementSystem.getInstance());
    }

    @Test
    void factoryCreatesTechnicalEvent() {
        Event event = new TechnicalEventFactory()
                .createEvent("Hackathon", "Coding", "2026-09-10",
                        "Lab", 100);
        assertEquals("TECHNICAL", event.getCategory());
        assertEquals("Hackathon", event.getName());
    }
}