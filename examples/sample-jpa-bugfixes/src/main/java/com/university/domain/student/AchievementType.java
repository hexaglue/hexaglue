package com.university.domain.student;

/**
 * Types of achievements a student can earn.
 *
 * <p>Used in a List to demonstrate BUG-004 fix:
 * @ElementCollection of enums now correctly generates @Enumerated(STRING).
 */
public enum AchievementType {

    HONOR_ROLL,
    PERFECT_ATTENDANCE,
    DEAN_LIST,
    SCHOLARSHIP,
    RESEARCH_AWARD,
    COMMUNITY_SERVICE
}
