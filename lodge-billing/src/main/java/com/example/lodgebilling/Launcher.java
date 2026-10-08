package com.example.lodgebilling;

// This class deliberately does NOT extend Application. Java can start a plain class,
// which then hands over to JavaFX. This avoids "JavaFX runtime components are missing".
public class Launcher {
    public static void main(String[] args) {
        App.main(args);     // calls App's main(), which starts the JavaFX window
    }
}
