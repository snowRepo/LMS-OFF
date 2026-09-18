package com.lms.auth.setup;

import com.lms.auth.AuthService;
import com.lms.util.Navigator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class AgreementView {

    private final AuthService authService;

    public AgreementView(AuthService authService) {
        this.authService = authService;
    }

    public Scene build() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #ffffff;");

        // ── Center Content ───────────────────────────────────────────────
        VBox topCenterVBox = new VBox(30);
        topCenterVBox.setAlignment(Pos.CENTER); // PMS FXML uses CENTER for EULA

        VBox mainContent = new VBox(20);
        mainContent.setMaxWidth(600);
        mainContent.setAlignment(Pos.CENTER);
        mainContent.setPadding(new Insets(40));

        // Header
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        
        Label lblTitle = new Label("End User License Agreement");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #18181b;");
        
        Label lblSubtitle = new Label("Please read and agree to the terms below to continue.");
        lblSubtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #52525b;");
        
        header.getChildren().addAll(lblTitle, lblSubtitle);

        // Body
        VBox body = new VBox(16);
        body.setPadding(new Insets(10, 0, 10, 0));

        // EULA Text Box
        VBox eulaContainer = new VBox();
        eulaContainer.setStyle("-fx-padding: 16; -fx-background-color: white;");
        
        Label eulaTextLabel = new Label(getEulaText());
        eulaTextLabel.setWrapText(true);
        eulaTextLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #3f3f46;");
        eulaContainer.getChildren().add(eulaTextLabel);

        ScrollPane scrollPane = new ScrollPane(eulaContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(280);
        scrollPane.setStyle("-fx-border-color: #d4d4d8; -fx-background-color: transparent;");

        // Checkbox
        CheckBox agreeCheckBox = new CheckBox("I have read and agree to the Terms and Conditions.");
        agreeCheckBox.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #18181b;");

        Region spacer = new Region();
        spacer.setPrefHeight(10);

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnDecline = new Button("Decline");
        btnDecline.setPrefHeight(32);
        btnDecline.setOnAction(e -> System.exit(0)); // Exit app if declined

        Button btnContinue = new Button("Agree & Continue →");
        btnContinue.setDefaultButton(true);
        btnContinue.setPrefHeight(32);
        btnContinue.setDisable(true);
        
        // Bind continue button to checkbox
        agreeCheckBox.selectedProperty().addListener((obs, oldV, newV) -> btnContinue.setDisable(!newV));

        btnContinue.setOnAction(e -> {
            // Save the agreement preference so we don't show this again
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(com.lms.App.class);
            prefs.putBoolean("setup_eula_agreed", true);

            // Next step is Database Setup
            DbSetupView dbSetupView = new DbSetupView(authService);
            Navigator.show(dbSetupView.build(), "Database Setup");
        });

        buttonBox.getChildren().addAll(btnDecline, btnContinue);

        body.getChildren().addAll(scrollPane, agreeCheckBox, spacer, buttonBox);

        mainContent.getChildren().addAll(header, body);
        topCenterVBox.getChildren().add(mainContent);
        
        root.setCenter(topCenterVBox);

        // ── Footer ────────────────────────────────────────────────────────
        Label lblFooter = new Label("© 2026 LMS Inc. - All Rights Reserved");
        lblFooter.setMaxWidth(Double.MAX_VALUE);
        lblFooter.setAlignment(Pos.CENTER);
        lblFooter.setStyle("-fx-padding: 24; -fx-text-fill: #52525b; -fx-font-size: 12px;");
        
        root.setBottom(lblFooter);

        return new Scene(root, 750, 600);
    }

    private String getEulaText() {
        return "END USER LICENSE AGREEMENT (EULA) FOR LMS\n\n"
             + "1. ACCEPTANCE OF TERMS\n"
             + "By installing, accessing, or using the Library Management System (LMS), you agree to be bound by the terms and conditions of this Agreement.\n\n"
             + "2. LICENSE GRANT\n"
             + "LMS grants you a non-exclusive, non-transferable, limited license to use the software for managing library operations.\n\n"
             + "3. DATA SYNC & PRIVACY\n"
             + "The software provides capabilities to synchronize your local database with a remote cloud database. You are solely responsible for ensuring the security and privacy of the credentials provided and the data synced.\n\n"
             + "4. TERMINATION\n"
             + "This Agreement is effective until terminated. You may terminate it at any time by uninstalling the software.\n\n"
             + "5. LIMITATION OF LIABILITY\n"
             + "In no event shall the developers of LMS be liable for any damages arising out of the use or inability to use the software.\n\n"
             + "By checking the box below, you signify your irrevocable acceptance of these terms.";
    }
}
