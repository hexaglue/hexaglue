package com.university.domain.course;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Course aggregate root.
 *
 * <p>Demonstrates multiple JPA bug fixes:
 * <ul>
 *   <li>BUG-001: @ManyToMany with @JoinTable generation (tags relationship)</li>
 *   <li>BUG-002: @ManyToOne with @JoinColumn (inverse via lessons)</li>
 *   <li>BUG-003: Bidirectional relationship detection (tags is owning side)</li>
 *   <li>BUG-006: Auditing fields when enableAuditing=true</li>
 *   <li>BUG-007: @Version field when enableOptimisticLocking=true</li>
 * </ul>
 */
public class Course {

    private final CourseId id;
    private String title;
    private String description;
    private int credits;
    private boolean active;

    /**
     * Tags for this course - owning side of @ManyToMany.
     *
     * <p>Demonstrates BUG-001 fix: Generated entity will have @JoinTable:
     * <pre>
     * {@code @ManyToMany}
     * {@code @JoinTable(
     *     name = "course_tag",
     *     joinColumns = @JoinColumn(name = "course_id"),
     *     inverseJoinColumns = @JoinColumn(name = "tag_id")
     * )}
     * private List<TagEntity> tags;
     * </pre>
     */
    private final List<Tag> tags;

    /**
     * Lessons in this course - @OneToMany relationship.
     *
     * <p>Demonstrates BUG-003 fix: Bidirectional relationship properly detected.
     */
    private final List<Lesson> lessons;

    public Course(CourseId id, String title, String description, int credits) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.credits = credits;
        this.active = true;
        this.tags = new ArrayList<>();
        this.lessons = new ArrayList<>();
    }

    public CourseId id() {
        return id;
    }

    public CourseId getId() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public int credits() {
        return credits;
    }

    public boolean active() {
        return active;
    }

    public List<Tag> tags() {
        return Collections.unmodifiableList(tags);
    }

    public List<Lesson> lessons() {
        return Collections.unmodifiableList(lessons);
    }

    public void updateInfo(String title, String description, int credits) {
        this.title = title;
        this.description = description;
        this.credits = credits;
    }

    public void addTag(Tag tag) {
        if (!tags.contains(tag)) {
            tags.add(tag);
            tag.addCourse(this);
        }
    }

    public void removeTag(Tag tag) {
        if (tags.remove(tag)) {
            tag.removeCourse(this);
        }
    }

    public Lesson addLesson(LessonId lessonId, String title, String content) {
        int orderIndex = lessons.size() + 1;
        Lesson lesson = new Lesson(lessonId, title, content, orderIndex, this);
        lessons.add(lesson);
        return lesson;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
