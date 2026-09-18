package com.lms.auth;

import com.lms.util.Navigator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Initial administrator registration screen.
 * Shown ONLY on first launch when no users exist in the database.
 *
 * Offers two paths:
 * 1. Create a new local administrator account
 * 2. Connect to a Cloud Database to pull existing records
 */
public class RegisterView {

    private final AuthService authService;

    // Form fields
    private TextField     tfFullName;
    private TextField     tfUsername;
    private PasswordField pfPassword;
    private PasswordField pfConfirm;
    private Label         lblStatus;
    private Button        btnNext;

    public RegisterView(AuthService authService) {
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
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #18181b;");
        Label lblSubtitle = new Label("Step 2 of 3 — Create Admin Account");
        lblSubtitle.setStyle("-fx-text-fill: #52525b; -fx-font-size: 14px;");
        header.getChildren().addAll(lblTitle, lblSubtitle);

        VBox form = new VBox(12);
        form.setMaxWidth(400);

        tfFullName = new TextField();
        tfFullName.setPromptText("e.g. John Doe");
        
        tfUsername = new TextField();
        tfUsername.setPromptText("No spaces");
        
        pfPassword = new PasswordField();
        pfPassword.setPromptText("Min 6 characters");
        
        pfConfirm = new PasswordField();
        pfConfirm.setPromptText("Re-enter password");

        form.getChildren().addAll(
            createFieldBox("Full Name:", tfFullName),
            createFieldBox("Username:", tfUsername),
            createFieldBox("Password:", pfPassword),
            createFieldBox("Confirm Password:", pfConfirm)
        );

        lblStatus = new Label("");
        lblStatus.setStyle("-fx-text-fill: #cc0000;");
        lblStatus.setWrapText(true);
        lblStatus.setVisible(false);
        lblStatus.setManaged(false);
        form.getChildren().add(lblStatus);

        HBox buttonBox = new HBox(10);
        
        Button btnBack = new Button("← Back");
        btnBack.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnBack, Priority.ALWAYS);
        btnBack.setOnAction(e -> {
            com.lms.auth.setup.DbSetupView dbSetupView = new com.lms.auth.setup.DbSetupView(authService);
            Navigator.show(dbSetupView.build(), "Database Setup");
        });

        btnNext = new Button("Continue →");
        btnNext.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnNext, Priority.ALWAYS);
        btnNext.setDefaultButton(true);
        btnNext.setOnAction(e -> handleNext());
        
        buttonBox.getChildren().addAll(btnBack, btnNext);
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

    // -------------------------------------------------------------------------
    // Helper — creates a VBox with Label on top, Field on bottom
    // -------------------------------------------------------------------------
    private VBox createFieldBox(String labelText, Control field) {
        Label lbl = new Label(labelText);
        lbl.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
        lbl.setStyle("-fx-text-fill: #333333;");
        
        VBox box = new VBox(4, lbl, field);
        box.setAlignment(Pos.TOP_LEFT);
        return box;
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    private void handleNext() {
        lblStatus.setText("");
        lblStatus.setVisible(false);
        lblStatus.setManaged(false);

        String fullName = tfFullName.getText().trim();
        String username = tfUsername.getText().trim();
        String password = pfPassword.getText();
        String confirm  = pfConfirm.getText();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showError("All fields are required.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        // Pass collected credentials to Step 3: PIN Setup
        RegisterPinView pinView = new RegisterPinView(authService, fullName, username, password);
        Navigator.show(pinView.build(), "Security PIN");
    }

    private void showError(String msg) {
        lblStatus.setText(msg);
        lblStatus.setVisible(true);
        lblStatus.setManaged(true);
    }

    private void showSuccess(String msg) {
        lblStatus.setStyle("-fx-text-fill: #267326;");
        lblStatus.setText(msg);
    }

    private void clearStatus() {
        lblStatus.setText("");
    }
}
