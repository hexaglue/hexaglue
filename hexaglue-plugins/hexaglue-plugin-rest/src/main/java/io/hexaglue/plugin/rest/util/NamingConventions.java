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

package io.hexaglue.plugin.rest.util;

/**
 * Naming convention utilities for REST code generation.
 *
 * <p>Provides consistent naming transformations for REST endpoints:
 * kebab-case paths, pluralization, suffix stripping, and capitalization.
 *
 * @since 3.1.0
 */
public final class NamingConventions {

    private static final String[] KNOWN_SUFFIXES = {"UseCases", "Service", "Port", "UseCase"};

    private NamingConventions() {
        /* prevent instantiation */
    }

    /**
     * Converts a camelCase or PascalCase string to kebab-case.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "AccountUseCases"} &rarr; {@code "account-use-cases"}</li>
     *   <li>{@code "Account"} &rarr; {@code "account"}</li>
     *   <li>{@code "HTTPRequest"} &rarr; {@code "http-request"}</li>
     * </ul>
     *
     * @param input the camelCase or PascalCase string
     * @return the kebab-case string
     * @throws IllegalArgumentException if input is null or empty
     */
    public static String toKebabCase(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Input string cannot be null or empty");
        }
        return input.replaceAll("([a-z])([A-Z])", "$1-$2")
                .replaceAll("([A-Z])([A-Z][a-z])", "$1-$2")
                .toLowerCase();
    }

    /**
     * Naive English pluralization.
     *
     * <p>Rules:
     * <ul>
     *   <li>Words ending in s, sh, ch, x, z &rarr; add "es"</li>
     *   <li>Words ending in consonant+y &rarr; replace y with "ies"</li>
     *   <li>Otherwise &rarr; add "s"</li>
     * </ul>
     *
     * @param word the singular word
     * @return the pluralized word
     * @throws IllegalArgumentException if word is null or empty
     */
    public static String pluralize(String word) {
        if (word == null || word.isEmpty()) {
            throw new IllegalArgumentException("Word cannot be null or empty");
        }
        if (word.endsWith("s")
                || word.endsWith("sh")
                || word.endsWith("ch")
                || word.endsWith("x")
                || word.endsWith("z")) {
            return word + "es";
        }
        if (word.endsWith("y") && word.length() > 1 && !isVowel(word.charAt(word.length() - 2))) {
            return word.substring(0, word.length() - 1) + "ies";
        }
        return word + "s";
    }

    /**
     * Strips known port/service suffixes from a type name.
     *
     * <p>Strips the first matching suffix from: UseCases, Service, Port, UseCase.
     *
     * @param typeName the type name (e.g., "AccountUseCases")
     * @return the stripped name (e.g., "Account")
     */
    public static String stripSuffix(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return typeName;
        }
        for (String suffix : KNOWN_SUFFIXES) {
            if (typeName.endsWith(suffix) && typeName.length() > suffix.length()) {
                return typeName.substring(0, typeName.length() - suffix.length());
            }
        }
        return typeName;
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param str the string to capitalize
     * @return the capitalized string
     * @throws IllegalArgumentException if str is null
     */
    public static String capitalize(String str) {
        if (str == null) {
            throw new IllegalArgumentException("String cannot be null");
        }
        if (str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Decapitalizes the first letter of a string.
     *
     * @param str the string to decapitalize
     * @return the decapitalized string
     * @throws IllegalArgumentException if str is null
     */
    public static String decapitalize(String str) {
        if (str == null) {
            throw new IllegalArgumentException("String cannot be null");
        }
        if (str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    private static boolean isVowel(char c) {
        return "aeiouAEIOU".indexOf(c) >= 0;
    }
}
