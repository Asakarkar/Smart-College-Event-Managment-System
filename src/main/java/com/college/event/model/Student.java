package com.college.event.model;

public class Student {
    private final int id;
    private final String name;
    private final String username;
    private final String password;
    private final String email;
    private final String department;

    public Student(int id, String name, String username, String password,
                   String email, String department) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        this.email = email;
        this.department = department;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
}