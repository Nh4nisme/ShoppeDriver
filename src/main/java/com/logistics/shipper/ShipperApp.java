package com.logistics.shipper;

import com.logistics.auth.LoginView;
import com.logistics.chat.ChatClient;
import com.logistics.db.DatabaseInitializer;
import com.logistics.model.Batch;
import com.logistics.model.BatchStatus;
import com.logistics.model.Order;
import com.logistics.model.OrderStatus;
import com.logistics.repository.BatchRepository;
import com.logistics.repository.BatchRepositoryImpl;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.OrderRepositoryImpl;
import com.logistics.repository.ShipperRepository;
import com.logistics.repository.ShipperRepositoryImpl;
import com.logistics.ui.shipper.ChatPanel;
import com.logistics.util.Logger;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Independent Shipper Application
 * Run with: java -cp target/classes --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml com.logistics.shipper.ShipperApp <shipperId>
 */
public class ShipperApp extends Application {
    private int shipperId;
    private String shipperName;
    private BatchRepository batchRepository;
    private OrderRepository orderRepository;
    private ShipperRepository shipperRepository;
    private ChatClient chatClient;

    // UI Components
    private Label shipperInfoLabel;
    private ListView<String> orderListView;
    private ListView<String> batchListView;
    private TextArea orderDetailArea;
    private Button startDeliveryButton;
    private Button pauseDeliveryButton;
    private Button deliverNextButton;
    private Button failDeliveryButton;
    private Button refreshButton;
    private Label progressLabel;
    private Label lastRefreshLabel;

    // Current state
    private Batch currentBatch;
    private List<Order> currentOrders;
    private int currentOrderIndex = 0;

