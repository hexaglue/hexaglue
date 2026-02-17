package com.example.util;

/** String utility class - not a domain concept. */
public class StringHelper {
    public static String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
