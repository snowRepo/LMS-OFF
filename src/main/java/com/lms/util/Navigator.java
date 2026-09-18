package com.lms.util;

import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Simple scene navigator — swaps scenes on the primary stage.
 * All views call Navigator.show(...) to transition between screens.
 */
public class Navigator {

    private static Stage primaryStage;

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getStage() {
        return primaryStage;
    }

    public static void show(Scene scene) {
        applyStylesheet(scene);
        preserveStateAndSetScene(scene);
    }

    public static void show(Scene scene, String title) {
        primaryStage.setTitle(title);
        applyStylesheet(scene);
        preserveStateAndSetScene(scene);
    }

    private static void preserveStateAndSetScene(Scene scene) {
        Scene currentScene = primaryStage.getScene();
        
        if (currentScene != null) {
            // Detach the root from the new scene by giving it a dummy root
            javafx.scene.Parent root = scene.getRoot();
            scene.setRoot(new javafx.scene.layout.Region());
            
            // Now safely swap the root onto the active scene
            currentScene.setRoot(root);
        } else {
            // First time setup
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        }
    }

    private static void applyStylesheet(Scene scene) {
        if (scene.getStylesheets().isEmpty()) {
            java.net.URL url = Navigator.class.getResource("/style.css");
            if (url != null) {
                scene.getStylesheets().add(url.toExternalForm());
            }
        }
        
        // Global keyboard shortcut: Enter key triggers currently focused button
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                if (scene.getFocusOwner() instanceof javafx.scene.control.Button btn) {
                    btn.fire();
                    event.consume();
                }
            }
        });
    }

    public static void setTitle(String title) {
        primaryStage.setTitle(title);
    }
}
