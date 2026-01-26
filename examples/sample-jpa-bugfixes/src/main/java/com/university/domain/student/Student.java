package com.university.domain.student;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Student aggregate root.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>BUG-004: List of enums (achievements) generates @ElementCollection with @Enumerated(STRING)</li>
 *   <li>BUG-006: Auditing fields (@CreatedDate, @LastModifiedDate) when enableAuditing=true</li>
 *   <li>BUG-007: Version field (@Version) when enableOptimisticLocking=true</li>
 * </ul>
 */
public class Student {

    private final StudentId id;
    private String firstName;
    private String lastName;
    private String email;
    private StudentStatus status;

    /**
     * List of achievements - demonstrates BUG-004 fix.
     * Generated entity will have @ElementCollection with @Enumerated(EnumType.STRING).
     */
    private final List<AchievementType> achievements;

    public Student(StudentId id, String firstName, String lastName, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.status = StudentStatus.ENROLLED;
        this.achievements = new ArrayList<>();
    }

    public StudentId id() {
        return id;
    }

    public StudentId getId() {
        return id;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public String email() {
        return email;
    }

    public StudentStatus status() {
        return status;
    }

    public List<AchievementType> achievements() {
        return Collections.unmodifiableList(achievements);
    }

    public void updateProfile(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public void addAchievement(AchievementType achievement) {
        if (!achievements.contains(achievement)) {
            achievements.add(achievement);
        }
    }

    public void suspend() {
        this.status = StudentStatus.SUSPENDED;
    }

    public void graduate() {
        this.status = StudentStatus.GRADUATED;
    }
}
