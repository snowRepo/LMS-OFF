package com.lms.ui;

import com.lms.db.DashboardDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.List;

public class DashboardMetricsView {

    private final DashboardDAO dao;
    private static final int ITEMS_PER_PAGE = 15;

    public DashboardMetricsView() {
        this.dao = new DashboardDAO();
    }

    public Node build() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        // Top Section: Title
        Label lblTitle = new Label("Dashboard Overview");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #18181b;");

        // Summary Cards
        GridPane cardsGrid = new GridPane();
        cardsGrid.setHgap(16);
        cardsGrid.setMaxWidth(Double.MAX_VALUE);
        
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(25);
        cardsGrid.getColumnConstraints().addAll(cc, cc, cc, cc);

        DashboardDAO.DashboardMetrics metrics = dao.getMetrics();
        
        cardsGrid.add(createCard("Total Books", metrics.totalBooks(), "#18181b"), 0, 0);
        cardsGrid.add(createCard("Total Members", metrics.totalMembers(), "#18181b"), 1, 0);
        cardsGrid.add(createCard("Borrowed Books", metrics.borrowedBooks(), "#18181b"), 2, 0);
        cardsGrid.add(createCard("Overdue Books", metrics.overdueBooks(), "#ef4444"), 3, 0);

        // Middle Section: Active Borrowings Table Title
        Label lblTableTitle = new Label("Active Borrowings");
        lblTableTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #3f3f46;");
        lblTableTitle.setPadding(new Insets(10, 0, 0, 0));

        // Pagination & Table
        int totalItems = dao.getActiveBorrowingsCount();
        int pageCount = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        if (pageCount == 0) pageCount = 1;

        Pagination pagination = new Pagination(pageCount, 0);
        pagination.setPageFactory(this::createPage);
        VBox.setVgrow(pagination, Priority.ALWAYS);

        root.getChildren().addAll(lblTitle, cardsGrid, lblTableTitle, pagination);
        return root;
    }

    private Node createCard(String title, int value, String colorHex) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e4e4e7; -fx-border-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");
        
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #71717a;");
        
        Label lblValue = new Label(String.valueOf(value));
        lblValue.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");

        card.getChildren().addAll(lblTitle, lblValue);
        return card;
    }

    private Node createPage(int pageIndex) {
        TableView<DashboardDAO.ActiveBorrowing> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e4e4e7; -fx-border-radius: 8;");
        
        TableColumn<DashboardDAO.ActiveBorrowing, String> colMember = new TableColumn<>("Member Name");
        colMember.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().memberName()));
        
        TableColumn<DashboardDAO.ActiveBorrowing, String> colBook = new TableColumn<>("Book Title");
        colBook.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().bookTitle()));
        
        TableColumn<DashboardDAO.ActiveBorrowing, String> colBorrowDate = new TableColumn<>("Borrow Date");
        colBorrowDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().borrowDate()));
        
        TableColumn<DashboardDAO.ActiveBorrowing, String> colDueDate = new TableColumn<>("Due Date");
        colDueDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().dueDate()));
        
        TableColumn<DashboardDAO.ActiveBorrowing, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status()));
        colStatus.setStyle("-fx-alignment: CENTER;");
        
        // Custom CellFactory for Status (colored dots)
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox box = new HBox(6);
                    box.setAlignment(Pos.CENTER);
                    Circle dot = new Circle(4);
                    
                    if ("OVERDUE".equals(item)) {
                        dot.setFill(Color.web("#ef4444")); // Red
                    } else if ("BORROWED".equals(item)) {
                        dot.setFill(Color.web("#3b82f6")); // Blue
                    } else {
                        dot.setFill(Color.web("#a1a1aa")); // Gray
                    }
                    
                    Label text = new Label(item);
                    text.setStyle("-fx-text-fill: #3f3f46;");
                    
                    box.getChildren().addAll(dot, text);
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        table.getColumns().addAll(colMember, colBook, colBorrowDate, colDueDate, colStatus);

        // Fetch data
        List<DashboardDAO.ActiveBorrowing> data = dao.getActiveBorrowings(pageIndex * ITEMS_PER_PAGE, ITEMS_PER_PAGE);
        table.getItems().addAll(data);

        return table;
    }
}
