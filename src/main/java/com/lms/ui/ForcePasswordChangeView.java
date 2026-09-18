package com.lms.ui;

import com.lms.auth.AuthService;
import com.lms.model.User;
import com.lms.util.Navigator;
import com.lms.util.ToastUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class ForcePasswordChangeView {
    
    private final AuthService authService;
    private final User currentUser;

    public ForcePasswordChangeView(AuthService authService, User currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    public Scene build() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #f0f0f2;");

        VBox form = new VBox(15);
        form.setPadding(new Insets(30));
        form.setStyle("-fx-background-color: white; -fx-border-color: #e4e4e7; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        form.setMaxWidth(400);

        Label lblTitle = new Label("Welcome, " + currentUser.fullName() + "!");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #18181b;");
        
        Label lblSub = new Label("For security reasons, you must change your password and set a 6-digit PIN before accessing the system.");
        lblSub.setWrapText(true);
        lblSub.setStyle("-fx-text-fill: #71717a; -fx-font-size: 13px;");

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("New Password");

        PasswordField txtConfirm = new PasswordField();
        txtConfirm.setPromptText("Confirm New Password");

        PasswordField txtPin = new PasswordField();
        txtPin.setPromptText("6-Digit PIN");
        
        // ensure only digits
        txtPin.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*")) {
                txtPin.setText(val.replaceAll("[^\\d]", ""));
            }
            if (txtPin.getText().length() > 6) {
                txtPin.setText(txtPin.getText().substring(0, 6));
            }
        });

        Button btnSubmit = new Button("Secure Account & Login");
        btnSubmit.setMaxWidth(Double.MAX_VALUE);
        btnSubmit.setStyle("-fx-background-color: #039ED3; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10px;");
        
        btnSubmit.setOnAction(e -> {
            String pass = txtPass.getText();
            String conf = txtConfirm.getText();
            String pin = txtPin.getText();

            if (pass.isBlank() || conf.isBlank() || pin.isBlank()) {
                ToastUtil.show("Please fill all fields.");
                return;
            }
            if (!pass.equals(conf)) {
                ToastUtil.show("Passwords do not match.");
                return;
            }
            if (pin.length() != 6) {
                ToastUtil.show("PIN must be exactly 6 digits.");
                return;
            }

            try {
                authService.forceChangePasswordAndPin(currentUser.username(), pass, pin);
                ToastUtil.show("Account secured successfully!");
                // User logic expects `mustChangePassword` to be false now.
                User updatedUser = new User(currentUser.id(), currentUser.fullName(), currentUser.username(), currentUser.role(), false);
                Navigator.show(new MainDashboardView(updatedUser).build(), "Dashboard");
            } catch (Exception ex) {
                ToastUtil.show("Failed to update security settings.");
                ex.printStackTrace();
            }
        });
        
        Button btnLogout = new Button("Cancel & Logout");
        btnLogout.setMaxWidth(Double.MAX_VALUE);
        btnLogout.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
        btnLogout.setOnAction(e -> {
            Navigator.show(new com.lms.auth.LoginView(authService).build(), "Login");
        });

        form.getChildren().addAll(lblTitle, lblSub, txtPass, txtConfirm, txtPin, btnSubmit, btnLogout);
        root.getChildren().add(form);

        return new Scene(root, 900, 600);
    }
}
