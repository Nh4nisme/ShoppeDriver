package com.logistics.ui.admin;

import com.logistics.model.Shipper;
import com.logistics.service.ShipperTrackingService;
import com.logistics.util.DataChangeListener;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.geometry.Insets;
import javafx.application.Platform;

public class ShipperStatusPanel extends BorderPane implements DataChangeListener {
    private final VBox shipperList;
    private final ScrollPane scrollPane;

    public ShipperStatusPanel() {
        this.shipperList = new VBox();
        shipperList.setSpacing(5);
        shipperList.setPadding(new Insets(10));
        shipperList.setStyle("-fx-border-color: #cccccc;");

        this.scrollPane = new ScrollPane(shipperList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-padding: 0;");

        Label titleLabel = new Label("Active Shippers");
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 10;");

        this.setTop(titleLabel);
        this.setCenter(scrollPane);

        // Register as listener
        ShipperTrackingService.getInstance().addListener(this);
        updateShipperList();
    }

    @Override
    public void onDataChanged() {
        Platform.runLater(this::updateShipperList);
    }

    private void updateShipperList() {
        shipperList.getChildren().clear();

        for (Shipper shipper : ShipperTrackingService.getInstance().getAllShippers()) {
            if (shipper.isActive()) {
                VBox shipperCard = createShipperCard(shipper);
                shipperList.getChildren().add(shipperCard);
            }
        }
    }

    private VBox createShipperCard(Shipper shipper) {
        VBox card = new VBox();
        card.setSpacing(3);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-border-color: #dddddd; -fx-border-width: 1; -fx-border-radius: 3;");

        Label nameLabel = new Label(shipper.getName() + " (" + shipper.getId() + ")");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");

        Label statusLabel = new Label("Status: " + shipper.getStatus().getDisplayName());
        statusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #555;");

        Label locationLabel = new Label(String.format("Location: (%.1f, %.1f)", shipper.getCurrentX(), shipper.getCurrentY()));
        locationLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #555;");

        card.getChildren().addAll(nameLabel, statusLabel, locationLabel);
        return card;
    }
}

