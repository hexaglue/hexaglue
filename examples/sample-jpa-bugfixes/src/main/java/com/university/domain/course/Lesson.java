package com.university.domain.course;

import org.jmolecules.ddd.annotation.Entity;

/**
 * Lesson entity within a Course aggregate.
 *
 * <p>Demonstrates multiple JPA bug fixes:
 * <ul>
 *   <li>BUG-002: @ManyToOne relationship generates explicit @JoinColumn annotation</li>
 *   <li>BUG-008: Entity relationships use entity types, not domain types</li>
 * </ul>
 *
 * <p>The generated LessonEntity will have:
 * <pre>
 * {@code @ManyToOne}
 * {@code @JoinColumn(name = "course_id")}
 * private CourseEntity course;
 * </pre>
 *
 * <p>Note: The @Entity annotation from jMolecules is required for HexaGlue
 * to classify this type as an ENTITY (child entity within an aggregate).
 * Without this annotation, HexaGlue would classify it as UNCLASSIFIED.
 */
@Entity
public class Lesson {

    private final LessonId id;
    private String title;
    private String content;
    private int orderIndex;

    /**
     * Reference to parent Course - demonstrates BUG-002 fix.
     * Generated entity will have @JoinColumn(name = "course_id").
     */
    private final Course course;

    public Lesson(LessonId id, String title, String content, int orderIndex, Course course) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.orderIndex = orderIndex;
        this.course = course;
    }

    public LessonId id() {
        return id;
    }

    public LessonId getId() {
        return id;
    }

    public String title() {
        return title;
    }

    public String content() {
        return content;
    }

    public int orderIndex() {
        return orderIndex;
    }

    public Course course() {
        return course;
    }

    public void updateContent(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void reorder(int newIndex) {
        this.orderIndex = newIndex;
    }
}
