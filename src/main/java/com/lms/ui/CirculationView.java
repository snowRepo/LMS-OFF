package com.lms.ui;

import com.lms.db.BookDAO;
import com.lms.db.CirculationDAO;
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

public class CirculationView {

    private final CirculationDAO dao;
    private static final int ITEMS_PER_PAGE = 15;
    
    private TextField txtSearch;
    private ComboBox<String> cbStatus;
    private Pagination pagination;
    private TableView<CirculationDAO.CirculationRecord> table;

    public CirculationView() {
        this.dao = new CirculationDAO();
    }

    public Node build() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        Label lblTitle = new Label("Circulation");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #18181b;");

        HBox topControls = new HBox(12);
        topControls.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new TextField();
        txtSearch.setPromptText("Search Book or Member...");
        txtSearch.setPrefWidth(250);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> refreshData());

        cbStatus = new ComboBox<>();
        cbStatus.getItems().addAll("All", "Borrowed", "Returned", "Overdue");
        cbStatus.setValue("All");
        cbStatus.valueProperty().addListener((obs, oldVal, newVal) -> refreshData());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnIssue = new Button("+ Issue Book");
        btnIssue.setOnAction(e -> showIssueDialog());

        topControls.getChildren().addAll(txtSearch, cbStatus, spacer, btnIssue);

        pagination = new Pagination();
        pagination.setPageFactory(this::createPage);
        VBox.setVgrow(pagination, Priority.ALWAYS);

        refreshData(); 

        root.getChildren().addAll(lblTitle, topControls, pagination);
        return root;
    }

    private void refreshData() {
        String search = txtSearch.getText();
        String status = cbStatus.getValue();
        int totalItems = dao.getCirculationCount(search, status);
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
            
            TableColumn<CirculationDAO.CirculationRecord, String> colBook = new TableColumn<>("Book Title");
            colBook.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().bookTitle()));
            
            TableColumn<CirculationDAO.CirculationRecord, String> colMember = new TableColumn<>("Member Name");
            colMember.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().memberName()));
            
            TableColumn<CirculationDAO.CirculationRecord, String> colDate = new TableColumn<>("Borrowed At");
            colDate.setCellValueFactory(data -> new SimpleStringProperty(formatDate(data.getValue().borrowedAt())));
            
            TableColumn<CirculationDAO.CirculationRecord, String> colDue = new TableColumn<>("Due Date");
            colDue.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().dueDate()));
            
            TableColumn<CirculationDAO.CirculationRecord, Void> colStatus = new TableColumn<>("Status");
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
                        CirculationDAO.CirculationRecord rec = getTableRow().getItem();
                        HBox box = new HBox(6);
                        box.setAlignment(Pos.CENTER);
                        
                        Circle dot = new Circle(4);
                        if ("RETURNED".equalsIgnoreCase(rec.status())) {
                            dot.setFill(Color.web("#22c55e"));
                        } else if ("OVERDUE".equalsIgnoreCase(rec.status())) {
                            dot.setFill(Color.web("#ef4444"));
                        } else {
                            dot.setFill(Color.web("#3b82f6")); // BORROWED
                        }
                        
                        Label lbl = new Label(rec.status());
                        lbl.setStyle("-fx-text-fill: #3f3f46;");
                        
                        box.getChildren().addAll(dot, lbl);
                        setGraphic(box);
                    }
                }
            });
            
            TableColumn<CirculationDAO.CirculationRecord, Void> colActions = new TableColumn<>("Actions");
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
                        CirculationDAO.CirculationRecord rec = getTableRow().getItem();
                        
                        if (!"RETURNED".equalsIgnoreCase(rec.status())) {
                            Button btnReturn = new Button("Return");
                            btnReturn.setOnAction(e -> {
                                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
alert.initOwner(com.lms.util.Navigator.getStage());
                                alert.setTitle("Confirm Book Return");
                                alert.setHeaderText(null);
                                if (txtSearch.getScene() != null && txtSearch.getScene().getWindow() != null) {
                                    alert.initOwner(txtSearch.getScene().getWindow());
                                }
                                alert.setContentText("Are you sure you want to mark this book as returned?");
                                
                                alert.showAndWait().ifPresent(result -> {
                                    if (result == ButtonType.OK) {
                                        if (dao.returnBook(rec.id(), rec.bookId())) {
                                            com.lms.util.ToastUtil.show("Book returned successfully!");
                                            refreshData();
                                        }
                                    }
                                });
                            });
                            HBox box = new HBox(btnReturn);
                            box.setAlignment(Pos.CENTER);
                            setGraphic(box);
                        } else {
                            Button btnView = new Button("View");
                            btnView.setOnAction(e -> showDetailsDialog(rec));
                            HBox box = new HBox(btnView);
                            box.setAlignment(Pos.CENTER);
                            setGraphic(box);
                        }
                    }
                }
            });

            table.getColumns().addAll(colBook, colMember, colDate, colDue, colStatus, colActions);
        }
        
        loadTableData(pageIndex);
        return table;
    }

    private void loadTableData(int pageIndex) {
        String search = txtSearch.getText();
        String status = cbStatus.getValue();
        List<CirculationDAO.CirculationRecord> data = dao.getCirculationRecords(search, status, pageIndex * ITEMS_PER_PAGE, ITEMS_PER_PAGE);
        table.getItems().setAll(data);
    }
    
    private String formatDate(String datetime) {
        if (datetime == null || datetime.isBlank()) return "-";
        String[] parts = datetime.split(" ");
        return parts[0]; // just return YYYY-MM-DD
    }

    private void showIssueDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
