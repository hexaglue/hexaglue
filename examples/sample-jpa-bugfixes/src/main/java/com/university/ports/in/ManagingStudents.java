package com.university.ports.in;

import com.university.domain.student.AchievementType;
import com.university.domain.student.Student;
import com.university.domain.student.StudentId;

import java.util.List;

/**
 * Use cases for managing students.
 */
public interface ManagingStudents {

    Student registerStudent(String firstName, String lastName, String email);

    Student findStudent(StudentId id);

    List<Student> listAllStudents();

    void awardAchievement(StudentId studentId, AchievementType achievement);

    void graduateStudent(StudentId id);
}
