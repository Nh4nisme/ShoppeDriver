package com.logistics.ui.admin;

import com.logistics.model.Batch;
import com.logistics.service.ShipperTrackingService;
import com.logistics.util.DataChangeListener;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.application.Platform;

public class Sidebar extends VBox implements DataChangeListener {
    private final VBox batchList;
    private final ScrollPane scrollPane;
    private final BatchAssignmentPanel assignmentPanel;
    private final TabPane tabPane;

    public Sidebar() {
        this.setPrefWidth(380);
        this.setStyle("-fx-background-color: #ecf0f1;");

        Label titleLabel = new Label("Quản lý Batch");
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-padding: 10;");

        // Create tabs
        this.tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tab 1: Batch Creation
        Tab creationTab = new Tab();
        creationTab.setText("Tạo Batch");
        creationTab.setContent(new BatchCreationPanel());

        // Tab 2: Batch Assignment
        Tab assignmentTab = new Tab();
        assignmentTab.setText("Gán Batch");
        this.assignmentPanel = new BatchAssignmentPanel();
        assignmentTab.setContent(assignmentPanel);

        // Tab 3: Batch List
        Tab listTab = new Tab();
        listTab.setText("Danh sách Batch");
        
        VBox listContent = new VBox();
        this.batchList = new VBox();
        batchList.setSpacing(8);
        batchList.setPadding(new Insets(10));

        this.scrollPane = new ScrollPane(batchList);
        scrollPane.setFitToWidth(true);

        HBox controlsBox = createControlsBox();

        listContent.getChildren().addAll(scrollPane, controlsBox);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        listTab.setContent(listContent);

        tabPane.getTabs().addAll(creationTab, assignmentTab, listTab);

        this.getChildren().addAll(titleLabel, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Register as listener
        ShipperTrackingService.getInstance().addListener(this);
        updateBatchList();
    }

    private HBox createControlsBox() {
        HBox box = new HBox();
        box.setSpacing(5);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> {
            updateBatchList();
            if (assignmentPanel != null) {
                assignmentPanel.refresh();
            }
        });
        refreshButton.setPrefWidth(100);

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> {
            ShipperTrackingService.getInstance().clearListeners();
            updateBatchList();
        });
        clearButton.setPrefWidth(100);

        box.getChildren().addAll(refreshButton, clearButton);
        return box;
    }

    @Override
    public void onDataChanged() {
        Platform.runLater(() -> {
            updateBatchList();
            if (assignmentPanel != null) {
                assignmentPanel.refresh();
            }
        });
    }

    private void updateBatchList() {
        batchList.getChildren().clear();

        for (Batch batch : ShipperTrackingService.getInstance().getAllBatches()) {
            VBox batchCard = createBatchCard(batch);
            batchList.getChildren().add(batchCard);
        }
    }

    private VBox createBatchCard(Batch batch) {
        VBox card = new VBox();
        card.setSpacing(3);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-border-color: #3498db; -fx-border-width: 2; -fx-border-radius: 3; -fx-background-color: #ffffff;");

        Label idLabel = new Label(String.valueOf(batch.getId()));
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");

        Label statusLabel = new Label("Status: " + batch.getStatus().getDisplayName());
        statusLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");

        Label progressLabel = new Label("Progress: " + batch.getDeliveredCount() + "/" + batch.getOrderCount());
        progressLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");

        Label shipperLabel = new Label(
                "Shipper: " + (batch.getShipperId() != 0 ? batch.getShipperId() : "Chưa gán")
        );
        shipperLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");

        Label modeLabel = new Label("(User-Driven)");
        modeLabel.setStyle("-fx-font-size: 9; -fx-text-fill: #9C27B0; -fx-font-weight: bold;");

        card.getChildren().addAll(idLabel, modeLabel, statusLabel, progressLabel, shipperLabel);
        return card;
    }
}

