/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

package io.hexaglue.plugin.audit.adapter.report;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility class for automatic section numbering in Markdown documents.
 *
 * <p>This class provides hierarchical section numbering for Markdown headings,
 * supporting levels from h2 (##) to h6 (######). The h1 level is reserved for
 * the document title.
 *
 * <p>Example usage:
 * <pre>{@code
 * SectionNumbering numbering = SectionNumbering.create();
 *
 * numbering.h2("Executive Summary");  // Returns "## 1. Executive Summary"
 * numbering.h3("Verdict");            // Returns "### 1.1. Verdict"
 * numbering.h3("Strengths");          // Returns "### 1.2. Strengths"
 * numbering.h2("Health Score");       // Returns "## 2. Health Score"
 * numbering.h3("Breakdown");          // Returns "### 2.1. Breakdown"
 * }</pre>
 *
 * @since 1.0.0
 */
public final class SectionNumbering {

    /**
     * Minimum supported heading level (h2 = ##).
     */
    private static final int MIN_LEVEL = 2;

    /**
     * Maximum supported heading level (h6 = ######).
     */
    private static final int MAX_LEVEL = 6;

    /**
     * Array of counters for each level (index 0 = h2, index 1 = h3, etc.).
     */
    private final int[] counters;

    /**
     * The current active level (0 = no section yet, 2-6 = heading level).
     */
    private int currentLevel;

    /**
     * Private constructor. Use {@link #create()} factory method.
     */
    private SectionNumbering() {
        this.counters = new int[MAX_LEVEL - MIN_LEVEL + 1]; // 5 levels: h2-h6
        this.currentLevel = 0;
    }

    /**
     * Creates a new section numbering instance.
     *
     * @return a new section numbering instance
     */
    public static SectionNumbering create() {
        return new SectionNumbering();
    }

    /**
     * Generates a numbered heading at the specified level.
     *
     * <p>When a higher-level heading is created (e.g., h2 after h3),
     * all lower-level counters are reset. When a same or lower level
     * heading is created, the counter for that level is incremented.
     *
     * @param level the heading level (2-6)
     * @param title the heading title
     * @return the formatted Markdown heading with number
     * @throws IllegalArgumentException if level is out of range
     */
    public String heading(int level, String title) {
        validateLevel(level);

        int index = levelToIndex(level);

        // If going to a higher level (e.g., from h3 to h2), reset lower counters
        if (level <= currentLevel) {
            resetCountersBelow(index);
        }

        // Increment the counter for this level
        counters[index]++;

        // Update current level
        currentLevel = level;

        // Build the heading
        String prefix = "#".repeat(level);
        String number = buildNumber(index);

        return prefix + " " + number + ". " + title;
    }

    /**
     * Generates an h2 heading (##).
     *
     * @param title the heading title
     * @return the formatted Markdown heading
     */
    public String h2(String title) {
        return heading(2, title);
    }

    /**
     * Generates an h3 heading (###).
     *
     * @param title the heading title
     * @return the formatted Markdown heading
     */
    public String h3(String title) {
        return heading(3, title);
    }

    /**
     * Generates an h4 heading (####).
     *
     * @param title the heading title
     * @return the formatted Markdown heading
     */
    public String h4(String title) {
        return heading(4, title);
    }

    /**
     * Generates an h5 heading (#####).
     *
     * @param title the heading title
     * @return the formatted Markdown heading
     */
    public String h5(String title) {
        return heading(5, title);
    }

    /**
     * Generates an h6 heading (######).
     *
     * @param title the heading title
     * @return the formatted Markdown heading
     */
    public String h6(String title) {
        return heading(6, title);
    }

    /**
     * Returns the current section number as a string (e.g., "1.2.3").
     *
     * @return the current section number, or empty string if no section started
     */
    public String currentNumber() {
        if (currentLevel == 0) {
            return "";
        }
        return buildNumber(levelToIndex(currentLevel));
    }

    /**
     * Resets all counters to start fresh.
     */
    public void reset() {
        for (int i = 0; i < counters.length; i++) {
            counters[i] = 0;
        }
        currentLevel = 0;
    }

    /**
     * Validates that the level is within the supported range.
     */
    private void validateLevel(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException(
                    "Heading level must be between " + MIN_LEVEL + " and " + MAX_LEVEL + ", got: " + level);
        }
    }

    /**
     * Converts a heading level to an array index.
     */
    private int levelToIndex(int level) {
        return level - MIN_LEVEL;
    }

    /**
     * Resets all counters below (and including) the given index.
     */
    private void resetCountersBelow(int index) {
        for (int i = index + 1; i < counters.length; i++) {
            counters[i] = 0;
        }
    }

    /**
     * Builds the hierarchical number string up to the given index.
     */
    private String buildNumber(int index) {
        return IntStream.rangeClosed(0, index)
                .filter(i -> counters[i] > 0 || i == index)
                .mapToObj(i -> String.valueOf(counters[i]))
                .collect(Collectors.joining("."));
    }
}
