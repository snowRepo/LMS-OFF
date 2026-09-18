package com.lms.auth.setup;

import com.lms.auth.AuthService;
import com.lms.util.Navigator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class WelcomeView {

    private final AuthService authService;

    public WelcomeView(AuthService authService) {
        this.authService = authService;
    }

    public Scene build() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #ffffff;");

        // ── Center Content ───────────────────────────────────────────────
        VBox topCenterVBox = new VBox(30);
        topCenterVBox.setAlignment(Pos.TOP_CENTER);
        topCenterVBox.setPadding(new Insets(60, 0, 0, 0));

        VBox mainContent = new VBox(20);
        mainContent.setMaxWidth(600);
        mainContent.setAlignment(Pos.CENTER);
        mainContent.setPadding(new Insets(40));

        // Header
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        
        Label lblTitle = new Label("Welcome");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #18181b;");
        
        Label lblSubtitle = new Label("Library Management System");
        lblSubtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #52525b;");
        
        header.getChildren().addAll(lblTitle, lblSubtitle);

        // Body
        VBox body = new VBox(20);
        body.setAlignment(Pos.CENTER);
        body.setPadding(new Insets(20, 0, 20, 0));
        
        Label lblWelcomeMsg = new Label("Thank you for choosing LMS to manage your library operations.");
        lblWelcomeMsg.setWrapText(true);
        lblWelcomeMsg.setTextAlignment(TextAlignment.CENTER);
        lblWelcomeMsg.setStyle("-fx-font-size: 14px; -fx-text-fill: #18181b;");

        Label lblSetupDesc = new Label("This setup wizard will guide you through initializing your local and cloud databases, setting up your administrative account, and configuring your security preferences.");
        lblSetupDesc.setWrapText(true);
        lblSetupDesc.setTextAlignment(TextAlignment.CENTER);
        lblSetupDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #52525b;");

        Region spacer = new Region();
        spacer.setPrefHeight(20);

        Button btnContinue = new Button("Continue Setup →");
        btnContinue.setDefaultButton(true);
        // Make the button fit the design (in PMS, standard buttons might be styled via CSS, but let's give it a decent padding here, or rely on our global button styling)
        btnContinue.setPrefHeight(32);
        btnContinue.setPrefWidth(200);
        btnContinue.setOnAction(e -> {
            AgreementView agreementView = new AgreementView(authService);
            Navigator.show(agreementView.build(), "End User License Agreement");
        });

        body.getChildren().addAll(lblWelcomeMsg, lblSetupDesc, spacer, btnContinue);

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
}
