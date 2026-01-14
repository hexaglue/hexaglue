package com.example.validation.util;

/**
 * String utility methods.
 *
 * <p>Classification: EXCLUDED via hexaglue.yaml pattern.
 *
 * <p>This class is excluded from HexaGlue analysis because it matches
 * the exclude pattern in hexaglue.yaml:
 *
 * <pre>
 * classification:
 *   exclude:
 *     - "*.util.*"
 * </pre>
 *
 * <p>Utility classes, helper methods, and infrastructure code should typically
 * be excluded from domain analysis as they are not part of the domain model.
 */
public final class StringUtils {

    private StringUtils() {
        // Utility class
    }

    /**
     * Checks if a string is null or blank.
     *
     * @param str the string to check
     * @return true if null or blank
     */
    public static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    /**
     * Checks if a string is not null and not blank.
     *
     * @param str the string to check
     * @return true if not null and not blank
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * Truncates a string to the specified length.
     *
     * @param str the string to truncate
     * @param maxLength the maximum length
     * @return the truncated string
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}
