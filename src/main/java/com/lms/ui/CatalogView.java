package com.lms.ui;

import com.lms.db.BookDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class CatalogView {

    private final BookDAO dao;
    private static final int ITEMS_PER_PAGE = 20;
    
    private TextField txtSearch;
    private Button btnCategoryFilter;
    private BookDAO.Category currentCategoryFilter = new BookDAO.Category(-1, "All Categories");
    private Pagination pagination;
    private TableView<BookDAO.Book> table;

    public CatalogView() {
        this.dao = new BookDAO();
    }

    public Node build() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        // Title
        Label lblTitle = new Label("Library Catalog");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #18181b;");

        // Top Controls (Search, Filter, Add Book)
        HBox topControls = new HBox(12);
        topControls.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new TextField();
        txtSearch.setPromptText("Search by Title, Author, ISBN...");
        txtSearch.setPrefWidth(250);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> refreshData());

        btnCategoryFilter = new Button("All Categories");
        btnCategoryFilter.setOnAction(e -> showCategoryFilterDialog());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = new Button("+ Add Book");
        btnAdd.setOnAction(e -> showBookDialog(null));

        topControls.getChildren().addAll(txtSearch, btnCategoryFilter, spacer, btnAdd);

        // Pagination and Table setup
        pagination = new Pagination();
        pagination.setPageFactory(this::createPage);
        VBox.setVgrow(pagination, Priority.ALWAYS);

        refreshData(); // Initial load

        root.getChildren().addAll(lblTitle, topControls, pagination);
        return root;
    }

    private void refreshData() {
        String search = txtSearch.getText();
        BookDAO.Category cat = currentCategoryFilter;
        Integer catId = (cat != null && cat.id() != -1) ? cat.id() : null;

        int totalItems = dao.getBooksCount(search, catId);
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
            table.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e4e4e7; -fx-border-radius: 8;");
            
            TableColumn<BookDAO.Book, String> colTitle = new TableColumn<>("Title");
            colTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().title()));
            
            TableColumn<BookDAO.Book, String> colAuthor = new TableColumn<>("Author");
            colAuthor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().author()));
            
            TableColumn<BookDAO.Book, String> colIsbn = new TableColumn<>("ISBN");
            colIsbn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isbn()));
            
            TableColumn<BookDAO.Book, String> colCategory = new TableColumn<>("Category");
            colCategory.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().categoryName()));
            
            TableColumn<BookDAO.Book, String> colAvailable = new TableColumn<>("Available");
            colAvailable.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().availableCopies() + " / " + data.getValue().totalCopies()));
            
            // Actions Column
            TableColumn<BookDAO.Book, Void> colActions = new TableColumn<>("Actions");
            colActions.setPrefWidth(140);
            colActions.setMaxWidth(140);
            colActions.setMinWidth(140);
            colActions.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        BookDAO.Book book = getTableRow().getItem();
                        HBox actionBox = new HBox(8);
                        actionBox.setAlignment(Pos.CENTER);
                        
                        Button btnEdit = new Button("Edit");
                        Button btnDelete = new Button("Delete");
                        
                        btnEdit.setOnAction(e -> showBookDialog(book));
                        
                        btnDelete.setOnAction(e -> {
                            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete '" + book.title() + "'?", ButtonType.YES, ButtonType.NO);
confirm.initOwner(com.lms.util.Navigator.getStage());
                            confirm.setHeaderText("Confirm Deletion");
                            if (txtSearch.getScene() != null && txtSearch.getScene().getWindow() != null) {
                                confirm.initOwner(txtSearch.getScene().getWindow());
                            }
                            confirm.showAndWait().ifPresent(response -> {
                                if (response == ButtonType.YES) {
                                    boolean deleted = dao.deleteBook(book.id());
                                    if (deleted) {
                                        com.lms.util.ToastUtil.show("Book deleted successfully!");
                                        refreshData(); // reload current state
                                    } else {
                                        com.lms.util.ToastUtil.show("Failed to delete book.");
                                    }
                                }
                            });
                        });
                        
                        actionBox.getChildren().addAll(btnEdit, btnDelete);
                        setGraphic(actionBox);
                    }
                }
            });

            table.getColumns().addAll(colTitle, colAuthor, colIsbn, colCategory, colAvailable, colActions);
        }
        
        loadTableData(pageIndex);
        return table;
    }

    private void loadTableData(int pageIndex) {
        String search = txtSearch.getText();
        BookDAO.Category cat = currentCategoryFilter;
        Integer catId = (cat != null && cat.id() != -1) ? cat.id() : null;

        List<BookDAO.Book> data = dao.getBooks(search, catId, pageIndex * ITEMS_PER_PAGE, ITEMS_PER_PAGE);
        table.getItems().setAll(data);
    }

    private void showBookDialog(BookDAO.Book bookToEdit) {
        Dialog<Boolean> dialog = new Dialog<>();
dialog.initOwner(com.lms.util.Navigator.getStage());
        dialog.setTitle(bookToEdit == null ? "Add Book" : "Edit Book");
        dialog.setHeaderText(null);
        
        // Fix for macOS full screen dialog black screen issue
        if (txtSearch.getScene() != null && txtSearch.getScene().getWindow() != null) {
            dialog.initOwner(txtSearch.getScene().getWindow());
        }

        ButtonType btnSave = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSave, ButtonType.CANCEL);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setPrefWidth(350); // Reduced width

        TextField txtTitle = new TextField();
        txtTitle.setPromptText("Title *");
        
        TextField txtAuthor = new TextField();
        txtAuthor.setPromptText("Author *");
        
        TextField txtIsbn = new TextField();
        txtIsbn.setPromptText("ISBN");
        
        TextField txtCatSearch = new TextField();
        txtCatSearch.setPromptText("Search Category...");
        VBox lvCat = new VBox();
        lvCat.setStyle("-fx-border-color: #d1d5db; -fx-background-color: white; -fx-border-radius: 4; -fx-background-radius: 4;");
        lvCat.setVisible(false);
        lvCat.setManaged(false);
        
        java.util.List<BookDAO.Category> allCats = dao.getCategories();
        
        txtCatSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                lvCat.setVisible(false);
                lvCat.setManaged(false);
            } else {
                java.util.List<BookDAO.Category> filtered = allCats.stream()
                    .filter(c -> c.name().toLowerCase().contains(newVal.trim().toLowerCase()))
                    .limit(1)
                    .toList();
                
                if (filtered.isEmpty()) {
                    lvCat.setVisible(false);
                    lvCat.setManaged(false);
                } else {
                    if (filtered.size() == 1 && filtered.get(0).name().equalsIgnoreCase(newVal.trim())) {
                        lvCat.setVisible(false);
                        lvCat.setManaged(false);
                    } else {
                        lvCat.getChildren().clear();
                        for (BookDAO.Category c : filtered) {
                            javafx.scene.control.Label lbl = new javafx.scene.control.Label(c.name());
                            lbl.setMaxWidth(Double.MAX_VALUE);
                            lbl.setPadding(new javafx.geometry.Insets(5, 10, 5, 10));
                            lbl.setOnMouseEntered(e -> lbl.setStyle("-fx-background-color: -fx-accent; -fx-text-fill: white;"));
                            lbl.setOnMouseExited(e -> lbl.setStyle("-fx-background-color: transparent; -fx-text-fill: -fx-text-base-color;"));
                            lbl.setOnMouseClicked(e -> {
                                txtCatSearch.setText(c.name());
                                lvCat.setVisible(false);
                                lvCat.setManaged(false);
                            });
                            lvCat.getChildren().add(lbl);
                        }
                        lvCat.setVisible(true);
                        lvCat.setManaged(true);
                    }
                }
            }
        });
        
        TextField txtCopies = new TextField();
        txtCopies.setPromptText("Total Copies * (e.g. 1)");
        
        TextField txtYear = new TextField();
        txtYear.setPromptText("Published Year");
        
        TextArea txtDesc = new TextArea();
        txtDesc.setPromptText("Description");
        txtDesc.setPrefRowCount(3);
        txtDesc.setWrapText(true);

        if (bookToEdit != null) {
            txtTitle.setText(bookToEdit.title());
            txtAuthor.setText(bookToEdit.author());
            txtIsbn.setText(bookToEdit.isbn() != null ? bookToEdit.isbn() : "");
            txtCopies.setText(String.valueOf(bookToEdit.totalCopies()));
            if (bookToEdit.publishedYear() != null) txtYear.setText(String.valueOf(bookToEdit.publishedYear()));
            if (bookToEdit.description() != null) txtDesc.setText(bookToEdit.description());
            
            if (bookToEdit.categoryName() != null) {
                txtCatSearch.setText(bookToEdit.categoryName());
            }
        }

        // Add to VBox directly using placeholders
        layout.getChildren().addAll(
            txtTitle, txtAuthor, txtIsbn, txtCatSearch, lvCat, txtCopies, txtYear, txtDesc
        );

        dialog.getDialogPane().setContent(layout);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSave) {
                if (txtTitle.getText().trim().isEmpty() || txtAuthor.getText().trim().isEmpty()) {
                    return false; // Title & Author required
                }
                int copies = 1;
                try {
                    copies = Integer.parseInt(txtCopies.getText().trim());
                } catch (NumberFormatException ignored) {}

                Integer year = null;
                try {
                    year = Integer.parseInt(txtYear.getText().trim());
                } catch (NumberFormatException ignored) {}

                String catName = txtCatSearch.getText().trim();
                Integer catId = null;
                for (BookDAO.Category c : allCats) {
                    if (c.name().equalsIgnoreCase(catName)) {
                        catId = c.id();
                        break;
                    }
                }

                if (bookToEdit == null) {
                    return dao.insertBook(txtTitle.getText(), txtAuthor.getText(), txtIsbn.getText(), catId, copies, year, txtDesc.getText());
                } else {
                    return dao.updateBook(bookToEdit.id(), txtTitle.getText(), txtAuthor.getText(), txtIsbn.getText(), catId, copies, year, txtDesc.getText());
                }
            }
            return null; // Cancel
        });

        dialog.showAndWait().ifPresent(success -> {
            if (success) {
                com.lms.util.ToastUtil.show(bookToEdit == null ? "Book saved successfully!" : "Book updated successfully!");
                refreshData();
            } else {
                com.lms.util.ToastUtil.show("Failed to save book (check required fields).");
            }
        });
    }

    private void showCategoryFilterDialog() {
        javafx.scene.control.Dialog<BookDAO.Category> dialog = new javafx.scene.control.Dialog<>();
        dialog.initOwner(com.lms.util.Navigator.getStage());
        dialog.setTitle("Filter by Category");
        dialog.setHeaderText(null);
        
        TextField txtCatSearch = new TextField();
        txtCatSearch.setPrefWidth(300);
        txtCatSearch.setPromptText("Search Category...");
        VBox lvCat = new VBox();
        lvCat.setStyle("-fx-border-color: #d1d5db; -fx-background-color: white; -fx-border-radius: 4; -fx-background-radius: 4;");
        lvCat.setVisible(false);
        lvCat.setManaged(false);
        
        java.util.List<BookDAO.Category> allCats = new java.util.ArrayList<>();
        allCats.add(new BookDAO.Category(-1, "All Categories"));
        allCats.addAll(dao.getCategories());
        
        txtCatSearch.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.trim().isEmpty()) {
                lvCat.setVisible(false);
                lvCat.setManaged(false);
            } else {
                java.util.List<BookDAO.Category> filtered = allCats.stream()
                    .filter(c -> c.name().toLowerCase().contains(val.trim().toLowerCase()))
                    .limit(1)
                    .toList();
                
                if (filtered.isEmpty()) {
                    lvCat.setVisible(false);
                    lvCat.setManaged(false);
                } else {
                    if (filtered.size() == 1 && filtered.get(0).name().equalsIgnoreCase(val.trim())) {
                        lvCat.setVisible(false);
                        lvCat.setManaged(false);
                    } else {
                        lvCat.getChildren().clear();
                        for (BookDAO.Category c : filtered) {
                            javafx.scene.control.Label lbl = new javafx.scene.control.Label(c.name());
                            lbl.setMaxWidth(Double.MAX_VALUE);
                            lbl.setPadding(new javafx.geometry.Insets(5, 10, 5, 10));
                            lbl.setOnMouseEntered(e -> lbl.setStyle("-fx-background-color: -fx-accent; -fx-text-fill: white;"));
                            lbl.setOnMouseExited(e -> lbl.setStyle("-fx-background-color: transparent; -fx-text-fill: -fx-text-base-color;"));
                            lbl.setOnMouseClicked(e -> {
                                txtCatSearch.setText(c.name());
                                lvCat.setVisible(false);
                                lvCat.setManaged(false);
                            });
                            lvCat.getChildren().add(lbl);
                        }
                        lvCat.setVisible(true);
                        lvCat.setManaged(true);
                    }
                }
            }
        });
        
        VBox layout = new VBox(10, txtCatSearch, lvCat);
        layout.setPadding(new Insets(10));
        layout.setPrefWidth(320);
        layout.setMinHeight(85);
        dialog.getDialogPane().setContent(layout);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String catName = txtCatSearch.getText().trim();
                for (BookDAO.Category c : allCats) {
                    if (c.name().equalsIgnoreCase(catName)) {
                        return c;
                    }
                }
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(cat -> {
            if (cat != null) {
                this.currentCategoryFilter = cat;
                btnCategoryFilter.setText(cat.name());
                refreshData();
            }
        });
    }
}
