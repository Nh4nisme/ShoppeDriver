package com.logistics.ui.shipper;

import com.logistics.worker.ShipperWorker;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.geometry.Insets;

public class ShipperAppView extends BorderPane {
    private final ShipperWorker shipperWorker;
    private final OrderListPanel orderListPanel;
    private final OrderDetailPanel orderDetailPanel;
    private final ControlsPanel controlsPanel;

    public ShipperAppView(ShipperWorker shipperWorker) {
        this.shipperWorker = shipperWorker;

        // Create panels
        this.orderListPanel = new OrderListPanel(shipperWorker);
        this.orderDetailPanel = new OrderDetailPanel(shipperWorker);
        this.controlsPanel = new ControlsPanel(shipperWorker);

        // Top: Shipper info
        VBox topBox = createTopBox();
        this.setTop(topBox);

        // Center: Order list on left, Order detail on right
        HBox centerBox = new HBox();
        centerBox.getChildren().addAll(orderListPanel, orderDetailPanel);
        HBox.setHgrow(orderListPanel, javafx.scene.layout.Priority.ALWAYS);
        orderDetailPanel.setPrefWidth(300);
        this.setCenter(centerBox);

        // Bottom: Controls
        this.setBottom(controlsPanel);
    }

    private VBox createTopBox() {
        VBox box = new VBox();
        box.setSpacing(5);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white;");

        Label nameLabel = new Label(shipperWorker.getShipper().getName());
        nameLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: white;");

        Label idLabel = new Label("ID: " + shipperWorker.getShipper().getId());
        idLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #ecf0f1;");

        Label statusLabel = new Label("Status: " + shipperWorker.getShipper().getStatus().getDisplayName());
        statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #ecf0f1;");

        box.getChildren().addAll(nameLabel, idLabel, statusLabel);
        return box;
    }
}

