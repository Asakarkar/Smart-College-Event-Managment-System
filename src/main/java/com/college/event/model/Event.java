package com.college.event.model;

public interface Event {
    String getName();
    String getCategory();
    String getDescription();
    String getDate();
    String getVenue();
    int getCapacity();
    default String getType() { return getCategory(); }
}