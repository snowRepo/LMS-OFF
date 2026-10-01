package com.lms;

import com.lms.auth.AuthService;
import com.lms.auth.LoginView;
import com.lms.auth.RegisterView;
import com.lms.db.DatabaseManager;
import com.lms.util.Navigator;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main JavaFX application entry point.
 *
 * Startup logic:
 *  1. Initialize SQLite database & schema
 *  2. If no users exist → show Register screen (first launch)
 *  3. Otherwise          → show Login screen
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialize DB (creates file + schema if needed)
        DatabaseManager.getInstance();

        // Set up navigator
        Navigator.init(primaryStage);
        primaryStage.setTitle("LMS");
        
        // Set cross-platform taskbar/dock icon during development
        try {
            java.net.URL iconUrl = App.class.getResource("/icons/icon.png");
            if (iconUrl != null) {
                String osName = System.getProperty("os.name").toLowerCase();
                
                // For Windows/Linux title bar & taskbar (macOS doesn't use title bar icons)
                if (!osName.contains("mac")) {
                    primaryStage.getIcons().add(new javafx.scene.image.Image(iconUrl.toExternalForm()));
                }
                
                // For macOS Dock (requires Java 9+)
                if (osName.contains("mac") && java.awt.Taskbar.isTaskbarSupported()) {
                    java.net.URL macIconUrl = App.class.getResource("/icons/icon_macos.png");
                    if (macIconUrl != null) {
                        java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
                        if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(macIconUrl);
                            taskbar.setIconImage(img);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to set app icon: " + e.getMessage());
        }

        // Window is now resizable

        AuthService authService = new AuthService();

        // If no users exist, it's a fresh install -> Show Welcome (Setup Wizard)
        // Otherwise, show Login
        if (authService.countUsers() == 0) {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(App.class);
            boolean eulaAgreed = prefs.getBoolean("setup_eula_agreed", false);
            
            if (eulaAgreed) {
                com.lms.auth.setup.DbSetupView dbSetupView = new com.lms.auth.setup.DbSetupView(authService);
                Navigator.show(dbSetupView.build(), "Database Setup");
            } else {
                com.lms.auth.setup.WelcomeView welcomeView = new com.lms.auth.setup.WelcomeView(authService);
                Navigator.show(welcomeView.build(), "Welcome");
            }
        } else {
            LoginView loginView = new LoginView(authService);
            Navigator.show(loginView.build(), "Login");
        }

        primaryStage.show();
        // Force window to front on macOS (needed when launched from terminal)
        primaryStage.toFront();
        primaryStage.requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
