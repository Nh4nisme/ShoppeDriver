package com.logistics.ui.shipper;

import com.logistics.worker.ShipperWorker;
import javafx.scene.layout.HBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class ControlsPanel extends HBox {
    private final ShipperWorker shipperWorker;
    private final Button startButton;
    private final Button stopButton;
    private final Button deliverNextButton;
    private final Label pendingOrdersLabel;
    private final ProgressBar progressBar;

    public ControlsPanel(ShipperWorker shipperWorker) {
        this.shipperWorker = shipperWorker;
        this.setSpacing(10);
        this.setPadding(new Insets(10));
        this.setStyle("-fx-background-color: #34495e; -fx-border-color: #000; -fx-border-width: 1 0 0 0;");
        this.setAlignment(Pos.CENTER_LEFT);

        // Start button
        this.startButton = new Button("Start Auto-Delivery");
        startButton.setStyle("-fx-font-size: 12; -fx-padding: 8;");
        startButton.setOnAction(e -> shipperWorker.startDelivery());

        // Stop button
        this.stopButton = new Button("Stop Auto-Delivery");
        stopButton.setStyle("-fx-font-size: 12; -fx-padding: 8;");
        stopButton.setOnAction(e -> shipperWorker.stopDelivery());

        // Deliver Next button
        this.deliverNextButton = new Button("Deliver Next (Manual)");
        deliverNextButton.setStyle("-fx-font-size: 12; -fx-padding: 8; -fx-background-color: #e67e22;");
        deliverNextButton.setTextFill(javafx.scene.paint.Color.WHITE);
        deliverNextButton.setOnAction(e -> shipperWorker.deliverNext());

        // Pending orders label
        this.pendingOrdersLabel = new Label("Pending: 0");
        pendingOrdersLabel.setStyle("-fx-font-size: 12; -fx-text-fill: white;");
        pendingOrdersLabel.setPrefWidth(100);

        // Progress bar
        this.progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(150);

        this.getChildren().addAll(startButton, stopButton, deliverNextButton, pendingOrdersLabel, progressBar);

        // Update UI periodically
        setupUpdateTask();
    }

    private void setupUpdateTask() {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(500),
                e -> updateControls()
            )
        );
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }

    private void updateControls() {
        int pending = shipperWorker.getPendingOrderCount();
        pendingOrdersLabel.setText("Pending: " + pending);

        startButton.setDisable(pending == 0 || shipperWorker.isAutoMode());
        stopButton.setDisable(!shipperWorker.isAutoMode());
        deliverNextButton.setDisable(pending == 0);

        if (pending > 0) {
            progressBar.setProgress(1.0 - (double) pending / 10);
        } else {
            progressBar.setProgress(0);
        }
    }
}

