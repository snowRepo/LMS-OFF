package com.lms.ui;

import com.lms.auth.AuthService;
import com.lms.model.User;
import com.lms.util.Navigator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * The primary application shell after login/registration.
 */
public class MainDashboardView {

    private final User currentUser;
    private final java.util.List<Button> navButtons = new java.util.ArrayList<>();

    public MainDashboardView(User currentUser) {
        this.currentUser = currentUser;
    }

    public Scene build() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f0f0f2;");

        // ── Sidebar (Left) ───────────────────────────────────────────────────
        VBox sidebar = new VBox(16);
        sidebar.setPrefWidth(240);
        sidebar.setPadding(new Insets(20, 16, 20, 16));
        sidebar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e4e4e7; -fx-border-width: 0 1 0 0;");

        // Branding
        VBox branding = new VBox(4);
        branding.setAlignment(Pos.CENTER);
        Label lblBrandTitle = new Label("LMS");
        lblBrandTitle.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #039ED3;");
        Label lblBrandSub = new Label("Library Management System");
        lblBrandSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #71717a;");
        branding.getChildren().addAll(lblBrandTitle, lblBrandSub);

        javafx.scene.control.Separator sep1 = new javafx.scene.control.Separator();
        sep1.setStyle("-fx-opacity: 0.2;");

        // Navigation Links
        VBox navBox = new VBox(4);
        VBox.setVgrow(navBox, javafx.scene.layout.Priority.ALWAYS);

        Button btnDash = createNavButton("📊 Dashboard");
        Button btnCatalog = createNavButton("📚 Catalog");
        Button btnCategories = createNavButton("🏷 Categories");
        Button btnMembers = createNavButton("👥 Members");
        Button btnAttendance = createNavButton("📅 Attendance");
        Button btnCirculation = createNavButton("🎫 Circulation");
        
        Button btnStaff = null;
        if ("ADMIN".equals(currentUser.role())) {
            btnStaff = createNavButton("👤 Staff");
        }
        Button btnActivityLogs = createNavButton("📋 Activity Logs");
        Button btnReports = createNavButton("📈 Reports");
        
        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Button btnSettings = createNavButton("⚙ Settings");

        navBox.getChildren().addAll(
            btnDash, btnCatalog, btnCategories, btnMembers, btnAttendance, btnCirculation
        );
        
        if (btnStaff != null) {
            navBox.getChildren().add(btnStaff);
        }
        
        navBox.getChildren().addAll(btnActivityLogs, btnReports, spacer, btnSettings);

        javafx.scene.control.Separator sep2 = new javafx.scene.control.Separator();
        sep2.setStyle("-fx-opacity: 0.2;");

        // User Info & Sync Status
        VBox userBox = new VBox(8);
        
