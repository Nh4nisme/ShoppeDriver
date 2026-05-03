package com.logistics.ui.admin;

import com.logistics.model.Batch;
import com.logistics.model.BatchStatus;
import com.logistics.model.LatLng;
import com.logistics.model.Order;
import com.logistics.model.Route;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.OrderRepositoryImpl;
import com.logistics.service.RouteService;
import com.logistics.service.ShipperTrackingService;
import com.logistics.ui.GoogleMapsPanel;
import com.logistics.util.DataChangeEvent;
import com.logistics.util.DataChangeListener;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
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

import java.util.ArrayList;
import java.util.List;

public class Sidebar extends VBox implements DataChangeListener {
    private final OrderRepository orderRepository;
    private final RouteService routeService;
    private final VBox batchList;
    private final VBox unbatchedOrderList;
    private final ScrollPane scrollPane;
    private final BatchAssignmentPanel assignmentPanel;
    private final FollowBatchPanel followBatchPanel;
    private final TabPane tabPane;
    private final VBox batchDetailBox;
    private final ComboBox<String> batchFilter;
    private final Label batchRouteStatusLabel;
    private final Label unbatchedStatusLabel;
    private Integer selectedBatchId;
    private int routePreviewRequestId = 0;

    public Sidebar() {
        this.orderRepository = new OrderRepositoryImpl();
        this.routeService = RouteService.getInstance();
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
                "ALL", "CREATED", "ASSIGNED", "IN_DELIVERY", "COMPLETED"));
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

        this.batchRouteStatusLabel = new Label("Chon batch de xem route tren map.");
        batchRouteStatusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #555;");

        HBox controlsBox = createControlsBox();

        listContent.getChildren().addAll(batchFilter, scrollPane, batchDetailBox, batchRouteStatusLabel, controlsBox);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        listTab.setContent(listContent);

        Tab unbatchedTab = new Tab();
        unbatchedTab.setText("Don chua gom");
        VBox unbatchedContent = new VBox(10);
        unbatchedContent.setPadding(new Insets(10));
        this.unbatchedOrderList = new VBox(8);
        ScrollPane unbatchedScrollPane = new ScrollPane(unbatchedOrderList);
        unbatchedScrollPane.setFitToWidth(true);
        unbatchedScrollPane.setPrefHeight(420);
        this.unbatchedStatusLabel = new Label("Danh sach order PENDING chua nam trong batch.");
        unbatchedStatusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #555;");
        Button refreshUnbatchedButton = new Button("Refresh");
        refreshUnbatchedButton.setOnAction(e -> updateUnbatchedOrderList());
        unbatchedContent.getChildren().addAll(unbatchedStatusLabel, unbatchedScrollPane, refreshUnbatchedButton);
        VBox.setVgrow(unbatchedScrollPane, Priority.ALWAYS);
        unbatchedTab.setContent(unbatchedContent);

        tabPane.getTabs().addAll(creationTab, assignmentTab, followTab, chatTab, listTab, unbatchedTab);

        this.getChildren().addAll(titleLabel, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        ShipperTrackingService.getInstance().addListener(this);
        updateBatchList();
        updateUnbatchedOrderList();
    }

    private HBox createControlsBox() {
        HBox box = new HBox();
        box.setSpacing(5);
        box.setPadding(new Insets(10, 0, 0, 0));
        box.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> {
            updateBatchList();
            updateUnbatchedOrderList();
            if (assignmentPanel != null) {
                assignmentPanel.refresh();
            }
        });
        refreshButton.setPrefWidth(100);

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> {
            selectedBatchId = null;
            renderBatchDetail(null);
            GoogleMapsPanel.clearRoutePreview();
            batchRouteStatusLabel.setText("Chon batch de xem route tren map.");
            updateBatchList();
        });
        clearButton.setPrefWidth(100);

        box.getChildren().addAll(refreshButton, clearButton);
        return box;
    }

    @Override
    public void onDataChanged(DataChangeEvent event) {
        if (event == null)
            return;

        if (!event.isType(DataChangeEvent.BATCH_UPDATED)) {
            return;
        }
        Platform.runLater(() -> {
            updateBatchList();
            updateUnbatchedOrderList();
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
        card.setStyle("-fx-border-color: " + highlight
                + "; -fx-border-width: 2; -fx-border-radius: 3; -fx-background-color: #ffffff;");
        card.setOnMouseClicked(e -> {
            selectedBatchId = batch.getId();
            Batch selectedBatch = ShipperTrackingService.getInstance().getBatch(batch.getId());
            renderBatchDetail(selectedBatch);
            showBatchRouteOnMap(selectedBatch);
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

    private void updateUnbatchedOrderList() {
        unbatchedOrderList.getChildren().clear();
        List<Order> pendingOrders = orderRepository.findByStatus(com.logistics.model.OrderStatus.PENDING);
        unbatchedStatusLabel.setText("Con " + pendingOrders.size() + " order chua gom.");

        if (pendingOrders.isEmpty()) {
            Label emptyLabel = new Label("Khong co order chua gom.");
            emptyLabel.setStyle("-fx-text-fill: #777;");
            unbatchedOrderList.getChildren().add(emptyLabel);
            return;
        }

        for (Order order : pendingOrders) {
            VBox item = createOrderDetailItem(order);
            item.setOnMouseClicked(event -> GoogleMapsPanel.showPreviewOrders(List.of(order)));
            unbatchedOrderList.getChildren().add(item);
        }
    }

    private void showBatchRouteOnMap(Batch batch) {
        int requestId = ++routePreviewRequestId;
        if (batch == null) {
            GoogleMapsPanel.clearRoutePreview();
            batchRouteStatusLabel.setText("Chon batch de xem route tren map.");
            return;
        }

        List<Order> renderableOrders = batch.getOrders().stream()
                .filter(this::hasRenderableCoordinate)
                .toList();
        GoogleMapsPanel.showPreviewOrders(renderableOrders);

        if (renderableOrders.size() < 2) {
            GoogleMapsPanel.showRoutePreview(List.of(), 0);
            batchRouteStatusLabel.setText("Batch #" + batch.getId() + " khong du order co toa do de ve route.");
            return;
        }

        batchRouteStatusLabel.setText("Dang ve route cho Batch #" + batch.getId() + "...");
        Task<Route> task = new Task<>() {
            @Override
            protected Route call() {
                return buildRouteForOrders(renderableOrders);
            }
        };

        task.setOnSucceeded(event -> {
            if (requestId != routePreviewRequestId) {
                return;
            }
            Route route = task.getValue();
            GoogleMapsPanel.showRoutePreview(List.of(route), 0);
            GoogleMapsPanel.showPreviewOrders(renderableOrders);
            batchRouteStatusLabel.setText("Dang xem route cua Batch #" + batch.getId()
                    + " (" + renderableOrders.size() + " order).");
        });

        task.setOnFailed(event -> {
            if (requestId != routePreviewRequestId) {
                return;
            }
            GoogleMapsPanel.showRoutePreview(List.of(createFallbackRoute(toPoints(sortOrdersByNearestNeighbor(renderableOrders)))), 0);
            GoogleMapsPanel.showPreviewOrders(renderableOrders);
            batchRouteStatusLabel.setText("Khong goi duoc route API, dang ve duong noi tam cho Batch #" + batch.getId() + ".");
        });

        Thread thread = new Thread(task, "batch-list-route-preview");
        thread.setDaemon(true);
        thread.start();
    }

    private Route buildRouteForOrders(List<Order> orders) {
        List<Order> sortedOrders = sortOrdersByNearestNeighbor(orders);
        List<LatLng> points = toPoints(sortedOrders);
        LatLng from = points.getFirst();
        LatLng to = points.getLast();
        List<LatLng> waypoints = points.size() > 2
                ? points.subList(1, points.size() - 1)
                : List.of();

        try {
            return routeService.getRouteWithWaypoints(from, to, waypoints);
        } catch (Exception ex) {
            return createFallbackRoute(points);
        }
    }

    private List<Order> sortOrdersByNearestNeighbor(List<Order> orders) {
        List<Order> remaining = new ArrayList<>(orders);
        remaining.sort((first, second) -> Integer.compare(first.getId(), second.getId()));

        List<Order> sorted = new ArrayList<>();
        Order current = remaining.removeFirst();
        sorted.add(current);

        while (!remaining.isEmpty()) {
            Order from = current;
            Order nearest = remaining.stream()
                    .min((first, second) -> Double.compare(distanceSquared(from, first), distanceSquared(from, second)))
                    .orElseThrow();
            remaining.remove(nearest);
            sorted.add(nearest);
            current = nearest;
        }
        return sorted;
    }

    private List<LatLng> toPoints(List<Order> orders) {
        return orders.stream()
                .map(order -> new LatLng(order.getLatitude(), order.getLongitude()))
                .toList();
    }

    private Route createFallbackRoute(List<LatLng> points) {
        double distanceMeters = 0.0;
        for (int i = 0; i < points.size() - 1; i++) {
            distanceMeters += haversineMeters(points.get(i), points.get(i + 1));
        }
        return new Route(points.getFirst(), points.getLast(), points, points, distanceMeters, 0.0);
    }

    private double distanceSquared(Order first, Order second) {
        double dLat = first.getLatitude() - second.getLatitude();
        double dLng = first.getLongitude() - second.getLongitude();
        return dLat * dLat + dLng * dLng;
    }

    private double haversineMeters(LatLng first, LatLng second) {
        double earthRadiusMeters = 6_371_000.0;
        double dLat = Math.toRadians(second.latitude() - first.latitude());
        double dLng = Math.toRadians(second.longitude() - first.longitude());
        double lat1 = Math.toRadians(first.latitude());
        double lat2 = Math.toRadians(second.latitude());
        double haversine = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return earthRadiusMeters * c;
    }

    private boolean hasRenderableCoordinate(Order order) {
        return order != null
                && order.getLatitude() >= 8.0 && order.getLatitude() <= 24.0
                && order.getLongitude() >= 102.0 && order.getLongitude() <= 110.0;
    }
}
