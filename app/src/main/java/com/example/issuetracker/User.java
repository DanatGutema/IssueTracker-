package com.example.issuetracker;

public class User {
    public String fullName;
    public String email;
    public String dept;
    public String section;
    public String role;

    public String status;

    // Empty constructor required for Firestore
    public User() {}

    public User(String fullName, String email, String dept, String section, String role, String status) {
        this.fullName = fullName;
        this.email = email;
        this.dept = dept;
        this.section = section;
        this.role = role;
        this.status = status;
    }
}