        VBox nameRoleBox = new VBox(2);
        Label lblUserName = new Label(currentUser.fullName());
        lblUserName.setStyle("-fx-text-fill: #18181b; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label lblRole = new Label(currentUser.role());
        lblRole.setStyle("-fx-text-fill: #039ED3; -fx-font-size: 11px; -fx-font-weight: bold;");
        nameRoleBox.getChildren().addAll(lblUserName, lblRole);
        
        Label lblSyncStatus = new Label("● Offline");
        lblSyncStatus.setStyle("-fx-text-fill: #71717a; -fx-font-size: 11px;");
        
        HBox bottomActions = new HBox(8);
        Button btnSync = new Button("Sync Now");
        btnSync.setMaxWidth(Double.MAX_VALUE);
        btnSync.setStyle("-fx-font-size: 11px;");
        HBox.setHgrow(btnSync, javafx.scene.layout.Priority.ALWAYS);
        
        btnSync.setOnAction(e -> {
            btnSync.setText("Syncing...");
            btnSync.setDisable(true);
            new com.lms.sync.SyncManager().sync(
                () -> {
                    btnSync.setText("Sync Now");
                    btnSync.setDisable(false);
                    lblSyncStatus.setText("● Synced " + java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(java.time.LocalTime.now()));
                    lblSyncStatus.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px;");
                    com.lms.util.ToastUtil.show("Cloud sync complete!");
                },
                () -> {
                    btnSync.setText("Sync Now");
                    btnSync.setDisable(false);
                    lblSyncStatus.setText("● Sync Failed");
                    lblSyncStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
                    com.lms.util.ToastUtil.show("Cloud sync failed.");
                }
            );
        });
        
        Button btnLogout = new Button("Logout");
        btnLogout.setMaxWidth(Double.MAX_VALUE);
        btnLogout.setStyle("-fx-font-size: 11px; -fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold;");
        HBox.setHgrow(btnLogout, javafx.scene.layout.Priority.ALWAYS);
        
        btnLogout.setOnAction(e -> {
            com.lms.db.ActivityLogDAO.log("LOGOUT", "User logged out");
            com.lms.auth.SessionManager.clearSession();
            AuthService authService = new AuthService();
            com.lms.auth.LoginView loginView = new com.lms.auth.LoginView(authService);
            Navigator.show(loginView.build(), "Login");
            com.lms.util.ToastUtil.show("Logged out successfully");
        });
        
        bottomActions.getChildren().addAll(btnSync, btnLogout);
        userBox.getChildren().addAll(nameRoleBox, lblSyncStatus, bottomActions);

        sidebar.getChildren().addAll(branding, sep1, navBox, sep2, userBox);

        // ── Main Content Area (Center) ───────────────────────────────────────
        javafx.scene.layout.StackPane contentPane = new javafx.scene.layout.StackPane();
        contentPane.setStyle("-fx-background-color: #f0f0f2;");
        
        // Load the default Dashboard Metrics view
        contentPane.getChildren().add(new DashboardMetricsView().build());

        // Wire up routing with active state highlighting
        btnDash.setOnAction(e -> routeTo(btnDash, contentPane, new DashboardMetricsView().build()));
        btnCatalog.setOnAction(e -> routeTo(btnCatalog, contentPane, new CatalogView().build()));
        btnCategories.setOnAction(e -> routeTo(btnCategories, contentPane, new CategoryView().build()));
        btnMembers.setOnAction(e -> routeTo(btnMembers, contentPane, new MemberView().build()));
        btnAttendance.setOnAction(e -> routeTo(btnAttendance, contentPane, new AttendanceView().build()));
        btnCirculation.setOnAction(e -> routeTo(btnCirculation, contentPane, new CirculationView().build()));
        if (btnStaff != null) {
            Button finalBtnStaff = btnStaff; // effectively final for lambda
            btnStaff.setOnAction(e -> routeTo(finalBtnStaff, contentPane, new StaffView().build()));
        }
        btnActivityLogs.setOnAction(e -> {
            com.lms.ui.ActivityLogView view = new com.lms.ui.ActivityLogView();
            routeTo(btnActivityLogs, contentPane, view.build());
        });
        btnReports.setOnAction(e -> {
            com.lms.ui.ReportsView view = new com.lms.ui.ReportsView();
            routeTo(btnReports, contentPane, view.build());
        });
        btnSettings.setOnAction(e -> routeTo(btnSettings, contentPane, new SettingsView().build()));

        // Removed redundant block

        root.setLeft(sidebar);
        root.setCenter(contentPane);

        // Set default active
        setActiveNavButton(btnDash);

        return new Scene(root, 900, 600);
    }

    private void routeTo(Button btn, javafx.scene.layout.StackPane contentPane, javafx.scene.Node content) {
        setActiveNavButton(btn);
        contentPane.getChildren().setAll(content);
        // Extract plain text from button by removing emojis/icons if necessary, or just use as is
        String title = btn.getText();
        if (title.contains(" ")) {
            title = title.substring(title.indexOf(" ") + 1);
        }
        com.lms.util.Navigator.setTitle(title);
    }

    private void setActiveNavButton(Button activeBtn) {
        for (Button btn : navButtons) {
            btn.setStyle("");
            // Prevent inactive buttons from stealing focus when dialogs close
            btn.setFocusTraversable(false);
        }
        if (activeBtn != null) {
            activeBtn.setFocusTraversable(true);
            javafx.application.Platform.runLater(activeBtn::requestFocus);
        }
    }

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.BASELINE_LEFT);
        navButtons.add(btn);
        return btn;
    }
}
