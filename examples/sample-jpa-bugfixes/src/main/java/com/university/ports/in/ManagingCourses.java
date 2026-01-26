package com.university.ports.in;

import com.university.domain.course.Course;
import com.university.domain.course.CourseId;
import com.university.domain.course.Tag;
import com.university.domain.course.TagId;

import java.util.List;

/**
 * Use cases for managing courses and tags.
 */
public interface ManagingCourses {

    Course createCourse(String title, String description, int credits);

    Course findCourse(CourseId id);

    List<Course> listActiveCourses();

    void addLessonToCourse(CourseId courseId, String title, String content);

    Tag createTag(String name, String description);

    void tagCourse(CourseId courseId, TagId tagId);

    void untagCourse(CourseId courseId, TagId tagId);
}
