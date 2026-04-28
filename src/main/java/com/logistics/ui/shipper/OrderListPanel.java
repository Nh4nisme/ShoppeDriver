package com.logistics.ui.shipper;

import com.logistics.model.Order;
import com.logistics.worker.ShipperWorker;
import com.logistics.util.DataChangeListener;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.geometry.Insets;
import javafx.application.Platform;

public class OrderListPanel extends VBox implements DataChangeListener {
    private final ShipperWorker shipperWorker;
    private final VBox orderList;
    private final ScrollPane scrollPane;

    public OrderListPanel(ShipperWorker shipperWorker) {
        this.shipperWorker = shipperWorker;
        this.orderList = new VBox();
        orderList.setSpacing(5);
        orderList.setPadding(new Insets(10));

        this.scrollPane = new ScrollPane(orderList);
        scrollPane.setFitToWidth(true);

        Label titleLabel = new Label("Assigned Orders");
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 10;");

        this.getChildren().addAll(titleLabel, scrollPane);
        this.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);

        // Register as listener
        shipperWorker.addListener(this);
        updateOrderList();
    }

    @Override
    public void onDataChanged() {
        Platform.runLater(this::updateOrderList);
    }

    private void updateOrderList() {
        orderList.getChildren().clear();

        for (Order order : shipperWorker.getAssignedOrders()) {
            HBox orderRow = new HBox();
            orderRow.setSpacing(10);
            orderRow.setPadding(new Insets(8));
            orderRow.setStyle("-fx-border-color: #dddddd; -fx-border-width: 1;");

            Label idLabel = new Label(String.valueOf(order.getId()));
            idLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 100;");

            Label statusLabel = new Label(order.getStatus().getDisplayName());
            statusLabel.setStyle("-fx-text-fill: " + getStatusColor(order.getStatus().toString()) + ";");

            Label coordsLabel = new Label(String.format("(%.1f, %.1f)", order.getX(), order.getY()));
            coordsLabel.setStyle("-fx-text-fill: #666;");

            orderRow.getChildren().addAll(idLabel, statusLabel, coordsLabel);
            HBox.setHgrow(coordsLabel, javafx.scene.layout.Priority.ALWAYS);

            orderList.getChildren().add(orderRow);
        }

        // Show current order if exists
        Order currentOrder = shipperWorker.getCurrentOrder();
        if (currentOrder != null) {
            HBox currentOrderRow = new HBox();
            currentOrderRow.setSpacing(10);
            currentOrderRow.setPadding(new Insets(8));
            currentOrderRow.setStyle("-fx-border-color: #f39c12; -fx-border-width: 2; -fx-background-color: #fffacd;");

            Label currentLabel = new Label("CURRENT");
            currentLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f39c12;");

            Label idLabel = new Label(String.valueOf(currentOrder.getId()));
            idLabel.setStyle("-fx-font-weight: bold;");

            currentOrderRow.getChildren().addAll(currentLabel, idLabel);
            orderList.getChildren().add(0, currentOrderRow);
        }
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "DONE" -> "#27ae60";
            case "IN_DELIVERY" -> "#e67e22";
            case "PENDING" -> "#95a5a6";
            default -> "#000000";
        };
    }
}

