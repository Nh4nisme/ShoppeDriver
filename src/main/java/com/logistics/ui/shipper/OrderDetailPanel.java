package com.logistics.ui.shipper;

import com.logistics.model.Order;
import com.logistics.worker.ShipperWorker;
import com.logistics.util.DataChangeListener;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.geometry.Insets;
import javafx.application.Platform;

public class OrderDetailPanel extends VBox implements DataChangeListener {
    private final ShipperWorker shipperWorker;

    public OrderDetailPanel(ShipperWorker shipperWorker) {
        this.shipperWorker = shipperWorker;
        this.setSpacing(10);
        this.setPadding(new Insets(15));
        this.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #cccccc; -fx-border-width: 1;");

        Label titleLabel = new Label("Current Order");
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        this.getChildren().add(titleLabel);

        // Register as listener
        shipperWorker.addListener(this);
        updateOrderDetail();
    }

    @Override
    public void onDataChanged() {
        Platform.runLater(this::updateOrderDetail);
    }

    private void updateOrderDetail() {
        // Keep only the title
        this.getChildren().removeIf(node -> node != this.getChildren().get(0));

        Order currentOrder = shipperWorker.getCurrentOrder();

        if (currentOrder == null) {
            Label noOrderLabel = new Label("No active order");
            noOrderLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #999;");
            this.getChildren().add(noOrderLabel);
        } else {
            Label idLabel = new Label("ID: " + currentOrder.getId());
            idLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold;");

            Label statusLabel = new Label("Status: " + currentOrder.getStatus().getDisplayName());
            statusLabel.setStyle("-fx-font-size: 12;");

            Label coordsLabel = new Label(String.format("Destination: (%.1f, %.1f)", currentOrder.getX(), currentOrder.getY()));
            coordsLabel.setStyle("-fx-font-size: 12;");

            Label shipperLocLabel = new Label(String.format("Your Location: (%.1f, %.1f)",
                    shipperWorker.getShipper().getCurrentX(),
                    shipperWorker.getShipper().getCurrentY()));
            shipperLocLabel.setStyle("-fx-font-size: 12;");

            double distance = shipperWorker.getShipper().distanceTo(currentOrder.getX(), currentOrder.getY());
            Label distanceLabel = new Label(String.format("Distance: %.2f", distance));
            distanceLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold;");

            this.getChildren().addAll(idLabel, statusLabel, coordsLabel, shipperLocLabel, distanceLabel);
        }
    }
}

