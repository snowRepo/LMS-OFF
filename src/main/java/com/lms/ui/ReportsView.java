package com.lms.ui;

import com.lms.util.ReportExporter;
import com.lms.util.ToastUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.application.Platform;

import java.io.File;
import java.time.format.DateTimeFormatter;

public class ReportsView {

    private DatePicker dpFromDate;
    private DatePicker dpToDate;

    public Node build() {
        VBox root = new VBox(24);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        // Title and Description
        VBox headerBox = new VBox(8);
        Label lblTitle = new Label("Reports & Exports");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #18181b;");
        Label lblSub = new Label("Download system data as formatted Excel (.xlsx) spreadsheets.");
        lblSub.setStyle("-fx-text-fill: #71717a;");
        headerBox.getChildren().addAll(lblTitle, lblSub);

        // Date Filters
        HBox filterBox = new HBox(12);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        
        dpFromDate = new DatePicker();
        dpFromDate.setPromptText("From Date");
        
        dpToDate = new DatePicker();
        dpToDate.setPromptText("To Date");
        
        filterBox.getChildren().addAll(dpFromDate, dpToDate);

        // Reports Grid
        FlowPane grid = new FlowPane();
        grid.setHgap(20);
        grid.setVgap(20);
        
        grid.getChildren().addAll(
            createReportCard("Books Catalog", "Full book inventory. (Always exports all records)", () -> exportBooks()),
            createReportCard("Members Roster", "Full member list. (Always exports all records)", () -> exportMembers()),
            createReportCard("Circulation History", "Borrow and return records. (Respects date filter)", () -> exportCirculation()),
            createReportCard("Attendance Logs", "Check-in and check-out logs. (Respects date filter)", () -> exportAttendance()),
            createReportCard("Activity Logs", "System audit trails and user actions. (Respects date filter)", () -> exportActivityLogs()),
            createReportCard("All-in-One Data Dump", "All 5 reports on separate sheets. (Respects date filter)", () -> exportAll())
        );

        root.getChildren().addAll(headerBox, filterBox, grid);
        
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #f0f0f2; -fx-border-color: transparent;");
        
        return scrollPane;
    }

    private Node createReportCard(String title, String description, Runnable onDownload) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setPrefWidth(350);
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e4e4e7; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #18181b;");
        
        Label lblDesc = new Label(description);
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-text-fill: #52525b; -fx-font-size: 13px;");
        lblDesc.setPrefHeight(40);
        
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnDownload = new Button("Download");
        btnDownload.setMaxWidth(Double.MAX_VALUE);
        btnDownload.setOnAction(e -> onDownload.run());

        card.getChildren().addAll(lblTitle, lblDesc, spacer, btnDownload);
        return card;
    }

    private void exportBooks() {
        File file = showSaveDialog("Books_Report");
        if (file != null) {
            if (ReportExporter.exportBooksReport(file)) {
                Platform.runLater(() -> ToastUtil.show("Books report exported successfully!"));
            } else {
                Platform.runLater(() -> ToastUtil.show("Failed to export Books report."));
            }
        }
    }

    private void exportMembers() {
        File file = showSaveDialog("Members_Report");
        if (file != null) {
            if (ReportExporter.exportMembersReport(file)) {
                Platform.runLater(() -> ToastUtil.show("Members report exported successfully!"));
            } else {
                Platform.runLater(() -> ToastUtil.show("Failed to export Members report."));
            }
        }
    }

    private void exportCirculation() {
        File file = showSaveDialog("Circulation_Report");
        if (file != null) {
            String from = dpFromDate.getValue() != null ? dpFromDate.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
            String to = dpToDate.getValue() != null ? dpToDate.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
            if (ReportExporter.exportCirculationReport(file, from, to)) {
                Platform.runLater(() -> ToastUtil.show("Circulation report exported successfully!"));
            } else {
                Platform.runLater(() -> ToastUtil.show("Failed to export Circulation report."));
            }
        }
    }

    private void exportAttendance() {
        File file = showSaveDialog("Attendance_Report");
        if (file != null) {
            String from = dpFromDate.getValue() != null ? dpFromDate.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
            String to = dpToDate.getValue() != null ? dpToDate.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
            if (ReportExporter.exportAttendanceReport(file, from, to)) {
                Platform.runLater(() -> ToastUtil.show("Attendance report exported successfully!"));
            } else {
                Platform.runLater(() -> ToastUtil.show("Failed to export Attendance report."));
            }
        }
    }

    private void exportActivityLogs() {
        File file = showSaveDialog("Activity_Logs_Report");
        if (file != null) {
            String from = dpFromDate.getValue() != null ? dpFromDate.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
            String to = dpToDate.getValue() != null ? dpToDate.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
            if (ReportExporter.exportActivityLogsReport(file, from, to)) {
                Platform.runLater(() -> ToastUtil.show("Activity Logs exported successfully!"));
            } else {
                Platform.runLater(() -> ToastUtil.show("Failed to export Activity Logs."));
            }
        }
    }

    private void exportAll() {
        File file = showSaveDialog("All_Data_Report");
        if (file != null) {
            String from = dpFromDate.getValue() != null ? dpFromDate.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
            String to = dpToDate.getValue() != null ? dpToDate.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
            if (ReportExporter.exportAllDataReport(file, from, to)) {
                Platform.runLater(() -> ToastUtil.show("All-in-One report exported successfully!"));
            } else {
                Platform.runLater(() -> ToastUtil.show("Failed to export All-in-One report."));
            }
        }
    }

    private File showSaveDialog(String defaultName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Excel Report");
        fileChooser.setInitialFileName(defaultName + "_" + java.time.LocalDate.now().toString() + ".xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files (*.xlsx)", "*.xlsx"));
        
        if (dpFromDate.getScene() != null && dpFromDate.getScene().getWindow() != null) {
            return fileChooser.showSaveDialog(dpFromDate.getScene().getWindow());
        }
        return null;
    }
}
