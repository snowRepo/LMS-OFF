package com.lms.ui;

import com.lms.auth.AuthService;
import com.lms.model.User;
import com.lms.util.Navigator;
import com.lms.util.ToastUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ForcePasswordChangeView {
    
    private final AuthService authService;
    private final User currentUser;

    private VBox step1;
    private VBox step2;
    private PasswordField txtPass;
    private PasswordField txtConfirm;
    private PasswordField pfPin;
    private PasswordField pfConfirm;
    private Label step1Error;
    private Label step2Error;

    public ForcePasswordChangeView(AuthService authService, User currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
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
        
        Label lblSub = new Label("Welcome, " + currentUser.fullName() + "!");
        lblSub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
        
        header.getChildren().add(lblSub);

        // --- Step 1: Password ---
        step1 = new VBox(14);
        step1.setMaxWidth(300);
        
        Label lblStep1 = new Label("For security reasons, you must change your password and set a 6-digit PIN before accessing the system.");
        lblStep1.setWrapText(true);
        lblStep1.setStyle("-fx-text-fill: #52525b; -fx-font-size: 13px;");

        txtPass = new PasswordField();
        txtPass.setPromptText("New Password");

        txtConfirm = new PasswordField();
        txtConfirm.setPromptText("Confirm New Password");

        step1Error = new Label();
        step1Error.setStyle("-fx-text-fill: #cc0000;");
        step1Error.setWrapText(true);
        step1Error.setVisible(false);
        step1Error.setManaged(false);

        Button btnNext = new Button("Set Security PIN");
        btnNext.setMaxWidth(Double.MAX_VALUE);
        btnNext.setDefaultButton(true);
        btnNext.setOnAction(e -> handleStep1());
        
        Button btnLogout1 = new Button("Cancel & Logout");
        btnLogout1.setMaxWidth(Double.MAX_VALUE);
        btnLogout1.setOnAction(e -> Navigator.show(new com.lms.auth.LoginView(authService).build(), "Login"));
        
        step1.getChildren().addAll(lblStep1, txtPass, txtConfirm, step1Error, btnNext, btnLogout1);

        // --- Step 2: PIN ---
        step2 = new VBox(14);
        step2.setMaxWidth(400); 
        step2.setVisible(false);
        step2.setManaged(false);
        
        Label lblStep2 = new Label("Set a 6-digit PIN for password recovery.");
        lblStep2.setWrapText(true);
        lblStep2.setAlignment(Pos.CENTER);
        lblStep2.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        lblStep2.setMaxWidth(Double.MAX_VALUE);
        lblStep2.setStyle("-fx-text-fill: #52525b; -fx-font-size: 13px;");
        
        pfPin = new PasswordField();
        pfConfirm = new PasswordField();
        forceNumeric(pfPin);
        forceNumeric(pfConfirm);
        
        VBox pin1Box = new VBox(8);
        pin1Box.setAlignment(Pos.CENTER);
        Label lblEnter = new Label("ENTER PIN");
        lblEnter.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        pin1Box.getChildren().addAll(lblEnter, createPinWidget(pfPin));

        VBox pin2Box = new VBox(8);
        pin2Box.setAlignment(Pos.CENTER);
        Label lblConfirmPin = new Label("CONFIRM PIN");
        lblConfirmPin.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        pin2Box.getChildren().addAll(lblConfirmPin, createPinWidget(pfConfirm));

        step2Error = new Label();
        step2Error.setStyle("-fx-text-fill: #cc0000;");
        step2Error.setWrapText(true);
        step2Error.setVisible(false);
        step2Error.setManaged(false);

        Button btnSubmit = new Button("Secure Account & Login");
        btnSubmit.setMaxWidth(Double.MAX_VALUE);
        btnSubmit.setDefaultButton(true);
        btnSubmit.setOnAction(e -> handleStep2());
        
        Button btnBack = new Button("← Back");
        btnBack.setMaxWidth(Double.MAX_VALUE);
        btnBack.setOnAction(e -> {
            step2.setVisible(false);
            step2.setManaged(false);
            step1.setVisible(true);
            step1.setManaged(true);
        });
        
        VBox step2Buttons = new VBox(8);
        step2Buttons.setMaxWidth(300);
        step2Buttons.setAlignment(Pos.CENTER);
        step2Buttons.getChildren().addAll(btnSubmit, btnBack);
        
        VBox step2CenteredButtons = new VBox(step2Buttons);
        step2CenteredButtons.setAlignment(Pos.CENTER);

        step2.getChildren().addAll(lblStep2, pin1Box, pin2Box, step2Error, step2CenteredButtons);
        
        StackPane formContainer = new StackPane(step1, step2);
        formContainer.setAlignment(Pos.TOP_CENTER);
        
        center.getChildren().addAll(header, formContainer);
        root.setCenter(center);
        
        // ── Footer ────────────────────────────────────────────────────────
        Label lblFooter = new Label("© 2026 LMS Inc. - All Rights Reserved");
        lblFooter.setMaxWidth(Double.MAX_VALUE);
        lblFooter.setAlignment(Pos.CENTER);
        lblFooter.setStyle("-fx-padding: 24; -fx-text-fill: #52525b; -fx-font-size: 12px;");
        root.setBottom(lblFooter);

        return new Scene(root, 750, 600);
    }
    
    private void handleStep1() {
        String pass = txtPass.getText();
        String conf = txtConfirm.getText();

        if (pass.isBlank() || conf.isBlank()) {
            showError(step1Error, "Please fill all fields.");
            return;
        }
        if (pass.length() < 6) {
            showError(step1Error, "Password must be at least 6 characters.");
            return;
        }
        if (!pass.equals(conf)) {
            showError(step1Error, "Passwords do not match.");
            return;
        }
        
        step1Error.setVisible(false);
        step1Error.setManaged(false);
        
        step1.setVisible(false);
        step1.setManaged(false);
        step2.setVisible(true);
        step2.setManaged(true);
        
        pfPin.requestFocus();
    }
    
    private void handleStep2() {
        String pin = pfPin.getText();
        String confirm = pfConfirm.getText();
        
        if (pin.length() != 6) {
            showError(step2Error, "PIN must be exactly 6 digits.");
            return;
        }

        if (!pin.equals(confirm)) {
            showError(step2Error, "PINs do not match.");
            return;
        }
        
        try {
            authService.forceChangePasswordAndPin(currentUser.username(), txtPass.getText(), pin);
            ToastUtil.show("Account secured successfully!");
            // User logic expects `mustChangePassword` to be false now.
            User updatedUser = new User(currentUser.id(), currentUser.fullName(), currentUser.username(), currentUser.role(), false);
            Navigator.show(new MainDashboardView(updatedUser).build(), "Dashboard");
        } catch (Exception ex) {
            showError(step2Error, "Failed to update security settings.");
            ex.printStackTrace();
        }
    }
    
    private void showError(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void forceNumeric(PasswordField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                field.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (field.getText().length() > 6) {
                field.setText(field.getText().substring(0, 6));
            }
        });
    }

    private StackPane createPinWidget(PasswordField hiddenField) {
        HBox boxContainer = new HBox(10);
        boxContainer.setAlignment(Pos.CENTER);
        Label[] boxes = new Label[6];
        
        for (int i = 0; i < 6; i++) {
            Label box = new Label();
            box.setPrefSize(45, 55);
            box.setAlignment(Pos.CENTER);
            box.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-background-color: #ffffff; -fx-font-size: 28px; -fx-text-fill: #333333;");
            boxes[i] = box;
            boxContainer.getChildren().add(box);
        }
        
        Runnable updateBoxes = () -> {
            int len = hiddenField.getText().length();
            boolean focused = hiddenField.isFocused();
            for (int i = 0; i < 6; i++) {
                if (i < len) {
                    boxes[i].setText("•");
                    boxes[i].setStyle("-fx-border-color: -fx-default-button; -fx-border-width: 2px; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-background-color: #ffffff; -fx-font-size: 28px; -fx-text-fill: #333333; -fx-effect: dropshadow(three-pass-box, -fx-default-button, 4, 0, 0, 0);");
                } else if (i == len && focused && len < 6) {
                    boxes[i].setText("");
                    boxes[i].setStyle("-fx-border-color: -fx-default-button; -fx-border-width: 2px; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-background-color: #ffffff; -fx-font-size: 28px; -fx-text-fill: #333333; -fx-effect: dropshadow(three-pass-box, -fx-default-button, 4, 0, 0, 0);");
                } else {
                    boxes[i].setText("");
                    boxes[i].setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-background-color: #ffffff; -fx-font-size: 28px; -fx-text-fill: #333333;");
                }
            }
        };

        hiddenField.textProperty().addListener((obs, oldVal, newVal) -> updateBoxes.run());
        hiddenField.focusedProperty().addListener((obs, oldVal, newVal) -> updateBoxes.run());

        hiddenField.setStyle("-fx-background-color: transparent; -fx-text-fill: transparent; -fx-prompt-text-fill: transparent; -fx-border-color: transparent; -fx-highlight-fill: transparent; -fx-highlight-text-fill: transparent;");
        
        boxContainer.setOnMouseClicked(e -> hiddenField.requestFocus());

        StackPane stack = new StackPane(boxContainer, hiddenField);
        stack.setAlignment(Pos.CENTER);
        return stack;
    }
}
