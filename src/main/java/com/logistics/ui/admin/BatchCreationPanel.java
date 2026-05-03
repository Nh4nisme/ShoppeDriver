package com.logistics.ui.admin;

import com.logistics.model.*;
import com.logistics.service.AddressSuggestService;
import com.logistics.service.BatchService;
import com.logistics.service.OrderService;
import com.logistics.service.RouteBuilderService;
import com.logistics.service.ShipperTrackingService;
import com.logistics.ui.GoogleMapsPanel;
import com.logistics.util.Logger;
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
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
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
    private static final double ORDER_SEARCH_RADIUS_KM = 0.5;

    private final RouteBuilderService routeBuilderService;
    private final OrderService orderService;
    private final BatchService batchService;
    private final AddressSuggestService addressSuggestService;
    private final Map<Integer, CheckBox> orderSelections = new LinkedHashMap<>();

    private Label routeSummaryLabel;
    private Label statusLabel;
    private Label selectionLabel;
    private Label routeListLabel;
    private VBox routeListBox;
    private VBox orderListBox;
    private Button createButton;
    private Button selectAllButton;
    private Button clearSelectionButton;
    private TextField fromStreetField;
    private TextField fromNumberField;
    private TextField fromWardField;
    private TextField fromDistrictField;
    private TextField fromCityField;
    private TextField toStreetField;
    private TextField toNumberField;
    private TextField toWardField;
    private TextField toDistrictField;
    private TextField toCityField;
    private List<Route> previewRoutes = new ArrayList<>();
    private int selectedRouteIndex = 0;
    private Route currentRoute;
    private List<Order> loadedOrders = new ArrayList<>();

    public BatchCreationPanel() {
        this.routeBuilderService = RouteBuilderService.getInstance();
        this.orderService = OrderService.getInstance();
        this.batchService = new BatchService();
        this.addressSuggestService = AddressSuggestService.getInstance();
        this.setPrefWidth(470);
        this.setMinWidth(470);
        this.setPrefHeight(340);
        this.setStyle("-fx-background-color: #e2e8f0; -fx-padding: 15;");

        Label titleLabel = new Label("Tao Batch Moi (Address-Based)");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #000000;");

        VBox inputBox = createInputSection();
        VBox orderBox = createOrderSection();
        VBox contentBox = new VBox(10, inputBox, orderBox);
        ScrollPane mainScroll = new ScrollPane(contentBox);
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(mainScroll, Priority.ALWAYS);

        this.getChildren().addAll(titleLabel, mainScroll);
        this.setSpacing(10);
    }

    private VBox createInputSection() {
        VBox box = new VBox(14);
        box.setStyle("-fx-background-color: #e9ecef; -fx-border-color: #ced4da; -fx-border-width: 1; -fx-padding: 14; -fx-background-radius: 5; -fx-border-radius: 5;");

        Label helperLabel = new Label("Nhap dia chi theo tung phan. Khi go chu, he thong se goi y dia chi de dien nhanh.");
        helperLabel.setWrapText(true);
        helperLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #000000;");

        VBox fromAddressBox = createAddressComponentBox("Diem bat dau", true);
        VBox toAddressBox = createAddressComponentBox("Diem ket thuc", false);

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

        box.getChildren().addAll(helperLabel, fromAddressBox, toAddressBox, actionBox);
        return box;
    }

    private VBox createAddressComponentBox(String title, boolean isFromField) {
        VBox box = new VBox(10);
        box.setStyle("-fx-border-color: #d7deea; -fx-border-width: 1; -fx-padding: 12; -fx-background-color: #ffffff;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #000000;");

        Label subtitleLabel = new Label("Co the nhap ten duong, phuong, quan hoac thanh pho. Chon mot goi y de dien tu dong.");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #000000;");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);

        Label streetLabel = createFieldLabel("Ten duong");
        TextField streetField = createAddressField("VD: Nguyen Trai");

        Label numberLabel = createFieldLabel("So nha");
        TextField numberField = createAddressField("VD: 123");

        Label wardLabel = createFieldLabel("Phuong / Xa");
        TextField wardField = createAddressField("VD: Phuong Ben Thanh");

        Label districtLabel = createFieldLabel("Quan / Huyen");
        TextField districtField = createAddressField("VD: Quan 1");

        Label cityLabel = createFieldLabel("Tinh / Thanh pho");
        TextField cityField = createAddressField("VD: TP Ho Chi Minh");

        HBox streetNumberBox = new HBox(10, streetField, numberField);
        HBox.setHgrow(streetField, Priority.ALWAYS);
        numberField.setPrefWidth(170);
        numberField.setMinWidth(150);
        numberField.setMaxWidth(220);

        formGrid.add(streetLabel, 0, 0);
        formGrid.add(streetNumberBox, 1, 0);
        formGrid.add(wardLabel, 0, 1);
        formGrid.add(wardField, 1, 1);
        formGrid.add(districtLabel, 0, 2);
        formGrid.add(districtField, 1, 2);
        formGrid.add(cityLabel, 0, 3);
        formGrid.add(cityField, 1, 3);

        AddressSection section = new AddressSection(title, streetField, numberField, wardField, districtField, cityField);
        installAutocomplete(section);

        if (isFromField) {
            fromStreetField = streetField;
            fromNumberField = numberField;
            fromWardField = wardField;
            fromDistrictField = districtField;
            fromCityField = cityField;
        } else {
            toStreetField = streetField;
            toNumberField = numberField;
            toWardField = wardField;
            toDistrictField = districtField;
            toCityField = cityField;
        }

        box.getChildren().addAll(titleLabel, subtitleLabel, formGrid);
        return box;
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.setMinWidth(110);
        label.setPrefWidth(110);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #000000;");
        return label;
    }

    private TextField createAddressField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.setPrefWidth(320);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private VBox createOrderSection() {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: #e9ecef; -fx-border-color: #ced4da; -fx-border-width: 1; -fx-padding: 10; -fx-background-radius: 5; -fx-border-radius: 5;");

        Label sectionLabel = new Label("Orders Along Route");
        sectionLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        routeSummaryLabel = new Label("Nhap dia chi de preview route va load orders...");
        routeSummaryLabel.setStyle("-fx-text-fill: #000000;");
        routeSummaryLabel.setWrapText(true);

        routeListLabel = new Label("Chua co tuyen duong de chon.");
        routeListLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #000000;");

        routeListBox = new VBox(6);
        routeListBox.setPadding(new Insets(2, 0, 4, 0));

        HBox selectionActions = new HBox(8);
        selectionActions.setAlignment(Pos.CENTER_LEFT);

        selectAllButton = new Button("Select All");
        clearSelectionButton = new Button("Clear Selection");
        selectAllButton.setDisable(true);
        clearSelectionButton.setDisable(true);
        selectAllButton.setOnAction(e -> setAllSelections(true));
        clearSelectionButton.setOnAction(e -> setAllSelections(false));

        selectionLabel = new Label("0 selected");
        selectionLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #000000;");

        selectionActions.getChildren().addAll(selectAllButton, clearSelectionButton, selectionLabel);

        orderListBox = new VBox(6);
        orderListBox.setPadding(new Insets(4));

        ScrollPane orderScroll = new ScrollPane(orderListBox);
        orderScroll.setFitToWidth(true);
        orderScroll.setPrefHeight(280);
        orderScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        statusLabel = new Label("San sang");
        statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #000000;");

        box.getChildren().addAll(sectionLabel, routeSummaryLabel, routeListLabel, routeListBox, selectionActions, orderScroll, statusLabel);
        VBox.setVgrow(orderScroll, Priority.ALWAYS);
        renderEmptyOrders("Chua co du lieu order.");
        return box;
    }

    private void previewRoute() {
        if (isInvalidAddressInput()) {
            return;
        }

        appLog("Dang preview tuyen duong...");
        Logger.log("BATCH_UI", "Preview route: " + buildFullAddressText(fromStreetField, fromNumberField, fromWardField, fromDistrictField, fromCityField)
                + " -> " + buildFullAddressText(toStreetField, toNumberField, toWardField, toDistrictField, toCityField));
        statusLabel.setText("Dang preview route...");
        statusLabel.setStyle("-fx-text-fill: #FF9800;");

        new Thread(() -> {
            try {
                LatLng fromLatLng = routeBuilderService.getGeoService().geocodeStructured(
                        fromStreetField.getText(), fromNumberField.getText(),
                        fromWardField.getText(), fromDistrictField.getText(), fromCityField.getText()
                );
                LatLng toLatLng = routeBuilderService.getGeoService().geocodeStructured(
                        toStreetField.getText(), toNumberField.getText(),
                        toWardField.getText(), toDistrictField.getText(), toCityField.getText()
                );

                List<Route> routes = routeBuilderService.getRouteService().getAlternativeRoutes(fromLatLng, toLatLng);
                if (routes.isEmpty()) {
                    throw new IllegalStateException("Khong tim thay route");
                }
                previewRoutes = new ArrayList<>(routes);
                selectedRouteIndex = 0;
                currentRoute = previewRoutes.getFirst();
                Platform.runLater(() -> {
                    renderRouteOptions();
                    GoogleMapsPanel.showRoutePreview(previewRoutes, selectedRouteIndex);
                    GoogleMapsPanel.showPreviewOrders(List.of());
                    updateRouteSummary(currentRoute);
                    statusLabel.setText("Preview route thanh cong");
                    statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                    appLog("Preview route thanh cong: " + previewRoutes.size() + " tuyen");
                });
                Logger.log("BATCH_UI", "Preview route thanh cong voi " + previewRoutes.size() + " route option(s)");
            } catch (Exception ex) {
                Logger.error("BATCH_UI", "Preview route that bai: " + ex.getMessage());
                Platform.runLater(() -> {
                    showError("Khong preview duoc route: " + ex.getMessage());
                    appLog("Preview route that bai");
                });
            }
        }).start();
    }

    private void loadOrders() {
        if (isInvalidAddressInput()) {
            return;
        }

        statusLabel.setText("Dang load orders...");
        statusLabel.setStyle("-fx-text-fill: #FF9800;");
        createButton.setDisable(true);
        appLog("Dang tai danh sach order tren tuyen...");
        Logger.log("BATCH_UI", "Load orders along route bat dau");

        new Thread(() -> {
            try {
                Route route = currentRoute;
                if (route == null) {
                    LatLng fromLatLng = routeBuilderService.getGeoService().geocodeStructured(
                            fromStreetField.getText(), fromNumberField.getText(),
                            fromWardField.getText(), fromDistrictField.getText(), fromCityField.getText()
                    );
                    LatLng toLatLng = routeBuilderService.getGeoService().geocodeStructured(
                            toStreetField.getText(), toNumberField.getText(),
                            toWardField.getText(), toDistrictField.getText(), toCityField.getText()
                    );
                    previewRoutes = new ArrayList<>(routeBuilderService.getRouteService().getAlternativeRoutes(fromLatLng, toLatLng));
                    selectedRouteIndex = 0;
                    route = previewRoutes.getFirst();
                }
                currentRoute = route;
                List<Order> orders = orderService.findOrdersAlongRoute(route, ORDER_SEARCH_RADIUS_KM);
                Logger.log("BATCH_UI", "Load orders xong: " + orders.size() + " order hop le");
                Platform.runLater(() -> {
                    renderRouteOptions();
                    GoogleMapsPanel.showRoutePreview(previewRoutes, selectedRouteIndex);
                    GoogleMapsPanel.showPreviewOrders(orders);
                    updateLoadedOrders(orders);
                    appLog("Da tim thay " + orders.size() + " order phu hop");
                });
            } catch (Exception ex) {
                Logger.error("BATCH_UI", "Load orders that bai: " + ex.getMessage());
                Platform.runLater(() -> {
                    showError("Khong load duoc orders: " + ex.getMessage());
                    appLog("Load orders that bai");
                });
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
        appLog("Dang tao batch voi " + selectedOrders.size() + " order");
        Logger.log("BATCH_UI", "Create batch voi " + selectedOrders.size() + " order da chon");

        new Thread(() -> {
            try {
                Batch batch = batchService.createBatch(selectedOrders);
                Logger.log("BATCH_UI", "Create batch thanh cong: batchId=" + batch.getId());
                Platform.runLater(() -> {
                    GoogleMapsPanel.clearRoutePreview();
                    statusLabel.setText("Tao batch " + batch.getId() + " thanh cong voi " + batch.getOrderCount() + " order");
                    statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                    routeSummaryLabel.setText("Batch " + batch.getId() + " da duoc tao.");
                    routeListLabel.setText("Chua co tuyen duong de chon.");
                    routeListBox.getChildren().clear();
                    clearLoadedOrdersAfterCreate();
                    ShipperTrackingService.getInstance().refreshData();
                    appLog("Tao batch #" + batch.getId() + " thanh cong");
                });
            } catch (Exception ex) {
                Logger.error("BATCH_UI", "Create batch that bai: " + ex.getMessage());
                Platform.runLater(() -> {
                    showError("Tao batch that bai: " + ex.getMessage());
                    appLog("Tao batch that bai");
                });
            }
        }).start();
    }

    private boolean isInvalidAddressInput() {
        String fromCity = fromCityField.getText();
        String toCity = toCityField.getText();
        if (fromCity == null || fromCity.isBlank() || toCity == null || toCity.isBlank()) {
            showError("Vui long nhap day du from/to city field");
            return true;
        }
        return false;
    }

    private void updateLoadedOrders(List<Order> orders) {
        loadedOrders = new ArrayList<>(orders);
        orderSelections.clear();
        orderListBox.getChildren().clear();

        if (orders.isEmpty()) {
            GoogleMapsPanel.showPreviewOrders(List.of());
            renderEmptyOrders("Khong co order nam trong pham vi 0.5 km quanh route.");
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
        idLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        Label addressLabel = new Label(order.getAddress() == null || order.getAddress().isBlank()
                ? "(No address)"
                : order.getAddress());
        addressLabel.setStyle("-fx-text-fill: #000000;");
        addressLabel.setWrapText(true);

        Label metaLabel = new Label(
                order.getStatus().getDisplayName()
                        + " | (" + String.format("%.5f", order.getLatitude())
                        + ", " + String.format("%.5f", order.getLongitude()) + ")"
        );
        metaLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #000000;");

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
        emptyLabel.setStyle("-fx-text-fill: #000000;");
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
        previewRoutes = new ArrayList<>();
        selectedRouteIndex = 0;
        currentRoute = null;
        renderEmptyOrders("Batch da duoc tao. Load route de tim orders moi.");
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #f44336;");
        Logger.error("BATCH_UI", message);
    }

    private void appLog(String message) {
        LogPanel.getInstance().log(message);
    }

    private void installAutocomplete(AddressSection section) {
        installAutocompleteOnField(section, section.streetField());
        installAutocompleteOnField(section, section.numberField());
        installAutocompleteOnField(section, section.wardField());
        installAutocompleteOnField(section, section.districtField());
        installAutocompleteOnField(section, section.cityField());
    }

    private void installAutocompleteOnField(AddressSection section, TextField field) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            section.debounce().stop();
            section.popup().hide();

            if (newValue == null || newValue.isBlank() || newValue.trim().length() < 2) {
                return;
            }

            section.debounce().setOnFinished(event -> loadSuggestions(section, field));
            section.debounce().playFromStart();
        });

        field.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) {
                section.debounce().stop();
            }
        });
    }

    private void loadSuggestions(AddressSection section, TextField activeField) {
        String keyword = buildSuggestionKeyword(section, activeField);
        if (keyword.length() < 2) {
            return;
        }

        Logger.debug("BATCH_UI", "Suggest address for " + section.sectionName() + ": " + keyword);
        new Thread(() -> {
            try {
                List<AddressSuggestion> suggestions = addressSuggestService.suggest(keyword);
                Platform.runLater(() -> showSuggestions(section, activeField, suggestions));
            } catch (Exception ex) {
                Logger.error("BATCH_UI", "Suggest address that bai: " + ex.getMessage());
                Platform.runLater(section.popup()::hide);
            }
        }).start();
    }

    private void showSuggestions(AddressSection section, TextField activeField, List<AddressSuggestion> suggestions) {
        section.popup().getItems().clear();
        if (suggestions.isEmpty()) {
            section.popup().hide();
            return;
        }

        for (AddressSuggestion suggestion : suggestions) {
            VBox itemBox = new VBox(2);
            Label title = new Label(suggestion.getDisplayText());
            title.setWrapText(true);
            title.setMaxWidth(380);
            title.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #000000;");

            Label meta = new Label(buildSuggestionMeta(suggestion));
            meta.setWrapText(true);
            meta.setStyle("-fx-font-size: 10px; -fx-text-fill: #000000;");

            itemBox.getChildren().addAll(title, meta);

            CustomMenuItem item = new CustomMenuItem(itemBox, true);
            item.setOnAction(event -> applySuggestion(section, suggestion));
            section.popup().getItems().add(item);
        }

        if (!section.popup().isShowing()) {
            section.popup().show(activeField, javafx.geometry.Side.BOTTOM, 0, 0);
        }
    }

    private void applySuggestion(AddressSection section, AddressSuggestion suggestion) {
        if (!suggestion.getStreet().isBlank()) {
            section.streetField().setText(suggestion.getStreet());
        } else if (section.streetField().getText().isBlank()) {
            section.streetField().setText(suggestion.getDisplayText());
        }
        if (!suggestion.getNumber().isBlank()) {
            section.numberField().setText(suggestion.getNumber());
        }
        if (!suggestion.getWard().isBlank()) {
            section.wardField().setText(suggestion.getWard());
        }
        if (!suggestion.getDistrict().isBlank()) {
            section.districtField().setText(suggestion.getDistrict());
        }
        if (!suggestion.getCity().isBlank()) {
            section.cityField().setText(suggestion.getCity());
        }

        section.popup().hide();
        Logger.log("BATCH_UI", "Da chon goi y dia chi cho " + section.sectionName() + ": " + suggestion.getDisplayText());
    }

    private String buildSuggestionKeyword(AddressSection section, TextField activeField) {
        List<String> parts = new ArrayList<>();
        String activeValue = activeField.getText();
        if (activeValue != null && !activeValue.isBlank()) {
            parts.add(activeValue.trim());
        }
        addIfPresent(parts, section.streetField().getText(), activeField != section.streetField());
        addIfPresent(parts, section.numberField().getText(), activeField != section.numberField());
        addIfPresent(parts, section.wardField().getText(), activeField != section.wardField());
        addIfPresent(parts, section.districtField().getText(), activeField != section.districtField());
        addIfPresent(parts, section.cityField().getText(), activeField != section.cityField());
        return String.join(", ", parts);
    }

    private String buildSuggestionMeta(AddressSuggestion suggestion) {
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, suggestion.getStreet());
        addIfPresent(parts, suggestion.getWard());
        addIfPresent(parts, suggestion.getDistrict());
        addIfPresent(parts, suggestion.getCity());
        return parts.isEmpty() ? "Nhan de dien vao form" : String.join(" | ", parts);
    }

    private String buildFullAddressText(TextField street, TextField number, TextField ward, TextField district, TextField city) {
        List<String> parts = new ArrayList<>();
        String streetText = street.getText() == null ? "" : street.getText().trim();
        String numberText = number.getText() == null ? "" : number.getText().trim();
        if (!numberText.isBlank() || !streetText.isBlank()) {
            parts.add((numberText + " " + streetText).trim());
        }
        addIfPresent(parts, ward.getText());
        addIfPresent(parts, district.getText());
        addIfPresent(parts, city.getText());
        return String.join(", ", parts);
    }

    private void addIfPresent(List<String> parts, String value) {
        addIfPresent(parts, value, true);
    }

    private void addIfPresent(List<String> parts, String value, boolean include) {
        if (include && value != null && !value.isBlank()) {
            parts.add(value.trim());
        }
    }

    private record AddressSection(
            String sectionName,
            TextField streetField,
            TextField numberField,
            TextField wardField,
            TextField districtField,
            TextField cityField,
            ContextMenu popup,
            PauseTransition debounce
    ) {
        AddressSection(String sectionName, TextField streetField, TextField numberField, TextField wardField,
                       TextField districtField, TextField cityField) {
            this(sectionName, streetField, numberField, wardField, districtField, cityField, new ContextMenu(),
                    new PauseTransition(Duration.millis(300)));
        }
    }

    private void renderRouteOptions() {
        routeListBox.getChildren().clear();
        if (previewRoutes.isEmpty()) {
            routeListLabel.setText("Chua co tuyen duong de chon.");
            return;
        }

        routeListLabel.setText("Chon 1 trong " + previewRoutes.size() + " tuyen duong goi y:");
        for (int i = 0; i < previewRoutes.size(); i++) {
            routeListBox.getChildren().add(createRouteOptionCard(previewRoutes.get(i), i));
        }
    }

    private VBox createRouteOptionCard(Route route, int index) {
        boolean selected = index == selectedRouteIndex;
        VBox card = new VBox(4);
        card.setPadding(new Insets(8));
        card.setStyle(selected
                ? "-fx-background-color: #e8f1ff; -fx-border-color: #1d70b8; -fx-border-width: 2;"
                : "-fx-background-color: #ffffff; -fx-border-color: #d6dde8; -fx-border-width: 1;");
        card.setOnMouseClicked(event -> selectRoute(index));

        Label title = new Label("Route " + (index + 1));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #000000;");

        Label meta = new Label(formatRouteMeta(route));
        meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #000000;");

        Label hint = new Label(selected ? "Dang duoc chon" : "Nhan de chon tuyen nay");
        hint.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (selected ? "#1d70b8" : "#000000") + ";");

        card.getChildren().addAll(title, meta, hint);
        return card;
    }

    private void selectRoute(int index) {
        if (index < 0 || index >= previewRoutes.size()) {
            return;
        }
        selectedRouteIndex = index;
        currentRoute = previewRoutes.get(index);
        renderRouteOptions();
        updateRouteSummary(currentRoute);
        GoogleMapsPanel.showRoutePreview(previewRoutes, selectedRouteIndex);
        if (!loadedOrders.isEmpty()) {
            GoogleMapsPanel.showPreviewOrders(loadedOrders);
        }
        appLog("Da chon Route " + (index + 1));
    }

    private void updateRouteSummary(Route route) {
        routeSummaryLabel.setText("Route ready\n"
                + "Distance: " + String.format("%.2f km", route.getDistanceMeters() / 1000.0) + "\n"
                + "Duration: " + String.format("%.1f min", route.getDurationSeconds() / 60.0) + "\n"
                + "Polyline points: " + route.getPolyline().size());
    }

    private String formatRouteMeta(Route route) {
        return String.format("%.1f min - %.2f km",
                route.getDurationSeconds() / 60.0,
                route.getDistanceMeters() / 1000.0);
    }
}
