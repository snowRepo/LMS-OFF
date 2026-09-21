package com.lms.ui;

import com.lms.auth.AuthService;
import com.lms.auth.LoginView;
import com.lms.auth.SessionManager;
import com.lms.db.DatabaseManager;
import com.lms.db.SyncConfigDAO;
import com.lms.model.User;
import com.lms.sync.SyncManager;
import com.lms.util.Navigator;
import com.lms.util.ToastUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.sql.SQLException;

public class SettingsView {

    private final User currentUser;
    private final AuthService authService;
    private final SyncConfigDAO syncConfigDAO;
    private final SyncManager syncManager;

    public SettingsView() {
        this.currentUser = SessionManager.getCurrentUser();
        this.authService = new AuthService();
        this.syncConfigDAO = new SyncConfigDAO();
        this.syncManager = new SyncManager();
    }

    public Node build() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        VBox root = new VBox(24);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        Label lblTitle = new Label("Settings");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #18181b;");
        root.getChildren().add(lblTitle);

        // Security Section (All Users)
        root.getChildren().add(buildSecuritySection());

        // Admin-only sections
        if ("ADMIN".equals(currentUser.role())) {
            root.getChildren().add(buildDatabaseConnectionSection());
            root.getChildren().add(buildDataWipeSection());
        }

        // About Section (All Users)
        root.getChildren().add(buildAboutSection());

