package com.university.domain.course;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tag aggregate for categorizing courses.
 *
 * <p>Demonstrates BUG-001 and BUG-003 fixes:
 * <ul>
 *   <li>BUG-001: @ManyToMany relationship generates @JoinTable on the owning side</li>
 *   <li>BUG-003: Bidirectional relationship is properly detected (courses field)</li>
 * </ul>
 *
 * <p>Tag is the inverse side of the ManyToMany relationship with Course.
 * The generated entity will have {@code @ManyToMany(mappedBy = "tags")}.
 */
public class Tag {

    private final TagId id;
    private String name;
    private String description;

    /**
     * Courses with this tag - inverse side of @ManyToMany.
     * Generated entity will have mappedBy="tags" (BUG-003 fix).
     */
    private final List<Course> courses;

    public Tag(TagId id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.courses = new ArrayList<>();
    }

    public TagId id() {
        return id;
    }

    public TagId getId() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public List<Course> courses() {
        return Collections.unmodifiableList(courses);
    }

    public void rename(String name) {
        this.name = name;
    }

    void addCourse(Course course) {
        if (!courses.contains(course)) {
            courses.add(course);
        }
    }

    void removeCourse(Course course) {
        courses.remove(course);
    }
}
