package com.college.event.patterns.prototype;

import com.college.event.model.Event;

/**
 * Prototype Pattern: Copies an existing object to create another.
 * Enables cloning an existing event as a separate draft or with customized parameters.
 */
public interface EventPrototype {
    /**
     * Creates a deep-copy draft clone of the event with status set to DRAFT
     * and a draft prefix in the title.
     */
    Event cloneDraft();

    /**
     * Creates a customized clone of the existing event.
     */
    Event cloneWithCustomizations(String newName, String newDate, String newVenue);
}
