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
    private static final int ITEMS_PER_PAGE = 20;
    
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
        layout.setMinHeight(230);
        
        // --- Book Search ---
        TextField txtBookSearch = new TextField();
        txtBookSearch.setPromptText("Search Book Title...");
        VBox lvBooks = new VBox();
        lvBooks.setStyle("-fx-border-color: #d1d5db; -fx-background-color: white; -fx-border-radius: 4; -fx-background-radius: 4;");
        lvBooks.setVisible(false);
        lvBooks.setManaged(false);
        
        BookDAO.Book[] finalSelectedBook = new BookDAO.Book[1];
        MemberDAO.Member[] finalSelectedMember = new MemberDAO.Member[1];
        
        Node issueBtn = dialog.getDialogPane().lookupButton(btnIssueType);
        issueBtn.setDisable(true);
        Runnable validate = () -> {
            BookDAO.Book selectedBook = finalSelectedBook[0];
            MemberDAO.Member selectedMember = finalSelectedMember[0];
            boolean valid = selectedBook != null && selectedBook.availableCopies() > 0 && selectedMember != null;
            issueBtn.setDisable(!valid);
        };
        
        boolean[] ignoreBookSearch = new boolean[]{false};
        boolean[] ignoreMemberSearch = new boolean[]{false};
        
        BookDAO bookDao = new BookDAO();
        txtBookSearch.textProperty().addListener((obs, old, val) -> {
            if (ignoreBookSearch[0]) return;
            finalSelectedBook[0] = null;
            if (val != null && val.trim().length() > 0) {
                // Fetch up to 1 book to prevent pushing buttons off screen
                List<BookDAO.Book> results = bookDao.getBooks(val, null, 0, 1);
                
                if (results.isEmpty() || (results.size() == 1 && results.get(0).title().equalsIgnoreCase(val.trim()))) {
                    lvBooks.getChildren().clear();
                    lvBooks.setVisible(false);
                    lvBooks.setManaged(false);
                } else {
                    lvBooks.getChildren().clear();
                    for (BookDAO.Book item : results) {
                        javafx.scene.control.Label lbl = new javafx.scene.control.Label(item.title() + " - " + item.availableCopies() + " available");
                        lbl.setMaxWidth(Double.MAX_VALUE);
                        lbl.setPadding(new javafx.geometry.Insets(5, 10, 5, 10));
                        if (item.availableCopies() <= 0) {
                            lbl.setDisable(true);
                        } else {
                            lbl.setOnMouseEntered(e -> lbl.setStyle("-fx-background-color: -fx-accent; -fx-text-fill: white;"));
                            lbl.setOnMouseExited(e -> lbl.setStyle("-fx-background-color: transparent; -fx-text-fill: -fx-text-base-color;"));
                            lbl.setOnMouseClicked(e -> {
                                finalSelectedBook[0] = item;
                                ignoreBookSearch[0] = true;
                                txtBookSearch.setText(item.title());
                                ignoreBookSearch[0] = false;
                                lvBooks.setVisible(false);
                                lvBooks.setManaged(false);
                                validate.run();
                            });
                        }
                        lvBooks.getChildren().add(lbl);
                    }
                    lvBooks.setVisible(true);
                    lvBooks.setManaged(true);
                }
            } else {
                lvBooks.getChildren().clear();
                lvBooks.setVisible(false);
                lvBooks.setManaged(false);
            }
        });
        
        // ListCell factory removed since we use Labels now

        // --- Member Search ---
        TextField txtMemberSearch = new TextField();
        txtMemberSearch.setPromptText("Search Member Name...");
        VBox lvMembers = new VBox();
        lvMembers.setStyle("-fx-border-color: #d1d5db; -fx-background-color: white; -fx-border-radius: 4; -fx-background-radius: 4;");
        lvMembers.setVisible(false);
        lvMembers.setManaged(false);
        
        MemberDAO memberDao = new MemberDAO();
        txtMemberSearch.textProperty().addListener((obs, old, val) -> {
            if (ignoreMemberSearch[0]) return;
            finalSelectedMember[0] = null;
            if (val != null && val.trim().length() > 0) {
                List<MemberDAO.Member> results = memberDao.getMembers(val, 1, 0, 1);
                
                if (results.isEmpty() || (results.size() == 1 && results.get(0).fullName().equalsIgnoreCase(val.trim()))) {
                    lvMembers.getChildren().clear();
                    lvMembers.setVisible(false);
                    lvMembers.setManaged(false);
                } else {
                    lvMembers.getChildren().clear();
                    for (MemberDAO.Member item : results) {
                        String ageStr = "Unknown Age";
                        if (item.dob() != null && !item.dob().isBlank()) {
                            try {
                                LocalDate dob = LocalDate.parse(item.dob());
                                int age = java.time.Period.between(dob, LocalDate.now()).getYears();
                                ageStr = age + " yrs";
                            } catch (Exception ignored) {}
                        }
                        javafx.scene.control.Label lbl = new javafx.scene.control.Label(item.fullName() + " - " + ageStr);
                        lbl.setMaxWidth(Double.MAX_VALUE);
                        lbl.setPadding(new javafx.geometry.Insets(5, 10, 5, 10));
                        lbl.setOnMouseEntered(e -> lbl.setStyle("-fx-background-color: -fx-accent; -fx-text-fill: white;"));
                        lbl.setOnMouseExited(e -> lbl.setStyle("-fx-background-color: transparent; -fx-text-fill: -fx-text-base-color;"));
                        lbl.setOnMouseClicked(e -> {
                            finalSelectedMember[0] = item;
                            ignoreMemberSearch[0] = true;
                            txtMemberSearch.setText(item.fullName());
                            ignoreMemberSearch[0] = false;
                            lvMembers.setVisible(false);
                            lvMembers.setManaged(false);
                            validate.run();
                        });
                        lvMembers.getChildren().add(lbl);
                    }
                    lvMembers.setVisible(true);
                    lvMembers.setManaged(true);
                }
            } else {
                lvMembers.getChildren().clear();
                lvMembers.setVisible(false);
                lvMembers.setManaged(false);
            }
        });
        
        // ListCell factory removed since we use Labels now

        // --- Due Date ---
        DatePicker dpDue = new DatePicker(LocalDate.now().plusDays(14));
        dpDue.setMaxWidth(Double.MAX_VALUE);
        
        layout.getChildren().addAll(txtBookSearch, lvBooks, txtMemberSearch, lvMembers, dpDue);
        dialog.getDialogPane().setContent(layout);
        
        txtBookSearch.textProperty().addListener((obs, old, val) -> validate.run());
        txtMemberSearch.textProperty().addListener((obs, old, val) -> validate.run());
        
        dialog.setResultConverter(btn -> {
            if (btn == btnIssueType) {
                BookDAO.Book b = finalSelectedBook[0];
                MemberDAO.Member m = finalSelectedMember[0];
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
