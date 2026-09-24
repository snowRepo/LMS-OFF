package com.lms.ui;

import com.lms.db.MemberDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;

import java.util.List;

public class MemberView {

    private final MemberDAO dao;
    private static final int ITEMS_PER_PAGE = 20;
    
    private TextField txtSearch;
    private ComboBox<String> cbStatusFilter;
    private Pagination pagination;
    private TableView<MemberDAO.Member> table;

    public MemberView() {
        this.dao = new MemberDAO();
    }

    public Node build() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        Label lblTitle = new Label("Members Management");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #18181b;");

        HBox topControls = new HBox(12);
        topControls.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new TextField();
        txtSearch.setPromptText("Search Members...");
        txtSearch.setPrefWidth(250);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> refreshData());

        cbStatusFilter = new ComboBox<>();
        cbStatusFilter.getItems().addAll("All Statuses", "Active", "Inactive");
        cbStatusFilter.getSelectionModel().selectFirst();
        cbStatusFilter.setOnAction(e -> refreshData());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = new Button("+ Add Member");
        btnAdd.setOnAction(e -> showMemberDialog(null));

        topControls.getChildren().addAll(txtSearch, cbStatusFilter, spacer, btnAdd);

        pagination = new Pagination();
        pagination.setPageFactory(this::createPage);
        VBox.setVgrow(pagination, Priority.ALWAYS);

        refreshData(); 

        root.getChildren().addAll(lblTitle, topControls, pagination);
        return root;
    }

    private Integer getStatusFilter() {
        if (cbStatusFilter == null || cbStatusFilter.getValue() == null) return null;
        if (cbStatusFilter.getValue().equals("Active")) return 1;
        if (cbStatusFilter.getValue().equals("Inactive")) return 0;
        return null;
    }

    private void refreshData() {
        String search = txtSearch.getText();
        Integer statusFilter = getStatusFilter();
        int totalItems = dao.getMembersCount(search, statusFilter);
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
            
            TableColumn<MemberDAO.Member, String> colName = new TableColumn<>("Full Name");
            colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().fullName()));
            
            TableColumn<MemberDAO.Member, String> colDob = new TableColumn<>("Date of Birth");
            colDob.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().dob() != null ? data.getValue().dob() : "-"));
            
            TableColumn<MemberDAO.Member, String> colPhone = new TableColumn<>("Phone");
            colPhone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().phone() != null ? data.getValue().phone() : "-"));
            
            TableColumn<MemberDAO.Member, String> colDate = new TableColumn<>("Date Joined");
            colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().joinedDate() != null ? data.getValue().joinedDate() : "-"));
            
            TableColumn<MemberDAO.Member, Void> colStatus = new TableColumn<>("Status");
            colStatus.setPrefWidth(120);
            colStatus.setMaxWidth(120);
            colStatus.setMinWidth(120);
            colStatus.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        MemberDAO.Member member = getTableRow().getItem();
                        HBox box = new HBox(6);
                        box.setAlignment(Pos.CENTER);
                        
                        Circle dot = new Circle(4);
                        dot.setFill(member.isActive() ? Color.web("#22c55e") : Color.web("#9ca3af"));
                        
                        Label lbl = new Label(member.isActive() ? "Active" : "Inactive");
                        lbl.setStyle("-fx-text-fill: #3f3f46;");
                        
                        box.getChildren().addAll(dot, lbl);
                        setGraphic(box);
                    }
                }
            });
            
            TableColumn<MemberDAO.Member, Void> colActions = new TableColumn<>("Actions");
            colActions.setPrefWidth(150);
            colActions.setMaxWidth(150);
            colActions.setMinWidth(150);
            colActions.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        MemberDAO.Member member = getTableRow().getItem();
                        HBox actionBox = new HBox(8);
                        actionBox.setAlignment(Pos.CENTER);
                        
                        Button btnEdit = new Button("Edit");
                        btnEdit.setOnAction(e -> showMemberDialog(member));
                        
                        Button btnToggle = new Button(member.isActive() ? "Deactivate" : "Activate");
                        btnToggle.setOnAction(e -> {
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
alert.initOwner(com.lms.util.Navigator.getStage());
                            alert.setTitle("Confirm Status Change");
                            alert.setHeaderText(null);
                            if (txtSearch.getScene() != null && txtSearch.getScene().getWindow() != null) {
                                alert.initOwner(txtSearch.getScene().getWindow());
                            }
                            alert.setContentText("Are you sure you want to " + (member.isActive() ? "deactivate" : "activate") + " " + member.fullName() + "?");
                            
                            alert.showAndWait().ifPresent(result -> {
                                if (result == ButtonType.OK) {
                                    boolean newStatus = !member.isActive();
                                    boolean toggled = dao.toggleMemberStatus(member.id(), newStatus);
                                    if (toggled) {
                                        com.lms.util.ToastUtil.show("Member status updated!");
                                        refreshData();
                                    } else {
                                        com.lms.util.ToastUtil.show("Failed to update status.");
                                    }
                                }
                            });
                        });
                        
                        actionBox.getChildren().addAll(btnEdit, btnToggle);
                        setGraphic(actionBox);
                    }
                }
            });

            table.getColumns().addAll(colName, colDob, colPhone, colDate, colStatus, colActions);
        }
        
        loadTableData(pageIndex);
        return table;
    }

    private void loadTableData(int pageIndex) {
        String search = txtSearch.getText();
        Integer statusFilter = getStatusFilter();
        List<MemberDAO.Member> data = dao.getMembers(search, statusFilter, pageIndex * ITEMS_PER_PAGE, ITEMS_PER_PAGE);
        table.getItems().setAll(data);
    }

    private void showMemberDialog(MemberDAO.Member memberToEdit) {
        Dialog<Boolean> dialog = new Dialog<>();
dialog.initOwner(com.lms.util.Navigator.getStage());
        dialog.setTitle(memberToEdit == null ? "Add Member" : "Edit Member");
        dialog.setHeaderText(null);
        
        if (txtSearch.getScene() != null && txtSearch.getScene().getWindow() != null) {
            dialog.initOwner(txtSearch.getScene().getWindow());
        }

        ButtonType btnSave = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSave, ButtonType.CANCEL);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setPrefWidth(350); 

        TextField txtName = new TextField();
        txtName.setPromptText("Full Name *");
        
        TextField txtEmail = new TextField();
        txtEmail.setPromptText("Email Address");

        DatePicker dpDob = new DatePicker();
        dpDob.setPromptText("Date of Birth");
        dpDob.setMaxWidth(Double.MAX_VALUE);
        
        TextField txtPhone = new TextField();
        txtPhone.setPromptText("Phone Number");

        TextArea txtAddress = new TextArea();
        txtAddress.setPromptText("Residential Address");
        txtAddress.setPrefRowCount(3);
        txtAddress.setWrapText(true);
        
        ComboBox<String> cbStatus = new ComboBox<>();
        cbStatus.getItems().addAll("Active", "Inactive");
        cbStatus.setPromptText("Status");
        cbStatus.setMaxWidth(Double.MAX_VALUE);
        cbStatus.getSelectionModel().selectFirst();

        if (memberToEdit != null) {
            txtName.setText(memberToEdit.fullName());
            txtEmail.setText(memberToEdit.email() != null ? memberToEdit.email() : "");
            if (memberToEdit.dob() != null && !memberToEdit.dob().isBlank()) {
                dpDob.setValue(java.time.LocalDate.parse(memberToEdit.dob()));
            }
            txtPhone.setText(memberToEdit.phone() != null ? memberToEdit.phone() : "");
            txtAddress.setText(memberToEdit.address() != null ? memberToEdit.address() : "");
            cbStatus.getSelectionModel().select(memberToEdit.isActive() ? "Active" : "Inactive");
        }

        layout.getChildren().addAll(txtName, txtEmail, dpDob, txtPhone, txtAddress, cbStatus);
        dialog.getDialogPane().setContent(layout);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSave) {
                if (txtName.getText().trim().isEmpty()) {
                    return false; 
                }
                
                boolean isActive = "Active".equals(cbStatus.getValue());

                if (memberToEdit == null) {
                    String dobStr = dpDob.getValue() != null ? dpDob.getValue().toString() : null;
                    return dao.insertMember(txtName.getText(), txtEmail.getText(), txtPhone.getText(), txtAddress.getText(), isActive, dobStr);
                } else {
                    String dobStr = dpDob.getValue() != null ? dpDob.getValue().toString() : null;
                    return dao.updateMember(memberToEdit.id(), txtName.getText(), txtEmail.getText(), txtPhone.getText(), txtAddress.getText(), isActive, dobStr);
                }
            }
            return null; // Cancel
        });

        dialog.showAndWait().ifPresent(success -> {
            if (success) {
                com.lms.util.ToastUtil.show(memberToEdit == null ? "Member added successfully!" : "Member updated successfully!");
                refreshData();
            } else {
                com.lms.util.ToastUtil.show("Failed to save member. Full Name is required.");
            }
        });
    }
}
