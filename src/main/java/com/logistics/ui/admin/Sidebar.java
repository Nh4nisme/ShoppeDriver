package com.logistics.ui.admin;

import com.logistics.model.Batch;
import com.logistics.model.BatchStatus;
import com.logistics.model.Order;
import com.logistics.service.ShipperTrackingService;
import com.logistics.util.DataChangeEvent;
import com.logistics.util.DataChangeListener;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class Sidebar extends VBox implements DataChangeListener {
    private final VBox batchList;
    private final ScrollPane scrollPane;
    private final BatchAssignmentPanel assignmentPanel;
    private final FollowBatchPanel followBatchPanel;
    private final TabPane tabPane;
    private final VBox batchDetailBox;
    private final ComboBox<String> batchFilter;
    private Integer selectedBatchId;

    public Sidebar() {
        this.setPrefWidth(500);
        this.setMinWidth(500);
        this.setStyle("-fx-background-color: #ecf0f1;");

        Label titleLabel = new Label("Quan ly Batch");
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-padding: 10;");

        this.tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab creationTab = new Tab();
        creationTab.setText("Tao Batch");
        creationTab.setContent(new BatchCreationPanel());

        Tab assignmentTab = new Tab();
        assignmentTab.setText("Gan Batch");
        this.assignmentPanel = new BatchAssignmentPanel();
        assignmentTab.setContent(assignmentPanel);

        Tab followTab = new Tab();
        followTab.setText("Theo doi Batch");
        this.followBatchPanel = new FollowBatchPanel();
        followTab.setContent(followBatchPanel);

        Tab chatTab = new Tab();
        chatTab.setText("Chat");
        chatTab.setContent(new AdminChatPanel());

        Tab listTab = new Tab();
        listTab.setText("Danh sach Batch");

        VBox listContent = new VBox(10);
        listContent.setPadding(new Insets(10));

        this.batchFilter = new ComboBox<>(FXCollections.observableArrayList(
                "ALL", "CREATED", "ASSIGNED", "IN_DELIVERY", "COMPLETED"
        ));
        batchFilter.setValue("ALL");
        batchFilter.setOnAction(e -> updateBatchList());

        this.batchList = new VBox();
        batchList.setSpacing(8);

        this.scrollPane = new ScrollPane(batchList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(260);

        this.batchDetailBox = new VBox(6);
        batchDetailBox.setPadding(new Insets(10));
        batchDetailBox.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-background-color: #ffffff;");
        renderBatchDetail(null);

        HBox controlsBox = createControlsBox();

        listContent.getChildren().addAll(batchFilter, scrollPane, batchDetailBox, controlsBox);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        listTab.setContent(listContent);

        tabPane.getTabs().addAll(creationTab, assignmentTab, followTab, chatTab, listTab);

        this.getChildren().addAll(titleLabel, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        ShipperTrackingService.getInstance().addListener(this);
        updateBatchList();
    }

    private HBox createControlsBox() {
        HBox box = new HBox();
        box.setSpacing(5);
        box.setPadding(new Insets(10, 0, 0, 0));
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
            selectedBatchId = null;
            renderBatchDetail(null);
            updateBatchList();
        });
        clearButton.setPrefWidth(100);

        box.getChildren().addAll(refreshButton, clearButton);
        return box;
    }

    @Override
    public void onDataChanged(DataChangeEvent event) {
        if (event == null) return;

        if (!event.isType(DataChangeEvent.BATCH_UPDATED)) {
            return;
        }
        Platform.runLater(() -> {
            updateBatchList();

        });
    }

    private void updateBatchList() {
        batchList.getChildren().clear();

        List<Batch> batches = ShipperTrackingService.getInstance().getAllBatches().stream()
                .filter(this::matchesFilter)
                .toList();

        for (Batch batch : batches) {
            VBox batchCard = createBatchCard(batch);
            batchList.getChildren().add(batchCard);
        }

        if (batches.isEmpty()) {
            Label emptyLabel = new Label("Khong co batch nao");
            emptyLabel.setStyle("-fx-text-fill: #888;");
            batchList.getChildren().add(emptyLabel);
            renderBatchDetail(null);
            return;
        }

        if (selectedBatchId != null) {
            Batch selected = batches.stream()
                    .filter(batch -> batch.getId() == selectedBatchId)
                    .findFirst()
                    .orElse(null);
            renderBatchDetail(selected);
        }
    }

    private boolean matchesFilter(Batch batch) {
        String selected = batchFilter.getValue();
        if (selected == null || "ALL".equals(selected)) {
            return true;
        }
        return batch.getStatus().name().equals(selected);
    }

    private VBox createBatchCard(Batch batch) {
        VBox card = new VBox();
        card.setSpacing(3);
        card.setPadding(new Insets(8));
        String highlight = selectedBatchId != null && selectedBatchId == batch.getId() ? "#1abc9c" : "#3498db";
        card.setStyle("-fx-border-color: " + highlight + "; -fx-border-width: 2; -fx-border-radius: 3; -fx-background-color: #ffffff;");
        card.setOnMouseClicked(e -> {
            selectedBatchId = batch.getId();
            renderBatchDetail(ShipperTrackingService.getInstance().getBatch(batch.getId()));
            updateBatchList();
        });

        Label idLabel = new Label("Batch #" + batch.getId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");

        Label statusLabel = new Label("Status: " + batch.getStatus().getDisplayName());
        statusLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");

        Label progressLabel = new Label("Progress: " + batch.getDeliveredCount() + "/" + batch.getOrderCount());
        progressLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");

        Label shipperLabel = new Label("Shipper: " + (batch.getShipperId() != 0 ? batch.getShipperId() : "Chua gan"));
        shipperLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");

        card.getChildren().addAll(idLabel, statusLabel, progressLabel, shipperLabel);
        return card;
    }

    private void renderBatchDetail(Batch batch) {
        batchDetailBox.getChildren().clear();

        if (batch == null) {
            Label emptyTitle = new Label("Chi tiet Batch");
            emptyTitle.setStyle("-fx-font-weight: bold;");
            Label emptyText = new Label("Chon mot batch de xem danh sach order.");
            emptyText.setStyle("-fx-text-fill: #777;");
            batchDetailBox.getChildren().addAll(emptyTitle, emptyText);
            return;
        }

        Label title = new Label("Chi tiet Batch #" + batch.getId());
        title.setStyle("-fx-font-weight: bold;");

        Label meta = new Label("Status: " + batch.getStatus().getDisplayName()
                + " | Shipper: " + (batch.getShipperId() != 0 ? batch.getShipperId() : "Chua gan")
                + " | Orders: " + batch.getOrderCount());
        meta.setWrapText(true);
        meta.setStyle("-fx-font-size: 10px; -fx-text-fill: #555;");

        batchDetailBox.getChildren().addAll(title, meta);

        if (batch.getOrders().isEmpty()) {
            Label noOrders = new Label("Batch nay chua co order.");
            noOrders.setStyle("-fx-text-fill: #777;");
            batchDetailBox.getChildren().add(noOrders);
            return;
        }

        for (Order order : batch.getOrders()) {
            batchDetailBox.getChildren().add(createOrderDetailItem(order));
        }
    }

    private VBox createOrderDetailItem(Order order) {
        VBox item = new VBox(2);
        item.setPadding(new Insets(6));
        item.setStyle("-fx-border-color: #e5e5e5; -fx-border-width: 1; -fx-background-color: #fafafa;");

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label idLabel = new Label("Order #" + order.getId());
        idLabel.setStyle("-fx-font-weight: bold;");
        Label statusLabel = new Label(order.getStatus().getDisplayName());
        statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #555;");
        top.getChildren().addAll(idLabel, statusLabel);

        Label addressLabel = new Label(order.getAddress() == null || order.getAddress().isBlank()
                ? "(No address)"
                : order.getAddress());
        addressLabel.setWrapText(true);

        Label coordsLabel = new Label("(" + String.format("%.5f", order.getLatitude())
                + ", " + String.format("%.5f", order.getLongitude()) + ")");
        coordsLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");

        item.getChildren().addAll(top, addressLabel, coordsLabel);
        return item;
    }
}
