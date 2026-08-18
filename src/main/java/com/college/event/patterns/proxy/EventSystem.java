package com.college.event.patterns.proxy;

import com.college.event.model.Registration;
import com.college.event.model.Student;

public interface EventSystem {
    Student login(String username, String password);
    Registration register(int eventId);
}