    @Override
    public void start(Stage primaryStage) {
        Logger.log("SHIPPER", "Khởi động ứng dụng shipper");

        // Get shipper ID from command line args
        List<String> args = getParameters().getRaw();
        if (args.isEmpty()) {
            Logger.error("SHIPPER", "Thiếu shipper ID. Sử dụng: java ShipperApp <shipperId>");
            System.exit(1);
            return;
        }

        this.shipperId = Integer.parseInt(args.get(0));

        try {
            // Initialize database
            DatabaseInitializer.initialize();

            // Initialize repositories
            batchRepository = new BatchRepositoryImpl();
            orderRepository = new OrderRepositoryImpl();
            shipperRepository = new ShipperRepositoryImpl();

            // Get shipper info
            var shipper = shipperRepository.findById(shipperId);
            if (shipper == null) {
                Logger.error("SHIPPER", "Không tìm thấy shipper với ID: " + shipperId);
                System.exit(1);
                return;
            }
            this.shipperName = shipper.getName();

            Logger.log("SHIPPER", "Khởi tạo shipper: " + shipperName + " (ID: " + shipperId + ")");

            // Initialize Chat Client
            chatClient = ChatClient.getInstance();
            chatClient.initialize(shipperId, shipperName);

            // Create UI
            createUI(primaryStage);

            // Start polling for batches
            startBatchPolling();

            // Start chat client connection
            chatClient.connect();

        } catch (Exception e) {
            Logger.error("SHIPPER", "Lỗi khởi tạo ứng dụng shipper: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void createUI(Stage stage) {
        stage.setTitle("ShippeDriver - " + shipperName);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-background-color: #f0f2f5;");

        HBox topBox = new HBox(10);
        topBox.setPadding(new Insets(15));
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        shipperInfoLabel = new Label("Shipper: " + shipperName + " - Chưa có batch");
        shipperInfoLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #d35400;");
        topBox.getChildren().add(shipperInfoLabel);
        root.setTop(topBox);

        // Left: Batch list (new column) - mobile-like narrow column
        batchListView = new ListView<>();
        batchListView.setPrefWidth(220);
        batchListView.setPlaceholder(new Label("No batches"));
        batchListView.setStyle("-fx-background-insets: 0; -fx-padding: 0; -fx-background-radius: 5; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-font-size: 13px;");
        VBox leftBox = new VBox(10);
        Label batchesLabel = new Label("Danh sách chuyến xe");
        batchesLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #555;");
        leftBox.getChildren().addAll(batchesLabel, batchListView);
        leftBox.setPadding(new Insets(15));
        root.setLeft(leftBox);

        // Center: Order list (main content) - larger for mobile view
        orderListView = new ListView<>();
        orderListView.setStyle("-fx-background-insets: 0; -fx-padding: 0; -fx-background-radius: 5; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-font-size: 14px;");
        Label ordersLabel = new Label("Chi tiết đơn hàng");
        ordersLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #555;");
        VBox centerBox = new VBox(10, ordersLabel, orderListView);
        centerBox.setPadding(new Insets(15));
        root.setCenter(centerBox);

        // Right: Order details + chat (stacked) - on mobile this acts like a detail pane
        orderDetailArea = new TextArea();
        orderDetailArea.setEditable(false);
        orderDetailArea.setPromptText("Chi tiết đơn hàng sẽ hiển thị ở đây");
        orderDetailArea.setPrefWidth(320);
        orderDetailArea.setPrefHeight(200);
        orderDetailArea.setWrapText(true);
        orderDetailArea.setStyle("-fx-font-size: 14px; -fx-background-radius: 5; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");

        ChatPanel chatPanel = new ChatPanel();
        chatPanel.setPrefWidth(320);
        chatPanel.setPrefHeight(300);
        chatPanel.setStyle("-fx-background-color: white; -fx-background-radius: 5; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");

        Label detailLabel = new Label("Thông tin & Liên hệ");
        detailLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #555;");

        VBox rightBox = new VBox(10, detailLabel, orderDetailArea, chatPanel);
        rightBox.setPadding(new Insets(15));
        root.setRight(rightBox);

        // Bottom: Controls and progress
        VBox bottomBox = new VBox(12);
        bottomBox.setPadding(new Insets(15));
        bottomBox.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, -2);");

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        startDeliveryButton = new Button("Bắt đầu giao hàng");
        startDeliveryButton.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 8 16; -fx-cursor: hand;");
        startDeliveryButton.setOnAction(e -> startDelivery());

        pauseDeliveryButton = new Button("Tạm dừng");
        pauseDeliveryButton.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: #333; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 8 16; -fx-cursor: hand;");
        pauseDeliveryButton.setDisable(true);
        pauseDeliveryButton.setOnAction(e -> pauseDelivery());

        deliverNextButton = new Button("Giao đơn tiếp theo");
        deliverNextButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 8 16; -fx-cursor: hand;");
        deliverNextButton.setDisable(true);
        deliverNextButton.setOnAction(e -> deliverNext());

        failDeliveryButton = new Button("Giao không được");
        failDeliveryButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 8 16; -fx-cursor: hand;");
        failDeliveryButton.setDisable(true);
        failDeliveryButton.setOnAction(e -> failDeliverCurrent());

        refreshButton = new Button("↻ Làm mới");
        refreshButton.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 8 16; -fx-cursor: hand;");
        refreshButton.setOnAction(e -> manualRefresh());

        buttonBox.getChildren().addAll(startDeliveryButton, pauseDeliveryButton, deliverNextButton, failDeliveryButton, refreshButton);

        progressLabel = new Label("Chưa bắt đầu giao hàng");
        progressLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");
        lastRefreshLabel = new Label("");
        lastRefreshLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
        
        HBox infoBox = new HBox(10, progressLabel, lastRefreshLabel);
        infoBox.setAlignment(Pos.CENTER);

        bottomBox.getChildren().addAll(buttonBox, infoBox);
        root.setBottom(bottomBox);

        // Event handlers
        orderListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            if (currentOrders != null && newVal.intValue() >= 0 && newVal.intValue() < currentOrders.size()) {
                showOrderDetails(currentOrders.get(newVal.intValue()));
            }
        });

