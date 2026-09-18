package com.lms.ui;

import com.lms.db.CategoryDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;

import java.util.List;

public class CategoryView {

    private final CategoryDAO dao;
    private static final int ITEMS_PER_PAGE = 15;
    
    private TextField txtSearch;
    private ComboBox<String> cbStatusFilter;
    private Pagination pagination;
    private TableView<CategoryDAO.Category> table;

    public CategoryView() {
        this.dao = new CategoryDAO();
    }

    public Node build() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        // Title
        Label lblTitle = new Label("Categories Management");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #18181b;");

        // Top Controls
        HBox topControls = new HBox(12);
        topControls.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new TextField();
        txtSearch.setPromptText("Search Categories...");
        txtSearch.setPrefWidth(250);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> refreshData());

        cbStatusFilter = new ComboBox<>();
        cbStatusFilter.getItems().addAll("All Statuses", "Active", "Inactive");
        cbStatusFilter.getSelectionModel().selectFirst();
        cbStatusFilter.setOnAction(e -> refreshData());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = new Button("+ Add Category");
        btnAdd.setOnAction(e -> showCategoryDialog(null));

        topControls.getChildren().addAll(txtSearch, cbStatusFilter, spacer, btnAdd);

        // Pagination and Table setup
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
        int totalItems = dao.getCategoriesCount(search, statusFilter);
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
            
            TableColumn<CategoryDAO.Category, String> colName = new TableColumn<>("Name");
            colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
            
            TableColumn<CategoryDAO.Category, String> colDesc = new TableColumn<>("Description");
            colDesc.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().description()));
            
            // Status Column with Dot Indicator
            TableColumn<CategoryDAO.Category, Void> colStatus = new TableColumn<>("Status");
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
                        CategoryDAO.Category cat = getTableRow().getItem();
                        HBox box = new HBox(6);
                        box.setAlignment(Pos.CENTER);
                        
                        Circle dot = new Circle(4);
                        dot.setFill(cat.isActive() ? Color.web("#22c55e") : Color.web("#9ca3af"));
                        
                        Label lbl = new Label(cat.isActive() ? "Active" : "Inactive");
                        lbl.setStyle("-fx-text-fill: #3f3f46;");
                        
                        box.getChildren().addAll(dot, lbl);
                        setGraphic(box);
                    }
                }
            });
            
            // Actions Column
            TableColumn<CategoryDAO.Category, Void> colActions = new TableColumn<>("Actions");
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
                        CategoryDAO.Category cat = getTableRow().getItem();
                        HBox actionBox = new HBox(8);
                        actionBox.setAlignment(Pos.CENTER);
                        
                        Button btnEdit = new Button("Edit");
                        btnEdit.setOnAction(e -> showCategoryDialog(cat));
                        
                        Button btnDelete = new Button("Delete");
                        btnDelete.setOnAction(e -> {
                            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete '" + cat.name() + "'?", ButtonType.YES, ButtonType.NO);
                            confirm.setHeaderText("Confirm Deletion");
                            if (txtSearch.getScene() != null && txtSearch.getScene().getWindow() != null) {
                                confirm.initOwner(txtSearch.getScene().getWindow());
                            }
                            confirm.showAndWait().ifPresent(response -> {
                                if (response == ButtonType.YES) {
                                    boolean deleted = dao.deleteCategory(cat.id());
                                    if (deleted) {
                                        com.lms.util.ToastUtil.show("Category deleted successfully!");
                                        refreshData();
                                    } else {
                                        com.lms.util.ToastUtil.show("Failed to delete category! Ensure no books are using it.");
                                    }
                                }
                            });
                        });
                        
                        actionBox.getChildren().addAll(btnEdit, btnDelete);
                        setGraphic(actionBox);
                    }
                }
            });

            table.getColumns().addAll(colName, colDesc, colStatus, colActions);
        }
        
        loadTableData(pageIndex);
        return table;
    }

    private void loadTableData(int pageIndex) {
        String search = txtSearch.getText();
        Integer statusFilter = getStatusFilter();
        List<CategoryDAO.Category> data = dao.getCategories(search, statusFilter, pageIndex * ITEMS_PER_PAGE, ITEMS_PER_PAGE);
        table.getItems().setAll(data);
    }

    private void showCategoryDialog(CategoryDAO.Category catToEdit) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(catToEdit == null ? "Add Category" : "Edit Category");
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
        txtName.setPromptText("Category Name *");
        
        TextArea txtDesc = new TextArea();
        txtDesc.setPromptText("Description");
        txtDesc.setPrefRowCount(3);
        txtDesc.setWrapText(true);
        
        ComboBox<String> cbStatus = new ComboBox<>();
        cbStatus.getItems().addAll("Active", "Inactive");
        cbStatus.setPromptText("Status");
        cbStatus.setMaxWidth(Double.MAX_VALUE);
        cbStatus.getSelectionModel().selectFirst();

        if (catToEdit != null) {
            txtName.setText(catToEdit.name());
            txtDesc.setText(catToEdit.description() != null ? catToEdit.description() : "");
            cbStatus.getSelectionModel().select(catToEdit.isActive() ? "Active" : "Inactive");
        }

        layout.getChildren().addAll(txtName, txtDesc, cbStatus);
        dialog.getDialogPane().setContent(layout);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSave) {
                if (txtName.getText().trim().isEmpty()) {
                    return false; 
                }
                
                boolean isActive = "Active".equals(cbStatus.getValue());

                if (catToEdit == null) {
                    return dao.insertCategory(txtName.getText(), txtDesc.getText(), isActive);
                } else {
                    return dao.updateCategory(catToEdit.id(), txtName.getText(), txtDesc.getText(), isActive);
                }
            }
            return null; // Cancel
        });

        dialog.showAndWait().ifPresent(success -> {
            if (success) {
                com.lms.util.ToastUtil.show(catToEdit == null ? "Category saved successfully!" : "Category updated successfully!");
                refreshData();
            } else {
                com.lms.util.ToastUtil.show("Failed to save category. Check for duplicate names.");
            }
        });
    }
}
