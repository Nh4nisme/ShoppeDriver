package com.logistics.ui.admin;

import com.logistics.model.Batch;
import com.logistics.model.BatchStatus;
import com.logistics.model.Shipper;
import com.logistics.service.DispatcherService;
import com.logistics.service.ShipperTrackingService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.application.Platform;

import java.util.List;

public class BatchAssignmentPanel extends VBox {
    private final DispatcherService dispatcherService;
    private final ShipperTrackingService trackingService;
    private VBox batchListBox;
    private VBox shipperListBox;
    private final Label statusLabel;
    private Batch selectedBatch;
    private Shipper selectedShipper;

    public BatchAssignmentPanel() {
        this.dispatcherService = DispatcherService.getInstance();
        this.trackingService = ShipperTrackingService.getInstance();
        this.setPrefHeight(350);
        this.setStyle("-fx-background-color: #e2e8f0; -fx-padding: 15;");

        // Title
        Label titleLabel = new Label("Gán Batch cho Shipper (User-Driven)");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        // Main content
        HBox contentBox = createContentBox();
        ScrollPane mainScroll = new ScrollPane(contentBox);
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(mainScroll, Priority.ALWAYS);

        // Status label
        statusLabel = new Label("Chọn batch và shipper để gán");
        statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #000000;");

        this.getChildren().addAll(titleLabel, mainScroll, statusLabel);
        this.setSpacing(10);

        // Refresh batches and shippers
        refreshUI();
    }

    private HBox createContentBox() {
        HBox box = new HBox(15);
        box.setPadding(new Insets(10));

        // Left: Batch list
        VBox batchSection = createBatchSection();
        HBox.setHgrow(batchSection, Priority.ALWAYS);

        // Right: Shipper list
        VBox shipperSection = createShipperSection();
        HBox.setHgrow(shipperSection, Priority.ALWAYS);

        box.getChildren().addAll(batchSection, shipperSection);
        return box;
    }

    private VBox createBatchSection() {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: #e9ecef; -fx-border-color: #ced4da; -fx-border-width: 1; -fx-padding: 10; -fx-background-radius: 5; -fx-border-radius: 5;");

        Label label = new Label("Batch (Chờ Gán)");
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #000000;");

        batchListBox = new VBox(5);
        batchListBox.setPadding(new Insets(5));

        ScrollPane scrollPane = new ScrollPane(batchListBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(250);

        Button refreshButton = new Button("Refresh");
        refreshButton.setPrefWidth(80);
        refreshButton.setStyle("-fx-font-size: 10px;");
        refreshButton.setOnAction(e -> refreshBatches());

        box.getChildren().addAll(label, scrollPane, refreshButton);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return box;
    }

    private VBox createShipperSection() {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: #e9ecef; -fx-border-color: #ced4da; -fx-border-width: 1; -fx-padding: 10; -fx-background-radius: 5; -fx-border-radius: 5;");

        Label label = new Label("Shipper (Sẵn Sàng)");
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #000000;");

        shipperListBox = new VBox(5);
        shipperListBox.setPadding(new Insets(5));

        ScrollPane scrollPane = new ScrollPane(shipperListBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(250);

        Button assignButton = new Button("Gán");
        assignButton.setPrefWidth(80);
        assignButton.setStyle("-fx-font-size: 10px; -fx-background-color: #2196F3; -fx-text-fill: white;");
        assignButton.setOnAction(e -> handleAssignBatch());

        box.getChildren().addAll(label, scrollPane, assignButton);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return box;
    }

    private void refreshUI() {
        refreshBatches();
        refreshShippers();
    }

    private void refreshBatches() {
        batchListBox.getChildren().clear();

        List<Batch> createdBatches = trackingService.getAllBatches();
        createdBatches = createdBatches.stream()
                .filter(b -> b.getStatus() == BatchStatus.CREATED)
                .toList();

        if (createdBatches.isEmpty()) {
            Label emptyLabel = new Label("Không có batch chờ gán");
            emptyLabel.setStyle("-fx-text-fill: #000000;");
            batchListBox.getChildren().add(emptyLabel);
            return;
        }

        for (Batch batch : createdBatches) {
            HBox batchCard = createBatchCard(batch);
            batchListBox.getChildren().add(batchCard);
        }
    }

    private void refreshShippers() {
        shipperListBox.getChildren().clear();

        List<Shipper> shippers = dispatcherService.getAvailableShippers();

        if (shippers.isEmpty()) {
            Label emptyLabel = new Label("Không có shipper sẵn sàng");
            emptyLabel.setStyle("-fx-text-fill: #000000;");
            shipperListBox.getChildren().add(emptyLabel);
            return;
        }

        for (Shipper shipper : shippers) {
            HBox shipperCard = createShipperCard(shipper);
            shipperListBox.getChildren().add(shipperCard);
        }
    }

    private HBox createBatchCard(Batch batch) {
        HBox card = new HBox(8);
        card.setStyle("-fx-border-color: #3498db; -fx-border-width: 1; -fx-padding: 8; -fx-background-color: #ffffff;");
        card.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("Batch #" + batch.getId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #000000;");
        idLabel.setPrefWidth(70);

        Label countLabel = new Label(batch.getOrderCount() + " đơn");
        countLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #000000;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        RadioButton selectRadio = new RadioButton();
        selectRadio.setOnAction(e -> selectedBatch = batch);

        card.getChildren().addAll(selectRadio, idLabel, countLabel, spacer);
        return card;
    }

    private HBox createShipperCard(Shipper shipper) {
        HBox card = new HBox(8);
        card.setStyle("-fx-border-color: #4CAF50; -fx-border-width: 1; -fx-padding: 8; -fx-background-color: #ffffff;");
        card.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(shipper.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #000000;");
        nameLabel.setPrefWidth(80);

        Label locationLabel = new Label("(" + String.format("%.1f", shipper.getCurrentX()) + ", " + String.format("%.1f", shipper.getCurrentY()) + ")");
        locationLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #000000;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        RadioButton selectRadio = new RadioButton();
        selectRadio.setOnAction(e -> selectedShipper = shipper);

        card.getChildren().addAll(selectRadio, nameLabel, locationLabel, spacer);
        return card;
    }

    private void handleAssignBatch() {
        if (selectedBatch == null) {
            statusLabel.setText("✗ Vui lòng chọn batch");
            statusLabel.setStyle("-fx-text-fill: #f44336;");
            return;
        }

        if (selectedShipper == null) {
            statusLabel.setText("✗ Vui lòng chọn shipper");
            statusLabel.setStyle("-fx-text-fill: #f44336;");
            return;
        }

        statusLabel.setText("Đang gán batch...");
        statusLabel.setStyle("-fx-text-fill: #FF9800;");

        // Use final variables for lambda
        final Batch batch = selectedBatch;
        final Shipper shipper = selectedShipper;

        // Assign on background thread
        new Thread(() -> {
            boolean success = dispatcherService.assignBatchToShipper(batch.getId(), shipper.getId());
            Platform.runLater(() -> {
                if (success) {
                    statusLabel.setText("✓ Gán batch " + batch.getId() + " cho " + shipper.getName() + " thành công!");
                    statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                    selectedBatch = null;
                    selectedShipper = null;
                    refreshUI();
                } else {
                    statusLabel.setText("✗ Lỗi khi gán batch");
                    statusLabel.setStyle("-fx-text-fill: #f44336;");
                }
            });
        }).start();
    }

    public void refresh() {
        Platform.runLater(this::refreshUI);
    }
}