        // When user selects a batch on the left, load that batch into view
        batchListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            int idx = newVal.intValue();
            if (idx >= 0) {
                // fetch active batches and pick by index
                try {
                    var batches = batchRepository.findActiveByShipper(shipperId);
                    if (idx < batches.size()) {
                        Batch selectedBatch = batches.get(idx);
                        if (currentBatch == null || currentBatch.getId() != selectedBatch.getId()) {
                            currentBatch = selectedBatch;
                            currentOrders = currentBatch.getOrders();
                            currentOrderIndex = (int) currentOrders.stream()
                                    .filter(o -> o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.FAILED)
                                    .count();
                            updateUIForNewBatch();
                        }
                    }
                } catch (Exception e) {
                    Logger.error("SHIPPER", "Loi load batch tu list: " + e.getMessage());
                }
            }
        });

        // Use a compact window (approx half of typical admin width) instead of fullscreen
        Scene scene = new Scene(root, 800, 700);
        stage.setScene(scene);
        stage.setResizable(true);
        // Do not maximize/fullscreen for shipper app — keep a mobile-like compact window
        stage.setWidth(800);
        stage.setHeight(700);
        stage.centerOnScreen();
        stage.setOnCloseRequest(e -> {
            Logger.log("SHIPPER", "Đóng ứng dụng shipper: " + shipperName);
            if (chatClient != null) {
                chatClient.disconnect();
            }
            System.exit(0);
        });
        stage.show();

        Logger.log("UI", "Giao diện shipper đã tải: " + shipperName);
        Logger.log("UI", "Shipper app opened in fullscreen mode");
    }

    private void startBatchPolling() {
        Thread pollingThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    checkForNewBatch(false);
                    Thread.sleep(3000); // Check every 3 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        pollingThread.setDaemon(true);
        pollingThread.start();

        Logger.log("SHIPPER", "Bắt đầu kiểm tra batch cho shipper: " + shipperName);
    }

    private void checkForNewBatch(boolean force) {
        // Load active batch
        Batch activeBatch = batchRepository.findByShipperAndStatus(shipperId, BatchStatus.IN_DELIVERY);
        if (activeBatch == null) {
            activeBatch = batchRepository.findByShipperAndStatus(shipperId, BatchStatus.ASSIGNED);
        }

        if (activeBatch != null && (force || currentBatch == null || currentBatch.getId() != activeBatch.getId())) {
            // New batch assigned or force refresh
            currentBatch = activeBatch;
            currentOrders = activeBatch.getOrders();
            currentOrderIndex = (int) currentOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.FAILED)
                    .count();

            Logger.log("SHIPPER", "Nhận batch " + activeBatch.getId() + " với " + currentOrders.size() + " đơn");

            // Update UI
            javafx.application.Platform.runLater(() -> updateUIForNewBatch());
        }

        // Additionally update the left-side batch list (active batches)
        try {
            var batches = batchRepository.findActiveByShipper(shipperId);
            javafx.application.Platform.runLater(() -> {
                updateBatchListUI(batches);
                if (lastRefreshLabel != null) {
                    lastRefreshLabel.setText("Cập nhật lúc " + new SimpleDateFormat("HH:mm:ss").format(new Date()));
                }
            });
        } catch (Exception e) {
            Logger.error("SHIPPER", "Loi lay danh sach batch: " + e.getMessage());
        }
    }

    private void updateBatchListUI(java.util.List<Batch> batches) {
        if (batchListView == null) return;
        int selectedIndex = batchListView.getSelectionModel().getSelectedIndex();
        ObservableList<String> items = FXCollections.observableArrayList();
        for (Batch b : batches) {
            String label = "#" + b.getId() + " - " + b.getOrders().size() + " orders - " + b.getStatus();
            items.add(label);
        }
        batchListView.setItems(items);
        if (items.isEmpty()) {
            batchListView.setPlaceholder(new Label("No batches assigned"));
        }
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            batchListView.getSelectionModel().select(selectedIndex);
        }
    }

    private void updateUIForNewBatch() {
        shipperInfoLabel.setText("Shipper: " + shipperName + " - Batch: " + currentBatch.getId());

        orderListView.getItems().clear();
        for (Order order : currentOrders) {
            String statusText = order.getStatus().toString();
            if (order.getStatus() == OrderStatus.COMPLETED) {
                statusText = "✓ HOÀN THÀNH";
            } else if (order.getStatus() == OrderStatus.FAILED) {
                statusText = "✗ THẤT BẠI";
            }
            orderListView.getItems().add("Đơn #" + order.getId() + " - (" +
                    String.format("%.1f", order.getX()) + ", " +
                    String.format("%.1f", order.getY()) + ") - " + statusText);
        }

        if (currentBatch != null && currentBatch.getStatus() == BatchStatus.IN_DELIVERY) {
            startDeliveryButton.setDisable(true);
            pauseDeliveryButton.setDisable(false);
            if (currentOrderIndex < currentOrders.size()) {
                deliverNextButton.setDisable(false);
                failDeliveryButton.setDisable(false);
                progressLabel.setText("Đang giao đơn " + (currentOrderIndex + 1) + "/" + currentOrders.size());
                orderListView.getSelectionModel().select(currentOrderIndex);
                showOrderDetails(currentOrders.get(currentOrderIndex));
            } else {
                deliverNextButton.setDisable(true);
                failDeliveryButton.setDisable(true);
                pauseDeliveryButton.setDisable(true);
                progressLabel.setText("Batch hoàn thành! Chờ batch mới...");
            }
        } else {
            startDeliveryButton.setDisable(false);
            startDeliveryButton.setText(currentOrderIndex > 0 ? "Tiếp tục giao hàng" : "Bắt đầu giao hàng");
            if (pauseDeliveryButton != null) pauseDeliveryButton.setDisable(true);
            deliverNextButton.setDisable(true);
            failDeliveryButton.setDisable(true);
            progressLabel.setText(currentOrderIndex > 0 ? "Đang tạm dừng (" + currentOrderIndex + "/" + currentOrders.size() + ")" : "Batch mới: " + currentOrders.size() + " đơn - Chưa bắt đầu");
        }
        
        if (refreshButton != null) {
            refreshButton.setDisable(false);
            refreshButton.setText("↻ Làm mới");
        }
    }

    private void manualRefresh() {
        if (refreshButton != null) {
            refreshButton.setDisable(true);
            refreshButton.setText("Đang tải...");
        }
        Thread refreshThread = new Thread(() -> {
            checkForNewBatch(true);
            javafx.application.Platform.runLater(() -> {
                if (refreshButton != null) {
                    refreshButton.setDisable(false);
                    refreshButton.setText("↻ Làm mới");
                }
            });
        });
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    private void startDelivery() {
        if (currentBatch == null) return;

        Logger.log("SHIPPER", "Bắt đầu/Tiếp tục giao batch " + currentBatch.getId());
        
        batchRepository.updateStatus(currentBatch.getId(), BatchStatus.IN_DELIVERY);
        currentBatch.setStatus(BatchStatus.IN_DELIVERY);

        checkForNewBatch(true);
    }

    private void pauseDelivery() {
        if (currentBatch == null) return;

        Logger.log("SHIPPER", "Tạm dừng giao batch " + currentBatch.getId());
        
        batchRepository.updateStatus(currentBatch.getId(), BatchStatus.ASSIGNED);
        currentBatch.setStatus(BatchStatus.ASSIGNED);

        checkForNewBatch(true);
    }

    private void deliverNext() {
        if (currentOrders == null || currentOrderIndex >= currentOrders.size()) return;

        Order currentOrder = currentOrders.get(currentOrderIndex);

        // Mark order as done
        orderRepository.updateStatus(currentOrder.getId(), OrderStatus.COMPLETED);

        Logger.log("SHIPPER", "Hoàn thành đơn " + currentOrder.getId());

        // Update UI
        orderListView.getItems().set(currentOrderIndex,
                "Đơn #" + currentOrder.getId() + " - ✓ HOÀN THÀNH");

        currentOrderIndex++;
        progressLabel.setText("Đang giao đơn " + (currentOrderIndex + 1) + "/" + currentOrders.size());

        if (currentOrderIndex >= currentOrders.size()) {
            // Batch completed
            completeBatch();
        } else {
            checkForNewBatch(true);
        }
    }

    private void failDeliverCurrent() {
        if (currentOrders == null || currentOrderIndex >= currentOrders.size()) return;

        Order currentOrder = currentOrders.get(currentOrderIndex);

        // Mark order as FAILED
        orderRepository.updateStatus(currentOrder.getId(), OrderStatus.FAILED);

        Logger.log("SHIPPER", "Giao thất bại đơn " + currentOrder.getId());

        // Update UI
        orderListView.getItems().set(currentOrderIndex,
                "Đơn #" + currentOrder.getId() + " - ✗ THẤT BẠI");

        currentOrderIndex++;
        progressLabel.setText("Đang giao đơn " + (currentOrderIndex + 1) + "/" + currentOrders.size());

        if (currentOrderIndex >= currentOrders.size()) {
            // Batch completed
            completeBatch();
        } else {
            checkForNewBatch(true);
        }
    }

    private void completeBatch() {
        Logger.log("SHIPPER", "Hoàn thành batch " + currentBatch.getId());

        // Update batch status
        batchRepository.updateStatus(currentBatch.getId(), BatchStatus.COMPLETED);

        // Update shipper status to AVAILABLE
        shipperRepository.updateStatus(shipperId, com.logistics.model.ShipperStatus.AVAILABLE);

        // Update UI
        deliverNextButton.setDisable(true);
        failDeliveryButton.setDisable(true);
        progressLabel.setText("Batch hoàn thành! Chờ batch mới...");

        // Reset state
        currentBatch = null;
        currentOrders = null;
        currentOrderIndex = 0;
    }

    private void showOrderDetails(Order order) {
        String details = String.format(
                "ĐƠN HÀNG #%d\n\n" +
                        "Vị trí: (%.1f, %.1f)\n" +
                        "Trạng thái: %s\n" +
                        "Thời gian tạo: %s",
                order.getId(),
                order.getX(),
                order.getY(),
                order.getStatus(),
                "N/A" // Could add timestamp if needed
        );
        orderDetailArea.setText(details);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