dialog.initOwner(com.lms.util.Navigator.getStage());
        dialog.setTitle("Issue Book");
        dialog.setHeaderText(null);
        if (txtSearch.getScene() != null && txtSearch.getScene().getWindow() != null) {
            dialog.initOwner(txtSearch.getScene().getWindow());
        }
        
        ButtonType btnIssueType = new ButtonType("Issue Book", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnIssueType, ButtonType.CANCEL);
        
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setPrefWidth(400);
        
        // --- Book Search ---
        Label lblBook = new Label("Select Book:");
        lblBook.setStyle("-fx-font-weight: bold;");
        TextField txtBookSearch = new TextField();
        txtBookSearch.setPromptText("Search Book Title...");
        ListView<BookDAO.Book> lvBooks = new ListView<>();
        lvBooks.setPrefHeight(90);
        
        BookDAO bookDao = new BookDAO();
        txtBookSearch.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.trim().length() > 0) {
                // Fetch up to 10 books (ignoring category filter)
                List<BookDAO.Book> results = bookDao.getBooks(val, null, 0, 10);
                lvBooks.getItems().setAll(results);
            } else {
                lvBooks.getItems().clear();
            }
        });
        
        lvBooks.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(BookDAO.Book item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.title() + " - " + item.availableCopies() + " available");
                    setDisable(item.availableCopies() <= 0); // disable row if none available
                }
            }
        });

        // --- Member Search ---
        Label lblMember = new Label("Select Member:");
        lblMember.setStyle("-fx-font-weight: bold;");
        TextField txtMemberSearch = new TextField();
        txtMemberSearch.setPromptText("Search Member Name...");
        ListView<MemberDAO.Member> lvMembers = new ListView<>();
        lvMembers.setPrefHeight(90);
        
        MemberDAO memberDao = new MemberDAO();
        txtMemberSearch.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.trim().length() > 0) {
                List<MemberDAO.Member> results = memberDao.getMembers(val, 1, 0, 10);
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
                            LocalDate dob = LocalDate.parse(item.dob());
                            int age = java.time.Period.between(dob, LocalDate.now()).getYears();
                            ageStr = age + " yrs";
                        } catch (Exception e) {}
                    }
                    setText(item.fullName() + " - " + ageStr);
                }
            }
        });

        // --- Due Date ---
        Label lblDue = new Label("Due Date:");
        lblDue.setStyle("-fx-font-weight: bold;");
        DatePicker dpDue = new DatePicker(LocalDate.now().plusDays(14));
        dpDue.setMaxWidth(Double.MAX_VALUE);
        
        layout.getChildren().addAll(lblBook, txtBookSearch, lvBooks, lblMember, txtMemberSearch, lvMembers, lblDue, dpDue);
        dialog.getDialogPane().setContent(layout);
        
        Node issueBtn = dialog.getDialogPane().lookupButton(btnIssueType);
        issueBtn.setDisable(true);
        
        // Validation logic
        Runnable validate = () -> {
            BookDAO.Book selectedBook = lvBooks.getSelectionModel().getSelectedItem();
            MemberDAO.Member selectedMember = lvMembers.getSelectionModel().getSelectedItem();
            boolean valid = selectedBook != null && selectedBook.availableCopies() > 0 && selectedMember != null;
            issueBtn.setDisable(!valid);
        };
        lvBooks.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> validate.run());
        lvMembers.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> validate.run());
        
        dialog.setResultConverter(btn -> {
            if (btn == btnIssueType) {
                BookDAO.Book b = lvBooks.getSelectionModel().getSelectedItem();
                MemberDAO.Member m = lvMembers.getSelectionModel().getSelectedItem();
                LocalDate date = dpDue.getValue();
                if (b != null && m != null && date != null) {
                    return dao.issueBook(b.id(), m.id(), date);
                }
                return false;
            }
            return null; // fix for cancel bug
        });
        
        dialog.showAndWait().ifPresent(success -> {
            if (success) {
                com.lms.util.ToastUtil.show("Book issued successfully!");
                refreshData();
            } else {
                com.lms.util.ToastUtil.show("Failed to issue book. Check availability.");
            }
        });
    }
    private void showDetailsDialog(CirculationDAO.CirculationRecord rec) {
        Dialog<Void> dialog = new Dialog<>();
dialog.initOwner(com.lms.util.Navigator.getStage());
        dialog.setTitle("Circulation Details");
        dialog.setHeaderText(null);
        if (txtSearch.getScene() != null && txtSearch.getScene().getWindow() != null) {
            dialog.initOwner(txtSearch.getScene().getWindow());
        }
        
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(24));
        
        addDetailRow(grid, 0, "Book Title:", rec.bookTitle());
        addDetailRow(grid, 1, "Member Name:", rec.memberName());
        addDetailRow(grid, 2, "Date Borrowed:", formatDate(rec.borrowedAt()));
        addDetailRow(grid, 3, "Due Date:", rec.dueDate());
        
        String returnedDateStr = formatDate(rec.returnedAt());
        addDetailRow(grid, 4, "Date Returned:", returnedDateStr);
        
        String returnStatus = "Unknown";
        try {
            LocalDate dueDate = LocalDate.parse(rec.dueDate());
            LocalDate returnedDate = LocalDate.parse(returnedDateStr);
            if (returnedDate.isAfter(dueDate)) {
                returnStatus = "Late Return";
            } else {
                returnStatus = "Returned On Time";
            }
        } catch (Exception e) {}
        
        Label lblStatusVal = new Label(returnStatus);
        lblStatusVal.setStyle(returnStatus.equals("Late Return") ? "-fx-text-fill: #ef4444; -fx-font-weight: bold;" : "-fx-text-fill: #22c55e; -fx-font-weight: bold;");
        Label lblStatus = new Label("Return Status:");
        lblStatus.setStyle("-fx-font-weight: bold; -fx-text-fill: #52525b;");
        grid.add(lblStatus, 0, 5);
        grid.add(lblStatusVal, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #52525b;");
        Label val = new Label(value != null ? value : "N/A");
        val.setStyle("-fx-text-fill: #18181b;");
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }
}
