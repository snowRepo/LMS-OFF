package com.lms;

/**
 * Entry point wrapper — avoids JavaFX module launch issues
 * when running from a non-modular fat JAR or classpath.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
