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
 * Administrator registration — Step 2: PIN Setup.
 */
public class RegisterPinView {

    private final AuthService authService;
    private final String fullName;
    private final String username;
    private final String password;

    private PasswordField pfPin;
    private PasswordField pfConfirm;
    private Label         lblStatus;
    private Button        btnNext;

    public RegisterPinView(AuthService authService, String fullName, String username, String password) {
        this.authService = authService;
        this.fullName = fullName;
        this.username = username;
        this.password = password;
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
        Label lblTitle = new Label("Step 3 of 3 — Security PIN");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #18181b;");
        Label lblSubtitle = new Label("Set a 6-digit PIN for password recovery");
        lblSubtitle.setStyle("-fx-text-fill: #52525b; -fx-font-size: 14px;");
        header.getChildren().addAll(lblTitle, lblSubtitle);

        VBox form = new VBox(12);
        form.setMaxWidth(400);

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
        Label lblConfirm = new Label("CONFIRM PIN");
        lblConfirm.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        pin2Box.getChildren().addAll(lblConfirm, createPinWidget(pfConfirm));

        lblStatus = new Label("");
        lblStatus.setStyle("-fx-text-fill: #cc0000;");
        lblStatus.setWrapText(true);
        lblStatus.setVisible(false);
        lblStatus.setManaged(false);

        VBox buttonBox = new VBox(8);
        btnNext = new Button("Set PIN & Finish Setup");
        btnNext.setMaxWidth(Double.MAX_VALUE);
        btnNext.setDefaultButton(true);
        btnNext.setOnAction(e -> handleRegister());
        
        Button btnBack = new Button("← Back to Step 2");
        btnBack.setMaxWidth(Double.MAX_VALUE);
        btnBack.setOnAction(e -> {
            RegisterView regView = new RegisterView(authService);
            Navigator.show(regView.build(), "Administrator Setup");
        });

        buttonBox.getChildren().addAll(btnNext, btnBack);

        Label lblInfo = new Label("This PIN will be used to reset your password if you forget it.");
        lblInfo.setWrapText(true);
        lblInfo.setAlignment(Pos.CENTER);
        lblInfo.setStyle("-fx-text-fill: #6b6b6b; -fx-font-size: 13px;");

        form.getChildren().addAll(pin1Box, pin2Box, lblStatus, buttonBox, lblInfo);

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
                    // Highlight the currently active box
                    boxes[i].setStyle("-fx-border-color: -fx-default-button; -fx-border-width: 2px; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-background-color: #ffffff; -fx-font-size: 28px; -fx-text-fill: #333333; -fx-effect: dropshadow(three-pass-box, -fx-default-button, 4, 0, 0, 0);");
                } else {
                    boxes[i].setText("");
                    boxes[i].setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-background-color: #ffffff; -fx-font-size: 28px; -fx-text-fill: #333333;");
                }
            }
        };

        hiddenField.textProperty().addListener((obs, oldVal, newVal) -> updateBoxes.run());
        hiddenField.focusedProperty().addListener((obs, oldVal, newVal) -> updateBoxes.run());

        // Make the text field completely invisible but still intractable
        hiddenField.setStyle("-fx-background-color: transparent; -fx-text-fill: transparent; -fx-prompt-text-fill: transparent; -fx-border-color: transparent; -fx-highlight-fill: transparent; -fx-highlight-text-fill: transparent;");
        
        // When clicked anywhere on the boxes, focus the hidden field
        boxContainer.setOnMouseClicked(e -> hiddenField.requestFocus());

        StackPane stack = new StackPane(boxContainer, hiddenField);
        stack.setAlignment(Pos.CENTER);
        return stack;
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

    private VBox createFieldBox(String labelText, javafx.scene.Node fieldNode) {
        Label lbl = new Label(labelText);
        lbl.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
        lbl.setStyle("-fx-text-fill: #333333;");
        
        VBox box = new VBox(6, lbl, fieldNode);
        // Center the label and the PIN boxes
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private void handleRegister() {
        lblStatus.setText("");
        lblStatus.setVisible(false);
        lblStatus.setManaged(false);

        String pin = pfPin.getText();
        String confirm = pfConfirm.getText();

        if (pin.length() != 6) {
            showError("PIN must be exactly 6 digits.");
            return;
        }

        if (!pin.equals(confirm)) {
            showError("PINs do not match.");
            return;
        }

        btnNext.setDisable(true);
        boolean success = authService.register(fullName, username, password, pin, "ADMIN");

        if (success) {
            com.lms.util.ToastUtil.show("Administrator account created successfully! Please log in.");
            LoginView loginView = new LoginView(authService);
            Navigator.show(loginView.build(), "Login");
        } else {
            showError("Failed to register. Username might already exist.");
            btnNext.setDisable(false);
        }
    }

    private void showError(String msg) {
        lblStatus.setText(msg);
        lblStatus.setVisible(true);
        lblStatus.setManaged(true);
    }
}
