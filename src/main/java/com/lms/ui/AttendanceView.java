package com.lms.ui;

import com.lms.db.AttendanceDAO;
import com.lms.db.MemberDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import java.time.LocalDate;
import java.util.List;

public class AttendanceView {

    private final AttendanceDAO dao;
    private static final int ITEMS_PER_PAGE = 15;
    
    private TextField txtSearch;
    private DatePicker dpDate;
    private Pagination pagination;
    private TableView<AttendanceDAO.AttendanceRecord> table;

    public AttendanceView() {
        this.dao = new AttendanceDAO();
    }

    public Node build() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        Label lblTitle = new Label("Attendance Log");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #18181b;");

        HBox topControls = new HBox(12);
        topControls.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new TextField();
        txtSearch.setPromptText("Search Member Name...");
        txtSearch.setPrefWidth(250);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> refreshData());

        dpDate = new DatePicker(LocalDate.now());
        dpDate.setPrefWidth(150);
        dpDate.valueProperty().addListener((obs, oldVal, newVal) -> refreshData());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnCheckIn = new Button("+ Check In Member");
        btnCheckIn.setOnAction(e -> showCheckInDialog());

        topControls.getChildren().addAll(txtSearch, dpDate, spacer, btnCheckIn);

        pagination = new Pagination();
        pagination.setPageFactory(this::createPage);
        VBox.setVgrow(pagination, Priority.ALWAYS);

        refreshData(); 

        root.getChildren().addAll(lblTitle, topControls, pagination);
        return root;
    }

    private void refreshData() {
        String search = txtSearch.getText();
        LocalDate date = dpDate.getValue();
        int totalItems = dao.getAttendanceCount(search, date);
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
            
            TableColumn<AttendanceDAO.AttendanceRecord, String> colName = new TableColumn<>("Member Name");
            colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().memberName()));
            
            TableColumn<AttendanceDAO.AttendanceRecord, String> colDate = new TableColumn<>("Date");
            colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().date()));
            
            TableColumn<AttendanceDAO.AttendanceRecord, String> colCheckIn = new TableColumn<>("Check In");
            colCheckIn.setCellValueFactory(data -> new SimpleStringProperty(formatTime(data.getValue().checkIn())));
            
            TableColumn<AttendanceDAO.AttendanceRecord, String> colCheckOut = new TableColumn<>("Check Out");
            colCheckOut.setCellValueFactory(data -> new SimpleStringProperty(formatTime(data.getValue().checkOut())));
            
            TableColumn<AttendanceDAO.AttendanceRecord, Void> colStatus = new TableColumn<>("Status");
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
                        AttendanceDAO.AttendanceRecord rec = getTableRow().getItem();
                        boolean inLibrary = (rec.checkOut() == null || rec.checkOut().isBlank());
                        
                        HBox box = new HBox(6);
                        box.setAlignment(Pos.CENTER);
                        
                        Circle dot = new Circle(4);
                        dot.setFill(inLibrary ? Color.web("#22c55e") : Color.web("#9ca3af"));
                        
                        Label lbl = new Label(inLibrary ? "In Library" : "Checked Out");
                        lbl.setStyle("-fx-text-fill: #3f3f46;");
                        
                        box.getChildren().addAll(dot, lbl);
                        setGraphic(box);
                    }
                }
            });
            
            TableColumn<AttendanceDAO.AttendanceRecord, Void> colActions = new TableColumn<>("Actions");
            colActions.setPrefWidth(120);
            colActions.setMaxWidth(120);
            colActions.setMinWidth(120);
            colActions.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        AttendanceDAO.AttendanceRecord rec = getTableRow().getItem();
                        boolean inLibrary = (rec.checkOut() == null || rec.checkOut().isBlank());
                        
                        if (inLibrary) {
                            Button btnCheckOut = new Button("Check Out");
                            btnCheckOut.setOnAction(e -> {
                                if (dao.checkOut(rec.id())) {
                                    com.lms.util.ToastUtil.show("Member checked out!");
                                    refreshData();
                                }
                            });
                            HBox box = new HBox(btnCheckOut);
                            box.setAlignment(Pos.CENTER);
                            setGraphic(box);
                        } else {
                            setGraphic(null);
                        }
                    }
                }
            });

            table.getColumns().addAll(colName, colDate, colCheckIn, colCheckOut, colStatus, colActions);
        }
        
        loadTableData(pageIndex);
        return table;
    }

    private void loadTableData(int pageIndex) {
        String search = txtSearch.getText();
        LocalDate date = dpDate.getValue();
        List<AttendanceDAO.AttendanceRecord> data = dao.getAttendance(search, date, pageIndex * ITEMS_PER_PAGE, ITEMS_PER_PAGE);
        table.getItems().setAll(data);
    }
    
    private String formatTime(String datetime) {
        if (datetime == null || datetime.isBlank()) return "-";
        // sqlite datetime format is usually YYYY-MM-DD HH:MM:SS
        String[] parts = datetime.split(" ");
        if (parts.length > 1) {
            String time = parts[1];
            // drop seconds
            String[] timeParts = time.split(":");
            if (timeParts.length >= 2) {
                return timeParts[0] + ":" + timeParts[1];
            }
            return time;
        }
        return datetime;
    }

    private void showCheckInDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
dialog.initOwner(com.lms.util.Navigator.getStage());
        dialog.setTitle("Check In Member");
        dialog.setHeaderText(null);
        if (txtSearch.getScene() != null && txtSearch.getScene().getWindow() != null) {
            dialog.initOwner(txtSearch.getScene().getWindow());
        }
        
        ButtonType btnCheckInType = new ButtonType("Check In", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnCheckInType, ButtonType.CANCEL);
        
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setPrefWidth(400);
        
        TextField txtMemberSearch = new TextField();
        txtMemberSearch.setPromptText("Type Name to Search...");
        
        ListView<MemberDAO.Member> lvMembers = new ListView<>();
        lvMembers.setPrefHeight(200);
        
        MemberDAO memberDao = new MemberDAO();
        
        txtMemberSearch.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.trim().length() > 0) {
                // Fetch active matching members (statusFilter = 1)
                List<MemberDAO.Member> results = memberDao.getMembers(val, 1, 0, 15);
                lvMembers.getItems().setAll(results);
            } else {
                lvMembers.getItems().clear();
            }
        });
        
        lvMembers.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(MemberDAO.Member item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String ageStr = "Unknown Age";
                    if (item.dob() != null && !item.dob().isBlank()) {
                        try {
                            java.time.LocalDate dob = java.time.LocalDate.parse(item.dob());
                            int age = java.time.Period.between(dob, java.time.LocalDate.now()).getYears();
                            ageStr = age + " yrs";
                        } catch (Exception e) {
                            // ignore parse errors
                        }
                    }
                    setText(item.fullName() + " - " + ageStr);
                }
            }
        });
        
        layout.getChildren().addAll(txtMemberSearch, lvMembers);
        dialog.getDialogPane().setContent(layout);
        
        Node checkInButton = dialog.getDialogPane().lookupButton(btnCheckInType);
        checkInButton.setDisable(true);
        lvMembers.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            checkInButton.setDisable(val == null);
        });
        
        dialog.setResultConverter(btn -> {
            if (btn == btnCheckInType) {
                MemberDAO.Member selected = lvMembers.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    return dao.checkIn(selected.id());
                }
                return false;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(success -> {
            if (success) {
                com.lms.util.ToastUtil.show("Member checked in successfully!");
                refreshData();
            } else {
                com.lms.util.ToastUtil.show("Failed to check in member.");
            }
        });
    }
}
