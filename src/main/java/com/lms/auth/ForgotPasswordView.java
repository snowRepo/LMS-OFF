package com.lms.auth;

import com.lms.util.Navigator;
import com.lms.util.ToastUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class ForgotPasswordView {

    private final AuthService authService;

    // State
    private String capturedUsername = "";

    // Steps
    private VBox step1, step2, step3;

    // Step 1 Fields
    private TextField usernameField;
    private Label step1Error;

    // Step 2 Fields
    private PasswordField hiddenPinField;
    private HBox pinRow;
    private Label step2Error;

    // Step 3 Fields
    private PasswordField newPassField;
    private PasswordField confirmField;
    private Label step3Error;

    public ForgotPasswordView(AuthService authService) {
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
        Label lblSubtitle = new Label("Password Recovery");
        lblSubtitle.setStyle("-fx-text-fill: #52525b; -fx-font-size: 14px;");
        header.getChildren().addAll(lblTitle, lblSubtitle);

        VBox formContainer = new VBox(14);
        formContainer.setMaxWidth(400);

        // --- STEP 1: Username ---
        step1 = new VBox(12);
        
        Label step1Desc = new Label("Enter your username to begin recovery.");
        step1Desc.setWrapText(true);
        step1Desc.setAlignment(Pos.CENTER);
        step1Desc.setMaxWidth(Double.MAX_VALUE);

        usernameField = new TextField();
        usernameField.setPromptText("Your username");
        VBox step1Input = createFieldBox("Username:", usernameField);

        step1Error = new Label("");
        step1Error.setStyle("-fx-text-fill: #dc2626;");
        step1Error.setManaged(false);
        step1Error.setVisible(false);

        Button btnNext1 = new Button("Next →");
        btnNext1.setMaxWidth(Double.MAX_VALUE);
        btnNext1.setDefaultButton(true);
        btnNext1.setOnAction(e -> handleStep1());

        step1.getChildren().addAll(step1Desc, step1Input, step1Error, btnNext1);


        // --- STEP 2: PIN ---
        step2 = new VBox(12);
        step2.setVisible(false);
        step2.setManaged(false);

        Label step2Desc = new Label("Enter your 6-digit security PIN.");
        step2Desc.setWrapText(true);
        step2Desc.setAlignment(Pos.CENTER);
        step2Desc.setMaxWidth(Double.MAX_VALUE);

        pinRow = createPinRow();

        step2Error = new Label("");
        step2Error.setStyle("-fx-text-fill: #dc2626;");
        step2Error.setManaged(false);
        step2Error.setVisible(false);

        Button btnVerifyPin = new Button("Verify PIN →");
        btnVerifyPin.setMaxWidth(Double.MAX_VALUE);
        btnVerifyPin.setDefaultButton(true);
        btnVerifyPin.setOnAction(e -> handleStep2());

        step2.getChildren().addAll(step2Desc, pinRow, step2Error, btnVerifyPin);


        // --- STEP 3: New Password ---
        step3 = new VBox(12);
        step3.setVisible(false);
        step3.setManaged(false);

        Label step3Desc = new Label("Enter your new password.");
        step3Desc.setWrapText(true);
        step3Desc.setAlignment(Pos.CENTER);
        step3Desc.setMaxWidth(Double.MAX_VALUE);

        newPassField = new PasswordField();
        newPassField.setPromptText("Min 6 characters");
        VBox step3Input1 = createFieldBox("New Password:", newPassField);

        confirmField = new PasswordField();
        confirmField.setPromptText("Re-enter password");
        VBox step3Input2 = createFieldBox("Confirm Password:", confirmField);

        step3Error = new Label("");
        step3Error.setStyle("-fx-text-fill: #dc2626;");
        step3Error.setManaged(false);
        step3Error.setVisible(false);

        Button btnReset = new Button("Reset Password");
        btnReset.setMaxWidth(Double.MAX_VALUE);
        btnReset.setDefaultButton(true);
        btnReset.setOnAction(e -> handleStep3());

        step3.getChildren().addAll(step3Desc, step3Input1, step3Input2, step3Error, btnReset);


        // --- Back to Login Button ---
        Button btnBack = new Button("← Back to Login");
        btnBack.setMaxWidth(Double.MAX_VALUE);
        btnBack.setOnAction(e -> {
            LoginView loginView = new LoginView(authService);
            Navigator.show(loginView.build(), "Login");
        });


        formContainer.getChildren().addAll(step1, step2, step3, btnBack);
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

    private VBox createFieldBox(String labelText, javafx.scene.Node fieldNode) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: #333333; -fx-font-size: 12px;");
        VBox box = new VBox(4, lbl, fieldNode);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox createPinRow() {
        HBox boxContainer = new HBox(10);
        boxContainer.setAlignment(Pos.CENTER);

        // Hidden field to capture actual typing
        hiddenPinField = new PasswordField();
        hiddenPinField.setMaxWidth(0);
        hiddenPinField.setMaxHeight(0);
        hiddenPinField.setOpacity(0);
        
        // Force numeric and max 6 chars
        hiddenPinField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                hiddenPinField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (hiddenPinField.getText().length() > 6) {
                hiddenPinField.setText(hiddenPinField.getText().substring(0, 6));
            }
        });

        // 6 visual boxes
        Label[] boxes = new Label[6];
        for (int i = 0; i < 6; i++) {
            Label box = new Label("");
            box.setPrefSize(40, 50);
            box.setAlignment(Pos.CENTER);
            box.setFont(Font.font("System", FontWeight.BOLD, 18));
            box.setStyle("-fx-border-color: #d1d5db; -fx-border-width: 2; -fx-border-radius: 4; -fx-background-radius: 4; -fx-background-color: #f9fafb;");
            boxes[i] = box;
            boxContainer.getChildren().add(box);
        }

        // Sync hidden field with visual boxes
        Runnable updateBoxes = () -> {
            String val = hiddenPinField.getText();
            int len = val.length();
            boolean focused = hiddenPinField.isFocused();
            for (int i = 0; i < 6; i++) {
                if (i < len) {
                    boxes[i].setText("•");
                } else {
                    boxes[i].setText("");
                }
                
                // Highlight the active box
                if (focused && (i == len || (len == 6 && i == 5))) {
                    boxes[i].setStyle("-fx-border-color: -fx-default-button; -fx-border-width: 2px; -fx-border-radius: 4; -fx-background-radius: 4; -fx-background-color: #ffffff; -fx-effect: dropshadow(three-pass-box, -fx-default-button, 4, 0, 0, 0);");
                } else {
                    boxes[i].setStyle("-fx-border-color: #d1d5db; -fx-border-width: 2px; -fx-border-radius: 4; -fx-background-radius: 4; -fx-background-color: #f9fafb;");
                }
            }
        };

        hiddenPinField.textProperty().addListener((obs, oldVal, newVal) -> updateBoxes.run());
        hiddenPinField.focusedProperty().addListener((obs, oldVal, newVal) -> updateBoxes.run());

        boxContainer.setOnMouseClicked(e -> hiddenPinField.requestFocus());
        
        // Wrap in a StackPane to hold both the HBox and the hidden field
        javafx.scene.layout.StackPane stack = new javafx.scene.layout.StackPane(boxContainer, hiddenPinField);
        stack.setAlignment(Pos.CENTER);
        
        HBox row = new HBox(stack);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private void handleStep1() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            showError(step1Error, "Please enter your username.");
            return;
        }

        if (authService.checkUsernameExists(username)) {
            capturedUsername = username;
            step1Error.setVisible(false);
            step1Error.setManaged(false);
            
            // Switch to Step 2
            step1.setVisible(false);
            step1.setManaged(false);
            step2.setVisible(true);
            step2.setManaged(true);
            
            // Focus PIN
            hiddenPinField.requestFocus();
        } else {
            showError(step1Error, "Username not found in the system.");
        }
    }

    private void handleStep2() {
        String pin = hiddenPinField.getText();
        if (pin.length() != 6) {
            showError(step2Error, "PIN must be exactly 6 digits.");
            return;
        }

        if (authService.verifyPin(capturedUsername, pin)) {
            step2Error.setVisible(false);
            step2Error.setManaged(false);

            // Switch to Step 3
            step2.setVisible(false);
            step2.setManaged(false);
            step3.setVisible(true);
            step3.setManaged(true);
        } else {
            showError(step2Error, "Incorrect PIN.");
            hiddenPinField.clear(); // clear on fail
        }
    }

    private void handleStep3() {
        String newPass = newPassField.getText();
        String confirm = confirmField.getText();

        if (newPass.length() < 6) {
            showError(step3Error, "Password must be at least 6 characters.");
            return;
        }
        if (!newPass.equals(confirm)) {
            showError(step3Error, "Passwords do not match.");
            return;
        }

        authService.updatePassword(capturedUsername, newPass);
        
        ToastUtil.show("Password reset successfully. You can now login.");
        LoginView loginView = new LoginView(authService);
        Navigator.show(loginView.build(), "Login");
    }

    private void showError(Label errorLabel, String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