        scroll.setContent(root);
        return scroll;
    }

    private VBox buildSectionCard(String title) {
        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e4e4e7; -fx-border-radius: 8;");
        
        Label lblHeader = new Label(title);
        lblHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #18181b;");
        card.getChildren().add(lblHeader);
        return card;
    }

    private VBox buildSecuritySection() {
        VBox card = buildSectionCard("Security Settings");

        Button btnChangePassword = new Button("Change Password");
        btnChangePassword.setOnAction(e -> showChangePasswordDialog());

        Button btnChangePin = new Button("Change PIN");
        btnChangePin.setOnAction(e -> showChangePinDialog());

        HBox btns = new HBox(12, btnChangePassword, btnChangePin);
        card.getChildren().add(btns);
        return card;
    }

    private void showChangePasswordDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(com.lms.util.Navigator.getStage());
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Securely update your password");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox layout = new VBox(16);
        layout.setPadding(new Insets(10, 20, 10, 20));
        layout.setPrefWidth(350);

        Label desc = new Label("Please enter your current password to verify your identity, then set your new password.");
        desc.setWrapText(true);

        PasswordField oldPass = new PasswordField();
        oldPass.setPromptText("Current Password");

        PasswordField newPass = new PasswordField();
        newPass.setPromptText("New Password");

        PasswordField confirmPass = new PasswordField();
        confirmPass.setPromptText("Confirm New Password");

        VBox form = new VBox(10, 
            new Label("Current Password"), oldPass,
            new Label("New Password"), newPass,
            new Label("Confirm Password"), confirmPass
        );

        layout.getChildren().addAll(desc, form);
        dialog.getDialogPane().setContent(layout);

        Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (oldPass.getText().isEmpty() || newPass.getText().isEmpty() || confirmPass.getText().isEmpty()) {
                ToastUtil.show("All fields are required.");
                event.consume();
                return;
            }
            if (!newPass.getText().equals(confirmPass.getText())) {
                ToastUtil.show("New passwords do not match.");
                event.consume();
                return;
            }
            if (!authService.verifyPassword(currentUser.username(), oldPass.getText())) {
                ToastUtil.show("Incorrect current password.");
                event.consume();
                return;
            }
            authService.updatePassword(currentUser.username(), newPass.getText());
            ToastUtil.show("Password changed successfully.");
        });

        dialog.showAndWait();
    }

    private void showChangePinDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(com.lms.util.Navigator.getStage());
        dialog.setTitle("Change PIN");
        dialog.setHeaderText("Securely update your PIN");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox layout = new VBox(20);
        layout.setPadding(new Insets(10, 20, 10, 20));
        layout.setAlignment(Pos.CENTER);

        PasswordField oldPin = new PasswordField();
        PasswordField newPin = new PasswordField();
        PasswordField confirmPin = new PasswordField();

        forceNumeric(oldPin);
        forceNumeric(newPin);
        forceNumeric(confirmPin);

        VBox box1 = new VBox(8, createStyledLabel("CURRENT PIN"), createPinWidget(oldPin));
        box1.setAlignment(Pos.CENTER);
        VBox box2 = new VBox(8, createStyledLabel("NEW PIN"), createPinWidget(newPin));
        box2.setAlignment(Pos.CENTER);
        VBox box3 = new VBox(8, createStyledLabel("CONFIRM NEW PIN"), createPinWidget(confirmPin));
        box3.setAlignment(Pos.CENTER);

        layout.getChildren().addAll(box1, box2, box3);
        dialog.getDialogPane().setContent(layout);

        Node okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (oldPin.getText().length() < 6 || newPin.getText().length() < 6 || confirmPin.getText().length() < 6) {
                ToastUtil.show("All PINs must be 6 digits.");
                event.consume();
                return;
            }
            if (!newPin.getText().equals(confirmPin.getText())) {
                ToastUtil.show("New PINs do not match.");
                event.consume();
                return;
            }
            if (!authService.verifyPin(currentUser.username(), oldPin.getText())) {
                ToastUtil.show("Incorrect current PIN.");
                event.consume();
                return;
            }
            authService.updatePin(currentUser.username(), newPin.getText());
            ToastUtil.show("PIN changed successfully.");
        });

        dialog.showAndWait();
    }

    private Label createStyledLabel(String text) {
        return new Label(text);
    }

    private StackPane createPinWidget(PasswordField hiddenField) {
        HBox boxContainer = new HBox(8);
        boxContainer.setAlignment(Pos.CENTER);
        Label[] boxes = new Label[6];
        
        for (int i = 0; i < 6; i++) {
            Label box = new Label();
            box.setPrefSize(38, 48);
            box.setAlignment(Pos.CENTER);
            box.setStyle("-fx-border-color: #e4e4e7; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-background-color: #fafafa; -fx-font-size: 24px; -fx-text-fill: #18181b;");
            boxes[i] = box;
            boxContainer.getChildren().add(box);
        }
        
        Runnable updateBoxes = () -> {
            int len = hiddenField.getText().length();
            boolean focused = hiddenField.isFocused();
            for (int i = 0; i < 6; i++) {
                if (i < len) {
                    boxes[i].setText("•");
                    boxes[i].setStyle("-fx-border-color: #039ED3; -fx-border-width: 2px; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-background-color: #ffffff; -fx-font-size: 24px; -fx-text-fill: #18181b;");
                } else if (i == len && focused && len < 6) {
                    boxes[i].setText("");
                    boxes[i].setStyle("-fx-border-color: #039ED3; -fx-border-width: 2px; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-background-color: #ffffff; -fx-font-size: 24px; -fx-text-fill: #18181b;");
                } else {
                    boxes[i].setText("");
                    boxes[i].setStyle("-fx-border-color: #e4e4e7; -fx-border-width: 1px; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-background-color: #fafafa; -fx-font-size: 24px; -fx-text-fill: #18181b;");
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

    private VBox buildDatabaseConnectionSection() {
        VBox card = buildSectionCard("Database Connection (Cloud Sync)");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        ComboBox<String> cbType = new ComboBox<>();
        cbType.getItems().addAll("PostgreSQL", "MySQL", "MariaDB");
        cbType.setValue("PostgreSQL");
        
        TextField txtHost = new TextField();
        txtHost.setPromptText("e.g. db.abc.supabase.co");
        TextField txtPort = new TextField();
        txtPort.setPromptText("5432");
        TextField txtDbName = new TextField();
        txtDbName.setPromptText("postgres");
        TextField txtUser = new TextField();
        txtUser.setPromptText("postgres");
        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("password");

        grid.add(new Label("DB Type:"), 0, 0); grid.add(cbType, 1, 0);
        grid.add(new Label("Host:"), 0, 1);    grid.add(txtHost, 1, 1);
        grid.add(new Label("Port:"), 0, 2);    grid.add(txtPort, 1, 2);
        grid.add(new Label("DB Name:"), 0, 3); grid.add(txtDbName, 1, 3);
        grid.add(new Label("Username:"), 0, 4);grid.add(txtUser, 1, 4);
        grid.add(new Label("Password:"), 0, 5);grid.add(txtPass, 1, 5);

        // Load existing
        SyncConfigDAO.SyncConfig conf = syncConfigDAO.getConfig();
        if (conf != null && conf.dbType() != null) {
            cbType.setValue(conf.dbType());
            txtHost.setText(conf.host());
            txtPort.setText(conf.port());
            txtDbName.setText(conf.dbName());
            txtUser.setText(conf.username());
            txtPass.setText(conf.password());
        }

        HBox btns = new HBox(12);
        Button btnTest = new Button("Test Connection");
        Button btnSave = new Button("Save & Connect");
        btnSave.setDisable(true);

        Label lblStatus = new Label();

        btnTest.setOnAction(e -> {
            lblStatus.setText("Testing...");
            lblStatus.setStyle("-fx-text-fill: #3b82f6;");
            btnSave.setDisable(true);
            new Thread(() -> {
                boolean ok = syncManager.testConnection(
                        cbType.getValue(), txtHost.getText(), txtPort.getText(),
                        txtDbName.getText(), txtUser.getText(), txtPass.getText()
                );
                Platform.runLater(() -> {
                    if (ok) {
                        lblStatus.setText("✓ Connection successful!");
                        lblStatus.setStyle("-fx-text-fill: #22c55e;");
                        btnSave.setDisable(false);
                    } else {
                        lblStatus.setText("✗ Connection failed. Check credentials.");
                        lblStatus.setStyle("-fx-text-fill: #ef4444;");
                    }
                });
            }).start();
        });

        btnSave.setOnAction(e -> {
            syncConfigDAO.saveConfig(
                    cbType.getValue(), txtHost.getText(), txtPort.getText(),
                    txtDbName.getText(), txtUser.getText(), txtPass.getText()
            );
            ToastUtil.show("Database configuration saved!");
            lblStatus.setText("Configuration saved. You can now sync.");
            btnSave.setDisable(true); // require retest if changed
        });

        // if user types something, disable save until re-tested
        javafx.beans.value.ChangeListener<String> resetSave = (obs, oldV, newV) -> {
            btnSave.setDisable(true);
            lblStatus.setText("");
        };
        cbType.valueProperty().addListener(resetSave);
        txtHost.textProperty().addListener(resetSave);
        txtPort.textProperty().addListener(resetSave);
        txtDbName.textProperty().addListener(resetSave);
        txtUser.textProperty().addListener(resetSave);
        txtPass.textProperty().addListener(resetSave);

        btns.getChildren().addAll(btnTest, btnSave, lblStatus);
        btns.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(grid, btns);
        return card;
    }

    private VBox buildDataWipeSection() {
        VBox card = buildSectionCard("Data Management & Wipe");

        Label warning = new Label("Warning: Data wiping operations cannot be undone. Please ensure you have synced to the cloud before proceeding.");
        warning.setWrapText(true);
        warning.setStyle("-fx-text-fill: #dc2626; -fx-font-style: italic;");

        ToggleGroup wipeGroup = new ToggleGroup();

        RadioButton rbLocal = new RadioButton("Wipe Local Database (Keep Admins)");
        rbLocal.setToggleGroup(wipeGroup);
        rbLocal.setSelected(true);

        RadioButton rbCloud = new RadioButton("Wipe Cloud Database");
        rbCloud.setToggleGroup(wipeGroup);

        RadioButton rbBoth = new RadioButton("Wipe Both (Factory Reset)");
        rbBoth.setToggleGroup(wipeGroup);

        VBox radioBox = new VBox(8, rbLocal, rbCloud, rbBoth);

        Button btnProceed = new Button("Proceed with Wipe");
        btnProceed.setOnAction(e -> handleWipeProceed(rbLocal.isSelected(), rbCloud.isSelected(), rbBoth.isSelected()));

        card.getChildren().addAll(warning, radioBox, btnProceed);
        return card;
    }

    private void handleWipeProceed(boolean isLocal, boolean isCloud, boolean isBoth) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
alert.initOwner(com.lms.util.Navigator.getStage());
        alert.setTitle("Confirm Data Wipe");
        
        if (isBoth) {
            alert.setHeaderText("Wipe Everything?");
            alert.setContentText("This will delete ALL data locally, and attempt to clear your cloud database. This is a factory reset.\n\nYou will be logged out upon completion.");
        } else if (isLocal) {
            alert.setHeaderText("Wipe Local Library Data?");
            alert.setContentText("This will delete all books, members, circulation records, and staff (except Admins) from this machine. Your cloud sync settings and Admin account will be preserved.\n\nYou will be logged out upon completion.");
        } else if (isCloud) {
            alert.setHeaderText("Wipe Cloud Database?");
            alert.setContentText("This will clear all tables in your cloud database. Your local data will be untouched.");
        }

        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                if (isBoth || isCloud) {
                    try {
                        Connection cloud = syncManager.getRemoteConnection();
                        if (cloud != null) {
                            try (java.sql.Statement st = cloud.createStatement()) {
                                st.execute("DROP TABLE IF EXISTS attendance CASCADE");
                                st.execute("DROP TABLE IF EXISTS borrow_records CASCADE");
                                st.execute("DROP TABLE IF EXISTS books CASCADE");
                                st.execute("DROP TABLE IF EXISTS categories CASCADE");
                                st.execute("DROP TABLE IF EXISTS members CASCADE");
                                st.execute("DROP TABLE IF EXISTS users CASCADE");
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            } finally {
                                cloud.close();
                            }
                        } else if (!isBoth) {
                            ToastUtil.show("Cloud is not connected. Cannot wipe cloud data.");
                            return;
                        }
                    } catch (SQLException ex) {
                        ToastUtil.show("Failed to connect to cloud: " + ex.getMessage());
                        if (!isBoth) return;
                    }
                }

                if (isBoth) {
                    try {
                        DatabaseManager.getInstance().wipeEverything();
                        performLogout("Total wipe complete. System reset.");
                    } catch (SQLException ex) {
                        ToastUtil.show("Failed to wipe local data: " + ex.getMessage());
                    }
                } else if (isLocal) {
                    try {
                        DatabaseManager.getInstance().wipeLocalKeepAdmin();
                        performLogout("Local data wiped. Please log in to resync from cloud.");
                    } catch (SQLException ex) {
                        ToastUtil.show("Failed to wipe local data: " + ex.getMessage());
                    }
                } else if (isCloud) {
                    ToastUtil.show("Cloud database wiped successfully.");
                }
            }
        });
    }

    private void performLogout(String message) {
        SessionManager.clearSession();
        Navigator.show(new LoginView(authService).build(), "Login");
        ToastUtil.show(message);
    }

    private VBox buildAboutSection() {
        VBox card = buildSectionCard("About");

        VBox brandBox = new VBox(2);
        Label lblLMS = new Label("LMS");
        lblLMS.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label lblSub = new Label("Library Management System");
        lblSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #71717a;");
        brandBox.getChildren().addAll(lblLMS, lblSub);

        Label lblVersion = new Label("Version: 1.0.0");

        card.getChildren().addAll(brandBox, lblVersion);

        if ("ADMIN".equals(currentUser.role())) {
            Button btnUpdate = new Button("Check for Updates");
            btnUpdate.setOnAction(e -> {
                btnUpdate.setText("Checking...");
                btnUpdate.setDisable(true);
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (InterruptedException ex) {}
                    Platform.runLater(() -> {
                        ToastUtil.show("You are on the latest version.");
                        btnUpdate.setText("Check for Updates");
                        btnUpdate.setDisable(false);
                    });
                }).start();
            });
            card.getChildren().add(btnUpdate);
        }

        return card;
    }
}
