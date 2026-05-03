package com.logistics.ui.admin;

import com.logistics.service.ShipperTrackingService;
import com.logistics.util.DataChangeEvent;
import com.logistics.util.DataChangeListener;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.application.Platform;

public class KPIBar extends HBox implements DataChangeListener {
    private final Label totalOrdersLabel;
    private final Label inProgressLabel;
    private final Label completedLabel;
    private final Label totalShippersLabel;

    public KPIBar() {
        this.setStyle("-fx-background-color: #2c3e50; -fx-padding: 10;");
        this.setSpacing(30);
        this.setPadding(new Insets(10));

        totalOrdersLabel = createKPILabel("Total Orders: 0");
        inProgressLabel = createKPILabel("In Progress: 0");
        completedLabel = createKPILabel("Completed: 0");
        totalShippersLabel = createKPILabel("Shippers: 0");

        this.getChildren().addAll(totalOrdersLabel, inProgressLabel, completedLabel, totalShippersLabel);

        // Register as listener
        ShipperTrackingService.getInstance().addListener(this);
        updateKPIs();
    }

    private Label createKPILabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");
        return label;
    }

    @Override
    public void onDataChanged(DataChangeEvent event) {
        if (event == null) {
            return;
        }
        if (!event.isType(DataChangeEvent.SHIPPER_LOCATION_UPDATED)
                && !event.isType(DataChangeEvent.BATCH_UPDATED)
                && !event.isType(DataChangeEvent.DATA_CHANGED)) {
            return;
        }
        Platform.runLater(this::updateKPIs);
    }

    private void updateKPIs() {
        ShipperTrackingService trackingService = ShipperTrackingService.getInstance();

        long totalOrders = trackingService.getAllBatches().stream()
                .mapToLong(b -> b.getOrders().size())
                .sum();

        long completed = trackingService.getAllBatches().stream()
                .mapToLong(com.logistics.model.Batch::getDeliveredCount)
                .sum();

        long inProgress = totalOrders - completed;

        totalOrdersLabel.setText("Total Orders: " + totalOrders);
        inProgressLabel.setText("In Progress: " + inProgress);
        completedLabel.setText("Completed: " + completed);
        totalShippersLabel.setText("Shippers: " + trackingService.getAllShippers().size());
    }
}

