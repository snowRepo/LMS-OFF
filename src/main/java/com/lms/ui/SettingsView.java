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
        } else {
            root.getChildren().add(buildLibrarianSyncSection());
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

        com.lms.util.Navigator.centerDialog(dialog);
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

        com.lms.util.Navigator.centerDialog(dialog);
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

    private VBox buildLibrarianSyncSection() {
        VBox card = buildSectionCard("Cloud Sync Connection");
        
        SyncConfigDAO.SyncConfig conf = syncConfigDAO.getConfig();
        if (conf == null || conf.dbType() == null || conf.dbType().isEmpty()) {
            Label lblNotConfig = new Label("Cloud Sync has not been configured by an Administrator.");
            lblNotConfig.setStyle("-fx-text-fill: #71717a;");
            card.getChildren().add(lblNotConfig);
            return card;
        }

        Label lblInfo = new Label("Cloud Database is configured and ready.");
        lblInfo.setStyle("-fx-text-fill: #18181b;");

        boolean isConnected = conf.isEnabled();
        Button btnToggle = new Button(isConnected ? "Disconnect" : "Connect");
        Label lblStatus = new Label(isConnected ? "Connected to Cloud" : "Disconnected");
        lblStatus.setStyle(isConnected ? "-fx-text-fill: #22c55e;" : "-fx-text-fill: #71717a;");

        btnToggle.setOnAction(e -> {
            boolean turningOn = "Connect".equals(btnToggle.getText());
            syncConfigDAO.setEnabled(turningOn);
            if (turningOn) {
                btnToggle.setText("Disconnect");
                lblStatus.setText("Connected to Cloud");
                lblStatus.setStyle("-fx-text-fill: #22c55e;");
                ToastUtil.show("Connected to Cloud Database.");
            } else {
                btnToggle.setText("Connect");
                lblStatus.setText("Disconnected");
                lblStatus.setStyle("-fx-text-fill: #71717a;");
                ToastUtil.show("Disconnected from Cloud.");
            }
            com.lms.util.GlobalState.refreshSyncState();
        });

        HBox btns = new HBox(12, btnToggle, lblStatus);
        btns.setAlignment(Pos.CENTER_LEFT);
        
        card.getChildren().addAll(lblInfo, btns);
        return card;
    }

    private VBox buildDatabaseConnectionSection() {
        VBox card = buildSectionCard("Database Connection (Cloud Sync)");

        VBox form = new VBox(12);
        form.setMaxWidth(350);

        ComboBox<String> cbType = new ComboBox<>();
        cbType.getItems().addAll("PostgreSQL", "MySQL", "MariaDB");
        cbType.setPromptText("Database Type");
        cbType.setMaxWidth(Double.MAX_VALUE);
        cbType.setValue("PostgreSQL");
        
        TextField txtHost = new TextField();
        txtHost.setPromptText("Host (e.g. ep-cool-butterfly-12345.aws.neon.tech)");
        txtHost.setMaxWidth(Double.MAX_VALUE);
        
        TextField txtPort = new TextField();
        txtPort.setPromptText("Port (e.g. 5432)");
        txtPort.setMaxWidth(Double.MAX_VALUE);
        
        TextField txtDbName = new TextField();
        txtDbName.setPromptText("Database Name");
        txtDbName.setMaxWidth(Double.MAX_VALUE);
        
        TextField txtUser = new TextField();
        txtUser.setPromptText("Username");
        txtUser.setMaxWidth(Double.MAX_VALUE);
        
        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Password");
        txtPass.setMaxWidth(Double.MAX_VALUE);

        form.getChildren().addAll(cbType, txtHost, txtPort, txtDbName, txtUser, txtPass);

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
        
        boolean isConnected = (conf != null && conf.isEnabled());
        Button btnSave = new Button(isConnected ? "Disconnect" : "Save & Connect");
        if (isConnected) {
            btnSave.setDisable(false);
        } else {
            btnSave.setDisable(true);
        }

        Label lblStatus = new Label();

        btnTest.setOnAction(e -> {
            lblStatus.setText("Testing...");
            lblStatus.setStyle("-fx-text-fill: #039ED3;");
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
                        btnSave.setText("Save & Connect");
                        btnSave.setStyle("");
                        btnSave.setDisable(false);
                    } else {
                        lblStatus.setText("✗ Connection failed. Check credentials.");
                        lblStatus.setStyle("-fx-text-fill: #ef4444;");
                    }
                });
            }).start();
        });

        btnSave.setOnAction(e -> {
            if ("Disconnect".equals(btnSave.getText())) {
                syncConfigDAO.setEnabled(false);
                btnSave.setText("Save & Connect");
                btnSave.setDisable(false); // Can be immediately re-saved if test was already OK, but wait, typing resets it anyway
                lblStatus.setText("Disconnected.");
                lblStatus.setStyle("-fx-text-fill: #666666;");
                ToastUtil.show("Cloud database disconnected.");
                com.lms.util.GlobalState.refreshSyncState();
            } else {
                btnSave.setText("Saving & Connecting...");
                btnSave.setDisable(true);
                
                syncConfigDAO.saveConfig(
                        cbType.getValue(), txtHost.getText(), txtPort.getText(),
                        txtDbName.getText(), txtUser.getText(), txtPass.getText(), true
                );
                
                syncManager.sync(
                    () -> { // onSuccess
                        ToastUtil.show("Database configured and synced successfully!");
                        lblStatus.setText("Connected and Synced.");
                        lblStatus.setStyle("-fx-text-fill: #22c55e;");
                        btnSave.setText("Disconnect");
                        btnSave.setDisable(false);
                        com.lms.util.GlobalState.refreshSyncState();
                    },
                    () -> { // onFailure
                        lblStatus.setText("Sync failed. Check credentials.");
                        lblStatus.setStyle("-fx-text-fill: #ef4444;");
                        btnSave.setText("Save & Connect");
                        btnSave.setDisable(false);
                        com.lms.util.GlobalState.refreshSyncState();
                    }
                );
            }
        });

        // if user types something, disable save until re-tested
        javafx.beans.value.ChangeListener<String> resetSave = (obs, oldV, newV) -> {
            if (!"Disconnect".equals(btnSave.getText())) {
                btnSave.setDisable(true);
                lblStatus.setText("");
            }
        };
        cbType.valueProperty().addListener(resetSave);
        txtHost.textProperty().addListener(resetSave);
        txtPort.textProperty().addListener(resetSave);
        txtDbName.textProperty().addListener(resetSave);
        txtUser.textProperty().addListener(resetSave);
        txtPass.textProperty().addListener(resetSave);

        btns.getChildren().addAll(btnTest, btnSave);
        
        VBox statusBox = new VBox(5, btns, lblStatus);
        
        card.getChildren().addAll(form, statusBox);
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

        com.lms.util.Navigator.centerDialog(alert);
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
                        performFactoryReset("Factory reset complete.");
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

    private void performFactoryReset(String message) {
        SessionManager.clearSession();
        // Since EULA is already agreed to from the initial install, we skip WelcomeView 
        // and drop the user straight into Database Setup.
        Navigator.show(new com.lms.auth.setup.DbSetupView(authService).build(), "Database Setup");
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

        Label lblVersion = new Label("Version: " + com.lms.util.UpdateManager.CURRENT_VERSION);

        card.getChildren().addAll(brandBox, lblVersion);

        if ("ADMIN".equals(currentUser.role())) {
            Button btnUpdate = new Button("Check for Updates");
            btnUpdate.setOnAction(e -> {
                javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
                dialog.setTitle("Software Update");
                dialog.setHeaderText("Checking for updates...");
                
                VBox dialogContent = new VBox(16);
                dialogContent.setAlignment(Pos.CENTER);
                dialogContent.setPadding(new Insets(20));
                
                javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
                spinner.setStyle("-fx-accent: #039ED3;");
                Label lblStatus = new Label("Connecting to GitHub to fetch latest release...");
                lblStatus.setStyle("-fx-text-fill: #71717a;");
                
                dialogContent.getChildren().addAll(spinner, lblStatus);
                dialog.getDialogPane().setContent(dialogContent);
                dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CANCEL);
                
                dialog.show();
                
                new Thread(() -> {
                    try {
                        java.util.Optional<com.lms.util.UpdateManager.ReleaseInfo> opt = com.lms.util.UpdateManager.checkForUpdates();
                        Platform.runLater(() -> {
                            if (opt.isPresent()) {
                                com.lms.util.UpdateManager.ReleaseInfo info = opt.get();
                                dialog.setHeaderText("Update Available: " + info.version());
                                lblStatus.setText("A new version is available! Do you want to download it?");
                                spinner.setVisible(false);
                                spinner.setManaged(false);
                                
                                ButtonType btnDownload = new ButtonType("Download", ButtonBar.ButtonData.OK_DONE);
                                dialog.getDialogPane().getButtonTypes().setAll(btnDownload, ButtonType.CANCEL);
                                
                                javafx.scene.control.Button downBtn = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(btnDownload);
                                downBtn.setStyle("-fx-background-color: #039ED3; -fx-text-fill: white; -fx-font-weight: bold;");
                                
                                downBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
                                    ev.consume();
                                    downBtn.setDisable(true);
                                    dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setDisable(true);
                                    
                                    dialog.setHeaderText("Downloading Update...");
                                    lblStatus.setText("Starting download...");
                                    
                                    javafx.scene.control.ProgressBar progressBar = new javafx.scene.control.ProgressBar(0.0);
                                    progressBar.setStyle("-fx-accent: #039ED3;");
                                    progressBar.setMaxWidth(Double.MAX_VALUE);
                                    progressBar.setPrefWidth(300);
                                    
                                    dialogContent.getChildren().clear();
                                    dialogContent.getChildren().addAll(progressBar, lblStatus);
                                    
                                    new Thread(() -> {
                                        try {
                                            java.io.File installer = com.lms.util.UpdateManager.downloadUpdate(info.downloadUrl(), progress -> {
                                                Platform.runLater(() -> {
                                                    progressBar.setProgress(progress);
                                                    lblStatus.setText(String.format("Downloading... %d%%", (int)(progress * 100)));
                                                });
                                            });
                                            Platform.runLater(() -> {
                                                lblStatus.setText("Launching installer...");
                                                progressBar.setProgress(1.0);
                                                try {
                                                    com.lms.util.UpdateManager.executeUpdate(installer);
                                                } catch(Exception ex) {
                                                    lblStatus.setText("Failed to launch installer.");
                                                }
                                            });
                                        } catch (Exception ex) {
                                            Platform.runLater(() -> {
                                                lblStatus.setText("Download failed: " + ex.getMessage());
                                                dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setDisable(false);
                                            });
                                        }
                                    }).start();
                                });
                            } else {
                                dialog.setHeaderText("Up to Date");
                                spinner.setVisible(false);
                                spinner.setManaged(false);
                                lblStatus.setText("You are running the latest version (" + com.lms.util.UpdateManager.CURRENT_VERSION + ").");
                                dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK);
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            dialog.setHeaderText("Update Check Failed");
                            spinner.setVisible(false);
                            spinner.setManaged(false);
                            lblStatus.setText(ex.getMessage());
                            dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK);
                        });
                    }
                }).start();
            });
            card.getChildren().add(btnUpdate);
        }

        return card;
    }
}
