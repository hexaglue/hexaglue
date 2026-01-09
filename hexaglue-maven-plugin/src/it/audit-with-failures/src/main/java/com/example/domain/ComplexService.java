package com.example.domain;

/**
 * A service with intentionally high complexity to trigger audit violations.
 */
public class ComplexService {

    /**
     * This method has high cyclomatic complexity (intentional for testing).
     */
    public String processData(String input, int mode, boolean flag) {
        if (input == null) {
            return "null";
        }

        if (mode == 1) {
            if (flag) {
                return input.toUpperCase();
            } else {
                return input.toLowerCase();
            }
        } else if (mode == 2) {
            if (flag) {
                return input.trim();
            } else {
                return input.strip();
            }
        } else if (mode == 3) {
            if (flag) {
                return input.replace(" ", "_");
            } else {
                return input.replace("_", " ");
            }
        } else {
            if (flag) {
                return input.substring(0, Math.min(10, input.length()));
            } else {
                return input + input;
            }
        }
    }

    /**
     * This method is intentionally long to trigger length violations.
     */
    public void longMethod() {
        System.out.println("Line 1");
        System.out.println("Line 2");
        System.out.println("Line 3");
        System.out.println("Line 4");
        System.out.println("Line 5");
        System.out.println("Line 6");
        System.out.println("Line 7");
        System.out.println("Line 8");
        System.out.println("Line 9");
        System.out.println("Line 10");
        System.out.println("Line 11");
        System.out.println("Line 12");
        System.out.println("Line 13");
        System.out.println("Line 14");
        System.out.println("Line 15");
        System.out.println("Line 16");
        System.out.println("Line 17");
        System.out.println("Line 18");
        System.out.println("Line 19");
        System.out.println("Line 20");
        System.out.println("Line 21");
        System.out.println("Line 22");
        System.out.println("Line 23");
        System.out.println("Line 24");
        System.out.println("Line 25");
    }
}
