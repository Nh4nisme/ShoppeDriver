package com.logistics.ui.admin;

import com.logistics.model.AddressSuggestion;
import com.logistics.model.Batch;
import com.logistics.model.Order;
import com.logistics.model.Route;
import com.logistics.service.AddressSuggestService;
import com.logistics.service.BatchService;
import com.logistics.service.OrderService;
import com.logistics.service.RouteBuilderService;
import com.logistics.service.ShipperTrackingService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BatchCreationPanel extends VBox {
    private static final double ORDER_SEARCH_RADIUS_KM = 5.0;

    private final RouteBuilderService routeBuilderService;
    private final OrderService orderService;
    private final BatchService batchService;
    private final AddressSuggestService addressSuggestService;
    private final Map<Integer, CheckBox> orderSelections = new LinkedHashMap<>();

    private Label routeSummaryLabel;
    private Label statusLabel;
    private Label selectionLabel;
    private VBox orderListBox;
    private Button createButton;
    private Button selectAllButton;
    private Button clearSelectionButton;
    private TextField fromAddressField;
    private TextField toAddressField;
    private Route currentRoute;
    private List<Order> loadedOrders = new ArrayList<>();
    private AddressSuggestion selectedFromSuggestion;
    private AddressSuggestion selectedToSuggestion;

    public BatchCreationPanel() {
        this.routeBuilderService = RouteBuilderService.getInstance();
        this.orderService = OrderService.getInstance();
        this.batchService = new BatchService();
        this.addressSuggestService = AddressSuggestService.getInstance();
        this.setPrefHeight(300);
        this.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1 1 1 1; -fx-padding: 10;");

        Label titleLabel = new Label("Tao Batch Moi (Address-Based)");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        VBox inputBox = createInputSection();
        VBox orderBox = createOrderSection();

        this.getChildren().addAll(titleLabel, inputBox, orderBox);
        this.setSpacing(10);
    }

    private VBox createInputSection() {
        VBox box = new VBox(10);
        box.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 10;");

        HBox fromBox = new HBox(10);
        fromBox.setAlignment(Pos.CENTER_LEFT);
        Label fromLabel = new Label("From:");
        fromLabel.setPrefWidth(60);
        fromAddressField = new TextField();
        fromAddressField.setPromptText("From Address");
        fromAddressField.setPrefWidth(350);
        installAutocomplete(fromAddressField, true);
        fromBox.getChildren().addAll(fromLabel, fromAddressField);

        HBox toBox = new HBox(10);
        toBox.setAlignment(Pos.CENTER_LEFT);
        Label toLabel = new Label("To:");
        toLabel.setPrefWidth(60);
        toAddressField = new TextField();
        toAddressField.setPromptText("To Address");
        toAddressField.setPrefWidth(350);
        installAutocomplete(toAddressField, false);
        toBox.getChildren().addAll(toLabel, toAddressField);

        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_LEFT);

        Button previewButton = new Button("Preview Route");
        Button loadButton = new Button("Load Orders");
        createButton = new Button("Create Batch");
        createButton.setDisable(true);

        previewButton.setOnAction(e -> previewRoute());
        loadButton.setOnAction(e -> loadOrders());
        createButton.setOnAction(e -> createBatch());

        actionBox.getChildren().addAll(previewButton, loadButton, createButton);

        box.getChildren().addAll(fromBox, toBox, actionBox);
        return box;
    }

    private VBox createOrderSection() {
        VBox box = new VBox(8);
        box.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 10;");

        Label sectionLabel = new Label("Orders Along Route");
        sectionLabel.setStyle("-fx-font-weight: bold;");

        routeSummaryLabel = new Label("Nhap dia chi de preview route va load orders...");
        routeSummaryLabel.setWrapText(true);

        HBox selectionActions = new HBox(8);
        selectionActions.setAlignment(Pos.CENTER_LEFT);

        selectAllButton = new Button("Select All");
        clearSelectionButton = new Button("Clear Selection");
        selectAllButton.setDisable(true);
        clearSelectionButton.setDisable(true);
        selectAllButton.setOnAction(e -> setAllSelections(true));
        clearSelectionButton.setOnAction(e -> setAllSelections(false));

        selectionLabel = new Label("0 selected");
        selectionLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #555;");

        selectionActions.getChildren().addAll(selectAllButton, clearSelectionButton, selectionLabel);

        orderListBox = new VBox(6);
        orderListBox.setPadding(new Insets(4));

        ScrollPane orderScroll = new ScrollPane(orderListBox);
        orderScroll.setFitToWidth(true);
        orderScroll.setPrefHeight(220);

        statusLabel = new Label("San sang");
        statusLabel.setStyle("-fx-font-size: 10px;");

        box.getChildren().addAll(sectionLabel, routeSummaryLabel, selectionActions, orderScroll, statusLabel);
        VBox.setVgrow(orderScroll, Priority.ALWAYS);
        renderEmptyOrders("Chua co du lieu order.");
        return box;
    }

    private void installAutocomplete(TextField field, boolean isFromField) {
        ContextMenu suggestionsPopup = new ContextMenu();
        PauseTransition debounce = new PauseTransition(Duration.millis(350));

        field.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isFromField) {
                selectedFromSuggestion = null;
            } else {
                selectedToSuggestion = null;
            }

            suggestionsPopup.hide();
            debounce.stop();
            if (newValue == null || newValue.isBlank() || newValue.trim().length() < 3) {
                return;
            }

            debounce.setOnFinished(event -> loadSuggestions(field, suggestionsPopup, newValue.trim(), isFromField));
            debounce.playFromStart();
        });

        field.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                suggestionsPopup.hide();
            }
        });
    }

    private void loadSuggestions(TextField field, ContextMenu popup, String keyword, boolean isFromField) {
        new Thread(() -> {
            try {
                List<AddressSuggestion> suggestions = addressSuggestService.suggest(keyword);
                Platform.runLater(() -> showSuggestions(field, popup, suggestions, isFromField));
            } catch (Exception ex) {
                Platform.runLater(popup::hide);
            }
        }).start();
    }

    private void showSuggestions(TextField field, ContextMenu popup, List<AddressSuggestion> suggestions, boolean isFromField) {
        popup.getItems().clear();

        if (suggestions.isEmpty()) {
            popup.hide();
            return;
        }

        for (AddressSuggestion suggestion : suggestions) {
            Label label = new Label(suggestion.getDisplayText());
            label.setWrapText(true);
            label.setMaxWidth(320);

            CustomMenuItem item = new CustomMenuItem(label, true);
            item.setOnAction(event -> {
                field.setText(suggestion.getDisplayText());
                if (isFromField) {
                    selectedFromSuggestion = suggestion;
                } else {
                    selectedToSuggestion = suggestion;
                }
                popup.hide();
            });
            popup.getItems().add(item);
        }

        if (!popup.isShowing()) {
            popup.show(field, javafx.geometry.Side.BOTTOM, 0, 0);
        }
    }

    private void previewRoute() {
        String from = fromAddressField.getText();
        String to = toAddressField.getText();
        if (isInvalidAddressInput(from, to)) {
            return;
        }

        statusLabel.setText("Dang preview route...");
        statusLabel.setStyle("-fx-text-fill: #FF9800;");

        new Thread(() -> {
            try {
                Route route = routeBuilderService.previewRoute(from, to);
                currentRoute = route;
                Platform.runLater(() -> {
                    routeSummaryLabel.setText("Route ready\n"
                            + "Distance: " + String.format("%.2f km", route.getDistanceMeters() / 1000.0) + "\n"
                            + "Duration: " + String.format("%.1f min", route.getDurationSeconds() / 60.0) + "\n"
                            + "Polyline points: " + route.getPolyline().size());
                    statusLabel.setText("Preview route thanh cong");
                    statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Khong preview duoc route: " + ex.getMessage()));
            }
        }).start();
    }

    private void loadOrders() {
        String from = fromAddressField.getText();
        String to = toAddressField.getText();
        if (isInvalidAddressInput(from, to)) {
            return;
        }

        statusLabel.setText("Dang load orders...");
        statusLabel.setStyle("-fx-text-fill: #FF9800;");
        createButton.setDisable(true);

        new Thread(() -> {
            try {
                Route route = currentRoute != null ? currentRoute : routeBuilderService.previewRoute(from, to);
                currentRoute = route;
                List<Order> orders = orderService.findOrdersAlongRoute(route, ORDER_SEARCH_RADIUS_KM);
                Platform.runLater(() -> updateLoadedOrders(orders));
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Khong load duoc orders: " + ex.getMessage()));
            }
        }).start();
    }

    private void createBatch() {
        if (currentRoute == null) {
            showError("Can preview route truoc khi tao batch");
            return;
        }
        if (loadedOrders.isEmpty()) {
            showError("Chua co orders de tao batch");
            return;
        }

        List<Order> selectedOrders = getSelectedOrders();
        if (selectedOrders.isEmpty()) {
            showError("Vui long chon it nhat 1 order");
            return;
        }

        statusLabel.setText("Dang tao batch...");
        statusLabel.setStyle("-fx-text-fill: #FF9800;");

        new Thread(() -> {
            try {
                Batch batch = batchService.createBatch(selectedOrders);
                Platform.runLater(() -> {
                    statusLabel.setText("Tao batch " + batch.getId() + " thanh cong voi " + batch.getOrderCount() + " order");
                    statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                    routeSummaryLabel.setText("Batch " + batch.getId() + " da duoc tao.");
                    clearLoadedOrdersAfterCreate();
                    ShipperTrackingService.getInstance().refreshData();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Tao batch that bai: " + ex.getMessage()));
            }
        }).start();
    }

    private boolean isInvalidAddressInput(String from, String to) {
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            showError("Vui long nhap day du from/to address");
            return true;
        }
        return false;
    }

    private void updateLoadedOrders(List<Order> orders) {
        loadedOrders = new ArrayList<>(orders);
        orderSelections.clear();
        orderListBox.getChildren().clear();

        if (orders.isEmpty()) {
            renderEmptyOrders("Khong co order nam trong ban kinh 5 km quanh route.");
            showError("Khong tim thay order phu hop");
            return;
        }

        for (Order order : orders) {
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(true);
            checkBox.selectedProperty().addListener((obs, oldValue, newValue) -> updateSelectionState());
            orderSelections.put(order.getId(), checkBox);
            orderListBox.getChildren().add(createOrderItem(order, checkBox));
        }

        selectAllButton.setDisable(false);
        clearSelectionButton.setDisable(false);
        updateSelectionState();
        statusLabel.setText("Load orders thanh cong");
        statusLabel.setStyle("-fx-text-fill: #4CAF50;");
    }

    private HBox createOrderItem(Order order, CheckBox checkBox) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color: #dddddd; -fx-border-width: 1; -fx-padding: 8; -fx-background-color: #ffffff;");

        VBox content = new VBox(3);
        Label idLabel = new Label("Order #" + order.getId());
        idLabel.setStyle("-fx-font-weight: bold;");

        Label addressLabel = new Label(order.getAddress() == null || order.getAddress().isBlank()
                ? "(No address)"
                : order.getAddress());
        addressLabel.setWrapText(true);

        Label metaLabel = new Label(
                order.getStatus().getDisplayName()
                        + " | (" + String.format("%.5f", order.getLatitude())
                        + ", " + String.format("%.5f", order.getLongitude()) + ")"
        );
        metaLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");

        content.getChildren().addAll(idLabel, addressLabel, metaLabel);
        HBox.setHgrow(content, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(checkBox, content, spacer);
        return row;
    }

    private void renderEmptyOrders(String message) {
        orderListBox.getChildren().clear();
        Label emptyLabel = new Label(message);
        emptyLabel.setStyle("-fx-text-fill: #888;");
        orderListBox.getChildren().add(emptyLabel);
        selectionLabel.setText("0 selected");
        createButton.setDisable(true);
        selectAllButton.setDisable(true);
        clearSelectionButton.setDisable(true);
    }

    private void setAllSelections(boolean selected) {
        orderSelections.values().forEach(checkBox -> checkBox.setSelected(selected));
        updateSelectionState();
    }

    private List<Order> getSelectedOrders() {
        return loadedOrders.stream()
                .filter(order -> {
                    CheckBox checkBox = orderSelections.get(order.getId());
                    return checkBox != null && checkBox.isSelected();
                })
                .collect(Collectors.toList());
    }

    private void updateSelectionState() {
        long selectedCount = orderSelections.values().stream().filter(CheckBox::isSelected).count();
        selectionLabel.setText(selectedCount + " selected");
        createButton.setDisable(selectedCount == 0 || loadedOrders.isEmpty());
    }

    private void clearLoadedOrdersAfterCreate() {
        loadedOrders = new ArrayList<>();
        orderSelections.clear();
        renderEmptyOrders("Batch da duoc tao. Load route de tim orders moi.");
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #f44336;");
    }
}
