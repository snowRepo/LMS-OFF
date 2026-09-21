package com.lms.ui;

import com.lms.db.ActivityLogDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class ActivityLogView {

    private final ActivityLogDAO dao;
    private static final int ITEMS_PER_PAGE = 15;
    
    private TextField txtSearch;
    private DatePicker dpFromDate;
    private DatePicker dpToDate;
    private Pagination pagination;
    private TableView<ActivityLogDAO.ActivityLog> table;

    public ActivityLogView() {
        this.dao = new ActivityLogDAO();
    }

    public Node build() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        // Title
        Label lblTitle = new Label("Activity Logs");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #18181b;");

        // Top Controls
        HBox topControls = new HBox(12);
        topControls.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new TextField();
        txtSearch.setPromptText("Search by username, action, or description...");
        txtSearch.setPrefWidth(250);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> refreshData());
        
        dpFromDate = new DatePicker();
        dpFromDate.setPromptText("From Date");
        dpFromDate.setPrefWidth(120);
        dpFromDate.valueProperty().addListener((obs, oldVal, newVal) -> refreshData());

        dpToDate = new DatePicker();
        dpToDate.setPromptText("To Date");
        dpToDate.setPrefWidth(120);
        dpToDate.valueProperty().addListener((obs, oldVal, newVal) -> refreshData());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topControls.getChildren().addAll(txtSearch, dpFromDate, dpToDate, spacer);

        // Pagination and Table setup
        pagination = new Pagination();
        pagination.setPageFactory(this::createPage);
        VBox.setVgrow(pagination, Priority.ALWAYS);

        refreshData(); 

        root.getChildren().addAll(lblTitle, topControls, pagination);
        return root;
    }

    private void refreshData() {
        String search = txtSearch.getText();
        String fromDate = dpFromDate.getValue() != null ? dpFromDate.getValue().toString() : null;
        String toDate = dpToDate.getValue() != null ? dpToDate.getValue().toString() : null;
        
        int totalItems = dao.countLogs(search, fromDate, toDate);
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
            
            TableColumn<ActivityLogDAO.ActivityLog, String> colTime = new TableColumn<>("Timestamp");
            colTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().createdAt()));
            colTime.setMinWidth(150);
            colTime.setPrefWidth(200);

            TableColumn<ActivityLogDAO.ActivityLog, String> colUser = new TableColumn<>("User");
            colUser.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().username()));
            colUser.setMinWidth(150);
            colUser.setPrefWidth(200);

            TableColumn<ActivityLogDAO.ActivityLog, String> colAction = new TableColumn<>("Action Type");
            colAction.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().actionType()));
            colAction.setMinWidth(150);
            colAction.setPrefWidth(200);

            TableColumn<ActivityLogDAO.ActivityLog, String> colDesc = new TableColumn<>("Description");
            colDesc.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().description()));
            colDesc.setMinWidth(250);
            colDesc.setPrefWidth(400);

            table.getColumns().addAll(colTime, colUser, colAction, colDesc);
        }
        
        loadTableData(pageIndex);
        return table;
    }

    private void loadTableData(int pageIndex) {
        String search = txtSearch.getText();
        String fromDate = dpFromDate.getValue() != null ? dpFromDate.getValue().toString() : null;
        String toDate = dpToDate.getValue() != null ? dpToDate.getValue().toString() : null;
        List<ActivityLogDAO.ActivityLog> data;
        
        if (search == null || search.trim().isEmpty()) {
            data = dao.getLogs(fromDate, toDate, ITEMS_PER_PAGE, pageIndex * ITEMS_PER_PAGE);
        } else {
            data = dao.searchLogs(search.trim(), fromDate, toDate, ITEMS_PER_PAGE, pageIndex * ITEMS_PER_PAGE);
        }
        
        table.getItems().setAll(data);
    }
}
