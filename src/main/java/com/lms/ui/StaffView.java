package com.lms.ui;

import com.lms.db.UserDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class StaffView {

    private final UserDAO dao;
    private static final int ITEMS_PER_PAGE = 15;
    
    private TextField txtSearch;
    private ComboBox<String> cbStatus;
    private Pagination pagination;
    private TableView<UserDAO.Staff> table;

    public StaffView() {
        this.dao = new UserDAO();
    }

    public Node build() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        Label lblTitle = new Label("Staff Management");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #18181b;");

        HBox topControls = new HBox(12);
        topControls.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new TextField();
        txtSearch.setPromptText("Search Staff...");
        txtSearch.setPrefWidth(250);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> refreshData());

        cbStatus = new ComboBox<>();
        cbStatus.getItems().addAll("All", "Active", "Inactive");
        cbStatus.setValue("All");
        cbStatus.valueProperty().addListener((obs, oldVal, newVal) -> refreshData());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = new Button("+ Add Staff");
        btnAdd.setOnAction(e -> showAddDialog());

        topControls.getChildren().addAll(txtSearch, cbStatus, spacer, btnAdd);

        pagination = new Pagination();
        pagination.setPageFactory(this::createPage);
        VBox.setVgrow(pagination, Priority.ALWAYS);

        refreshData(); 

        root.getChildren().addAll(lblTitle, topControls, pagination);
        return root;
    }

    private void refreshData() {
        String search = txtSearch.getText();
        Integer statusFilter = null;
        if ("Active".equals(cbStatus.getValue())) statusFilter = 1;
        else if ("Inactive".equals(cbStatus.getValue())) statusFilter = 0;
        
        int totalItems = dao.getUsersCount(search, statusFilter);
        int pageCount = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        if (pageCount == 0) pageCount = 1;
        
        pagination.setPageCount(pageCount);
        pagination.setCurrentPageIndex(0);
        
        if (table != null) {
            loadTableData(0);
        }
    }

    private Node createPage(int pageIndex) {
        if (table == null) {
            table = new TableView<>();
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            
            TableColumn<UserDAO.Staff, String> colName = new TableColumn<>("Full Name");
            colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().fullName()));
            
            TableColumn<UserDAO.Staff, String> colUsername = new TableColumn<>("Username");
            colUsername.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().username()));
            
            TableColumn<UserDAO.Staff, String> colEmail = new TableColumn<>("Email");
            colEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().email() != null ? data.getValue().email() : ""));
            
            TableColumn<UserDAO.Staff, String> colDate = new TableColumn<>("Date Created");
            colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().createdAt() != null ? data.getValue().createdAt() : ""));
            
            TableColumn<UserDAO.Staff, String> colRole = new TableColumn<>("Role");
            colRole.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().role()));
            
            TableColumn<UserDAO.Staff, String> colStatus = new TableColumn<>("Status");
            colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isActive() ? "Active" : "Inactive"));
            
            TableColumn<UserDAO.Staff, Void> colActions = new TableColumn<>("Actions");
            colActions.setPrefWidth(140);
            colActions.setMinWidth(140);
            colActions.setMaxWidth(140);
            colActions.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        UserDAO.Staff staff = getTableRow().getItem();
                        HBox box = new HBox(8);
                        box.setAlignment(Pos.CENTER);
                        
                        Button btnToggle = new Button(staff.isActive() ? "Deactivate" : "Activate");
                        btnToggle.setOnAction(e -> {
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle("Confirm Status Change");
                            alert.setHeaderText(null);
                            if (txtSearch.getScene() != null && txtSearch.getScene().getWindow() != null) {
                                alert.initOwner(txtSearch.getScene().getWindow());
                            }
                            alert.setContentText("Are you sure you want to " + (staff.isActive() ? "deactivate" : "activate") + " " + staff.fullName() + "?");
                            Optional<ButtonType> result = alert.showAndWait();
                            if (result.isPresent() && result.get() == ButtonType.OK) {
                                if (dao.toggleUserStatus(staff.id(), !staff.isActive())) {
                                    refreshData();
                                }
                            }
                        });
                        
                        Button btnReset = new Button("Reset");
                        btnReset.setOnAction(e -> {
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle("Confirm Password Reset");
                            alert.setHeaderText(null);
                            if (txtSearch.getScene() != null && txtSearch.getScene().getWindow() != null) {
                                alert.initOwner(txtSearch.getScene().getWindow());
                            }
                            alert.setContentText("Are you sure you want to reset the password for " + staff.username() + "? They will be required to change it on their next login.");
                            Optional<ButtonType> result = alert.showAndWait();
                            if (result.isPresent() && result.get() == ButtonType.OK) {
                                String tempPass = dao.resetPassword(staff.id());
                                if (tempPass != null) {
                                    showCredentialsAlert(staff.username(), tempPass);
                                }
                            }
                        });

                        box.getChildren().addAll(btnToggle, btnReset);
                        setGraphic(box);
                    }
                }
            });

            table.getColumns().addAll(colName, colUsername, colEmail, colRole, colDate, colStatus, colActions);
        }
        
        loadTableData(pageIndex);
        return table;
    }

    private void loadTableData(int pageIndex) {
        String search = txtSearch.getText();
        Integer statusFilter = null;
        if ("Active".equals(cbStatus.getValue())) statusFilter = 1;
        else if ("Inactive".equals(cbStatus.getValue())) statusFilter = 0;
        
        List<UserDAO.Staff> data = dao.getUsers(search, statusFilter, pageIndex * ITEMS_PER_PAGE, ITEMS_PER_PAGE);
        table.getItems().setAll(data);
    }

    private void showAddDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Staff");
        dialog.setHeaderText(null);
        if (txtSearch.getScene() != null && txtSearch.getScene().getWindow() != null) {
            dialog.initOwner(txtSearch.getScene().getWindow());
        }
        
        ButtonType btnSaveType = new ButtonType("Create Account", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSaveType, ButtonType.CANCEL);
        
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setPrefWidth(350);
        
        TextField txtName = new TextField();
        txtName.setPromptText("Full Name");
        
        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Username");
        
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("Email Address");
        
        TextField txtPhone = new TextField();
        txtPhone.setPromptText("Phone Number");
        
        DatePicker dpDob = new DatePicker();
        dpDob.setPromptText("Date of Birth");
        dpDob.setMaxWidth(Double.MAX_VALUE);
        
        layout.getChildren().addAll(txtName, txtUsername, txtEmail, txtPhone, dpDob);
        dialog.getDialogPane().setContent(layout);
        
        Node saveBtn = dialog.getDialogPane().lookupButton(btnSaveType);
        saveBtn.setDisable(true);
        
        Runnable validate = () -> {
            saveBtn.setDisable(txtName.getText().trim().isEmpty() || txtUsername.getText().trim().isEmpty());
        };
        txtName.textProperty().addListener((obs, o, n) -> validate.run());
        txtUsername.textProperty().addListener((obs, o, n) -> validate.run());
        
        dialog.setResultConverter(btn -> {
            if (btn == btnSaveType) {
                String dobStr = dpDob.getValue() != null ? dpDob.getValue().toString() : null;
                String tempPass = dao.insertUser(txtName.getText(), txtUsername.getText(), txtEmail.getText(), txtPhone.getText(), dobStr, "LIBRARIAN");
                if (tempPass != null) {
                    showCredentialsAlert(txtUsername.getText(), tempPass);
                    refreshData();
                } else {
                    com.lms.util.ToastUtil.show("Failed to create user. Username may already exist.");
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    private void showCredentialsAlert(String username, String tempPass) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Account Credentials");
        alert.setHeaderText("Please copy and provide these credentials to the staff member.");
        
        if (txtSearch.getScene() != null && txtSearch.getScene().getWindow() != null) {
            alert.initOwner(txtSearch.getScene().getWindow());
        }
        
        TextArea area = new TextArea("Username: " + username + "\nTemporary Password: " + tempPass);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(3);
        
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }
}
