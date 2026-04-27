package com.logistics.ui.admin;

import com.logistics.model.Batch;
import com.logistics.service.ShipperTrackingService;
import com.logistics.util.DataChangeListener;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.application.Platform;

public class Sidebar extends VBox implements DataChangeListener {
    private final VBox batchList;
    private final ScrollPane scrollPane;

    public Sidebar() {
        this.setPrefWidth(250);
        this.setStyle("-fx-background-color: #ecf0f1;");

        Label titleLabel = new Label("Batches");
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-padding: 10;");

        this.batchList = new VBox();
        batchList.setSpacing(8);
        batchList.setPadding(new Insets(10));

        this.scrollPane = new ScrollPane(batchList);
        scrollPane.setFitToWidth(true);

        HBox controlsBox = createControlsBox();

        this.getChildren().addAll(titleLabel, scrollPane, controlsBox);
        this.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);

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
        refreshButton.setOnAction(e -> updateBatchList());
        refreshButton.setPrefWidth(100);

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> {
            ShipperTrackingService.getInstance().clear();
            updateBatchList();
        });
        clearButton.setPrefWidth(100);

        box.getChildren().addAll(refreshButton, clearButton);
        return box;
    }

    @Override
    public void onDataChanged() {
        Platform.runLater(this::updateBatchList);
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

        Label idLabel = new Label(batch.getId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");

        Label statusLabel = new Label("Status: " + batch.getStatus().getDisplayName());
        statusLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");

        Label progressLabel = new Label("Progress: " + batch.getDeliveredCount() + "/" + batch.getOrderCount());
        progressLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");

        Label shipperLabel = new Label("Shipper: " + (batch.getShipperId() != null ? batch.getShipperId() : "Unassigned"));
        shipperLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");

        card.getChildren().addAll(idLabel, statusLabel, progressLabel, shipperLabel);
        return card;
    }
}

