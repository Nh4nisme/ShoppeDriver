package com.logistics.ui.admin;

import com.logistics.model.Batch;
import com.logistics.model.Order;
import com.logistics.model.Shipper;
import com.logistics.service.ShipperTrackingService;
import com.logistics.util.DataChangeListener;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class FollowBatchPanel extends HBox implements DataChangeListener {
    private final ShipperTrackingService trackingService;
    private final VBox shipperListBox;
    private final VBox detailBox;
    private Integer selectedShipperId;

    public FollowBatchPanel() {
        this.trackingService = ShipperTrackingService.getInstance();
        this.shipperListBox = new VBox(6);
        this.detailBox = new VBox(8);

        setSpacing(12);
        setPadding(new Insets(10));

        VBox shipperSection = createShipperSection();
        VBox detailSection = createDetailSection();

        getChildren().addAll(shipperSection, detailSection);
        HBox.setHgrow(shipperSection, Priority.ALWAYS);
        HBox.setHgrow(detailSection, Priority.ALWAYS);

        trackingService.addListener(this);
        refresh();
    }

    private VBox createShipperSection() {
        VBox box = new VBox(8);
        box.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 8; -fx-background-color: #f9f9f9;");

        Label title = new Label("Shipper");
        title.setStyle("-fx-font-weight: bold;");

        ScrollPane scrollPane = new ScrollPane(shipperListBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(280);

        box.getChildren().addAll(title, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return box;
    }

    private VBox createDetailSection() {
        VBox box = new VBox(8);
        box.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 8; -fx-background-color: #ffffff;");

        Label title = new Label("Batch da gan");
        title.setStyle("-fx-font-weight: bold;");

        detailBox.getChildren().add(createMutedLabel("Chon mot shipper de xem batch va order."));

        ScrollPane scrollPane = new ScrollPane(detailBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(280);

        box.getChildren().addAll(title, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return box;
    }

    private void refresh() {
        renderShippers(trackingService.getAllShippers());
        if (selectedShipperId != null) {
            renderBatchDetail(trackingService.getShipper(selectedShipperId));
        }
    }

    private void renderShippers(List<Shipper> shippers) {
        shipperListBox.getChildren().clear();

        if (shippers.isEmpty()) {
            shipperListBox.getChildren().add(createMutedLabel("Khong co shipper."));
            return;
        }

        for (Shipper shipper : shippers) {
            shipperListBox.getChildren().add(createShipperCard(shipper));
        }
    }

    private VBox createShipperCard(Shipper shipper) {
        VBox card = new VBox(4);
        String borderColor = selectedShipperId != null && selectedShipperId == shipper.getId() ? "#1abc9c" : "#4CAF50";
        card.setStyle("-fx-border-color: " + borderColor + "; -fx-border-width: 1; -fx-padding: 8; -fx-background-color: #ffffff;");
        card.setOnMouseClicked(event -> {
            selectedShipperId = shipper.getId();
            renderBatchDetail(shipper);
            renderShippers(trackingService.getAllShippers());
        });

        Label nameLabel = new Label(shipper.getName() + " (#" + shipper.getId() + ")");
        nameLabel.setStyle("-fx-font-weight: bold;");

        Batch activeBatch = trackingService.getActiveBatchForShipper(shipper.getId());
        Label statusLabel = new Label("Status: " + shipper.getStatus().getDisplayName());
        statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #555;");

        Label batchLabel = new Label(activeBatch != null
                ? "Batch: #" + activeBatch.getId() + " - " + activeBatch.getStatus().getDisplayName()
                : "Batch: Chua co");
        batchLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #555;");

        card.getChildren().addAll(nameLabel, statusLabel, batchLabel);
        return card;
    }

    private void renderBatchDetail(Shipper shipper) {
        detailBox.getChildren().clear();
        if (shipper == null) {
            detailBox.getChildren().add(createMutedLabel("Khong tim thay shipper."));
            return;
        }

        Batch activeBatch = trackingService.getActiveBatchForShipper(shipper.getId());
        Label shipperTitle = new Label(shipper.getName() + " (#" + shipper.getId() + ")");
        shipperTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        detailBox.getChildren().add(shipperTitle);

        if (activeBatch == null) {
            detailBox.getChildren().add(createMutedLabel("Shipper nay chua co batch duoc gan."));
            return;
        }

        Label batchMeta = new Label("Batch #" + activeBatch.getId()
                + " | Status: " + activeBatch.getStatus().getDisplayName()
                + " | Orders: " + activeBatch.getOrderCount());
        batchMeta.setStyle("-fx-font-size: 10px; -fx-text-fill: #555;");
        detailBox.getChildren().add(batchMeta);

        if (activeBatch.getOrders().isEmpty()) {
            detailBox.getChildren().add(createMutedLabel("Batch nay khong co order."));
            return;
        }

        for (Order order : activeBatch.getOrders()) {
            detailBox.getChildren().add(createOrderCard(order));
        }
    }

    private VBox createOrderCard(Order order) {
        VBox card = new VBox(3);
        card.setStyle("-fx-border-color: #dddddd; -fx-border-width: 1; -fx-padding: 8; -fx-background-color: #fafafa;");

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label idLabel = new Label("Order #" + order.getId());
        idLabel.setStyle("-fx-font-weight: bold;");
        Label statusLabel = new Label(order.getStatus().getDisplayName());
        statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #555;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.getChildren().addAll(idLabel, spacer, statusLabel);

        Label addressLabel = new Label(order.getAddress() == null || order.getAddress().isBlank()
                ? "(No address)"
                : order.getAddress());
        addressLabel.setWrapText(true);

        Label coordsLabel = new Label("(" + String.format("%.5f", order.getLatitude()) + ", "
                + String.format("%.5f", order.getLongitude()) + ")");
        coordsLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");

        card.getChildren().addAll(top, addressLabel, coordsLabel);
        return card;
    }

    private Label createMutedLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #777;");
        return label;
    }

    @Override
    public void onDataChanged() {
        Platform.runLater(this::refresh);
    }
}
