package com.lms.auth;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import com.lms.util.Navigator;
import com.lms.model.User;

import java.util.Optional;

/**
 * Login screen — shown after initial admin setup or on every subsequent launch.
 * Pure JavaFX Modena, no external CSS.
 */
public class LoginView {

    private final AuthService authService;

    private TextField     tfUsername;
    private PasswordField pfPassword;
    private Label         lblStatus;
    private Button        btnLogin;

    public LoginView(AuthService authService) {
        this.authService = authService;
    }

    public Scene build() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #ffffff;");

        // ── Center Content ───────────────────────────────────────────────
        VBox center = new VBox(32);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(30, 0, 0, 0));

        // Header
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        
        Label lblTitle = new Label("LMS");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #18181b;");
        
        Label lblSubtitle = new Label("Library Management System");
        lblSubtitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
        
        header.getChildren().addAll(lblTitle, lblSubtitle);

        // Form
        VBox form = new VBox(14);
        form.setMaxWidth(300);

        tfUsername = new TextField();
        tfUsername.setPromptText("Username");
        
        pfPassword = new PasswordField();
        pfPassword.setPromptText("Password");
        
        form.getChildren().addAll(
            createFieldBox("Username:", tfUsername),
            createFieldBox("Password:", pfPassword)
        );

        // Status
        lblStatus = new Label("");
        lblStatus.setStyle("-fx-text-fill: #cc0000;");
        lblStatus.setWrapText(true);
        lblStatus.setManaged(false);
        lblStatus.setVisible(false);
        form.getChildren().add(lblStatus);

        // Buttons
        VBox buttonBox = new VBox(8);
        
        btnLogin = new Button("Log In");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setDefaultButton(true);
        btnLogin.setOnAction(e -> handleLogin());

        Button btnForgot = new Button("Forgot Password?");
        btnForgot.setMaxWidth(Double.MAX_VALUE);
        btnForgot.setOnAction(e -> {
            ForgotPasswordView forgotView = new ForgotPasswordView(authService);
            Navigator.show(forgotView.build(), "Password Recovery");
        });

        buttonBox.getChildren().addAll(btnLogin, btnForgot);
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

    private VBox createFieldBox(String labelText, Control field) {
        Label lbl = new Label(labelText);
        lbl.setFont(Font.font("System", FontWeight.SEMI_BOLD, 12));
        lbl.setStyle("-fx-text-fill: #333333;");
        
        VBox box = new VBox(4, lbl, field);
        box.setAlignment(Pos.TOP_LEFT);
        return box;
    }

    private void handleLogin() {
        lblStatus.setText("");
        lblStatus.setVisible(false);
        lblStatus.setManaged(false);

        String username = tfUsername.getText().trim();
        String password = pfPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            lblStatus.setText("Username and password are required.");
            lblStatus.setVisible(true);
            lblStatus.setManaged(true);
            return;
        }

        btnLogin.setDisable(true);
        Optional<User> result = authService.login(username, password);

        if (result.isPresent()) {
            User user = result.get();
            if (user.mustChangePassword()) {
                com.lms.ui.ForcePasswordChangeView forceView = new com.lms.ui.ForcePasswordChangeView(authService, user);
                Navigator.show(forceView.build(), "Security Setup");
            } else {
                com.lms.ui.MainDashboardView dashboard = new com.lms.ui.MainDashboardView(user);
                Navigator.show(dashboard.build(), "Dashboard");
                com.lms.util.ToastUtil.show("Welcome back, " + user.fullName() + "!");
            }
        } else {
            btnLogin.setDisable(false);
            lblStatus.setStyle("-fx-text-fill: #cc0000;");
            lblStatus.setText("Invalid username or password.");
            lblStatus.setVisible(true);
            lblStatus.setManaged(true);
        }
    }
}
