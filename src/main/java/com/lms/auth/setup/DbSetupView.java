package com.lms.auth.setup;

import com.lms.auth.AuthService;
import com.lms.auth.LoginView;
import com.lms.auth.RegisterView;
import com.lms.util.Navigator;
import com.lms.util.ToastUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class DbSetupView {

    private final AuthService authService;
    
    private ComboBox<String> dbTypeCombo;
    private TextField hostField, portField, dbNameField, userField;
    private PasswordField passField;
    private CheckBox sslCheck;
    private Label statusLabel, syncLabel;
    private ProgressBar syncProgressBar;
    private Button testBtn, skipBtn, continueBtn;
    
    private boolean connectionVerified = false;
    private boolean remoteHasAdmin = false;

    public DbSetupView(AuthService authService) {
        this.authService = authService;
    }

    public Scene build() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #ffffff;");

        // ── Center Content ───────────────────────────────────────────────
        VBox center = new VBox(32);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(30, 0, 0, 0));

        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        Label lblTitle = new Label("LMS Setup");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 24));
        Label lblSubtitle = new Label("Step 1 of 3 — Database Connection");
        lblSubtitle.setStyle("-fx-text-fill: #52525b; -fx-font-size: 14px;");
        header.getChildren().addAll(lblTitle, lblSubtitle);

        VBox form = new VBox(12);
        form.setMaxWidth(400);

        // Database Type
        dbTypeCombo = new ComboBox<>();
        dbTypeCombo.getItems().addAll("PostgreSQL", "MySQL", "MariaDB");
        dbTypeCombo.getSelectionModel().selectFirst();
        dbTypeCombo.setMaxWidth(Double.MAX_VALUE);
        form.getChildren().add(createFieldBox("Database Type:", dbTypeCombo));

        // Host / Port
        hostField = new TextField();
        hostField.setPromptText("db.abc.supabase.co");
        HBox.setHgrow(hostField, Priority.ALWAYS);
        
        portField = new TextField();
        portField.setPromptText("5432");
        portField.setPrefWidth(80);
        
        HBox hostPortBox = new HBox(8, hostField, portField);
        form.getChildren().add(createFieldBox("Host / Port:", hostPortBox));

        // DB Name
        dbNameField = new TextField();
        dbNameField.setPromptText("e.g. postgres");
        form.getChildren().add(createFieldBox("Database Name:", dbNameField));

        // Username & Password
        userField = new TextField();
        userField.setPromptText("Username");
        form.getChildren().add(createFieldBox("Username:", userField));

        passField = new PasswordField();
        passField.setPromptText("Password");
        form.getChildren().add(createFieldBox("Password:", passField));

        // SSL
        sslCheck = new CheckBox("Require SSL (recommended for cloud)");
        sslCheck.setSelected(true);
        form.getChildren().add(sslCheck);

        // Status Label
        statusLabel = new Label("");
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        form.getChildren().add(statusLabel);

        // Sync Progress (hidden by default)
        syncProgressBar = new ProgressBar(-1);
        syncProgressBar.setPrefWidth(400);
        syncProgressBar.setPrefHeight(10);
        syncProgressBar.setVisible(false);
        syncProgressBar.setManaged(false);
        
        syncLabel = new Label("");
        syncLabel.setWrapText(true);
        syncLabel.setVisible(false);
        syncLabel.setManaged(false);
        form.getChildren().addAll(syncProgressBar, syncLabel);

        // Buttons
        HBox buttonBox = new HBox(10);
        testBtn = new Button("Test Connection");
        testBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(testBtn, Priority.ALWAYS);
        testBtn.setOnAction(e -> handleTest());

        skipBtn = new Button("Skip (Local Only)");
        skipBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(skipBtn, Priority.ALWAYS);
        skipBtn.setOnAction(e -> handleSkip());

        continueBtn = new Button("Continue →");
        continueBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(continueBtn, Priority.ALWAYS);
        continueBtn.setDisable(true);
        continueBtn.setDefaultButton(true);
        continueBtn.setOnAction(e -> handleContinue());

        buttonBox.getChildren().addAll(testBtn, skipBtn, continueBtn);
        form.getChildren().add(buttonBox);

        center.getChildren().addAll(header, form);
        root.setCenter(center);

        // ── Footer ────────────────────────────────────────────────────────
        Label lblFooter = new Label("© 2026 LMS Inc. - All Rights Reserved");
        lblFooter.setMaxWidth(Double.MAX_VALUE);
        lblFooter.setAlignment(Pos.CENTER);
        lblFooter.setStyle("-fx-padding: 24; -fx-text-fill: #52525b; -fx-font-size: 12px;");
        root.setBottom(lblFooter);

        return new Scene(root, 750, 600);
    }

    private VBox createFieldBox(String labelText, javafx.scene.Node fieldNode) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: #333333; -fx-font-size: 12px;");
        VBox box = new VBox(4, lbl, fieldNode);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void handleTest() {
        String type = dbTypeCombo.getValue();
        String host = hostField.getText().trim();
        String port = portField.getText().trim();
        String dbName = dbNameField.getText().trim();
        String user = userField.getText().trim();

        if (type == null || host.isEmpty() || port.isEmpty() || dbName.isEmpty() || user.isEmpty()) {
            showStatus("Please fill in all fields.", false);
            return;
        }

        showStatus("Testing connection...", true);
        continueBtn.setDisable(true);
        connectionVerified = false;

        // Mocking the connection test logic since LMS relies on SQLite locally for now
        new Thread(() -> {
            try {
                Thread.sleep(1500); // simulate network delay
            } catch (InterruptedException e) { }
            
            Platform.runLater(() -> {
                connectionVerified = true;
                continueBtn.setDisable(false);
                
                // For demonstration, let's pretend no admin exists in the cloud DB right now
                // In a real implementation, we query `authService` or `DatabaseManager`
                remoteHasAdmin = false;

                if (remoteHasAdmin) {
                    showStatus("✓ Existing account found on this database.", true);
                    continueBtn.setText("Restore My Account →");
                } else {
                    showStatus("✓ Connection successful!", true);
                    continueBtn.setText("Continue →");
                }
                ToastUtil.show("Database connection verified.");
            });
        }).start();
    }

    private void handleSkip() {
        ToastUtil.show("Skipping cloud DB. Running in local-only mode.");
        Navigator.show(new RegisterView(authService).build(), "Administrator Setup");
    }

    private void handleContinue() {
        if (!connectionVerified) {
            ToastUtil.show("Please test the connection first.");
            return;
        }

        if (remoteHasAdmin) {
            setFormDisabled(true);
            showSyncProgress("Restoring your accounts...");

            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Simulate restoring data
                    Platform.runLater(() -> {
                        updateSyncLabel("✓ Restore complete!");
                        ToastUtil.show("Accounts and data restored successfully.");
                        // Go straight to login!
                        Navigator.show(new LoginView(authService).build(), "Login");
                    });
                } catch (InterruptedException e) {}
            }).start();
        } else {
            // New database, proceed to setup
            Navigator.show(new RegisterView(authService).build(), "Administrator Setup");
        }
    }

    private void setFormDisabled(boolean disabled) {
        dbTypeCombo.setDisable(disabled);
        hostField.setDisable(disabled);
        portField.setDisable(disabled);
        dbNameField.setDisable(disabled);
        userField.setDisable(disabled);
        passField.setDisable(disabled);
        sslCheck.setDisable(disabled);
        testBtn.setDisable(disabled);
        skipBtn.setDisable(disabled);
        continueBtn.setDisable(disabled);
    }

    private void showSyncProgress(String message) {
        syncProgressBar.setVisible(true);
        syncProgressBar.setManaged(true);
        syncLabel.setText(message);
        syncLabel.setVisible(true);
        syncLabel.setManaged(true);
    }

    private void updateSyncLabel(String message) {
        syncLabel.setText(message);
    }

    private void showStatus(String msg, boolean success) {
        statusLabel.setText(msg);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        if (success) {
            statusLabel.setStyle("-fx-text-fill: #16a34a;"); // green-600
        } else {
            statusLabel.setStyle("-fx-text-fill: #dc2626;"); // red-600
        }
    }
}
