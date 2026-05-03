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
    private Button deliverNextButton;
    private Label progressLabel;

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
        // Top: Shipper info (mobile-like header)
        shipperInfoLabel = new Label("Shipper: " + shipperName + " - Chưa có batch");
        shipperInfoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 8 0 8 8;");
        BorderPane.setMargin(shipperInfoLabel, new Insets(6));
        root.setTop(shipperInfoLabel);

        // Left: Batch list (new column) - mobile-like narrow column
        batchListView = new ListView<>();
        batchListView.setPrefWidth(220);
        batchListView.setPlaceholder(new Label("No batches"));
        VBox leftBox = new VBox(6);
        Label batchesLabel = new Label("Your Batches");
        batchesLabel.setStyle("-fx-font-weight: bold; -fx-padding: 6; -fx-font-size: 12px;");
        leftBox.getChildren().addAll(batchesLabel, batchListView);
        leftBox.setPadding(new Insets(6));
        root.setLeft(leftBox);

        // Center: Order list (main content) - larger for mobile view
        orderListView = new ListView<>();
        orderListView.setPrefHeight(300);
        orderListView.setStyle("-fx-font-size: 13px;");
        root.setCenter(orderListView);

        // Right: Order details + chat (stacked) - on mobile this acts like a detail pane
        orderDetailArea = new TextArea();
        orderDetailArea.setEditable(false);
        orderDetailArea.setPromptText("Chi tiết đơn hàng sẽ hiển thị ở đây");
        orderDetailArea.setPrefWidth(300);
        orderDetailArea.setPrefHeight(180);

        ChatPanel chatPanel = new ChatPanel();
        chatPanel.setPrefWidth(320);
        chatPanel.setPrefHeight(260);

        VBox rightBox = new VBox(8, orderDetailArea, chatPanel);
        rightBox.setPadding(new Insets(6));
        root.setRight(rightBox);

        // Bottom: Controls and progress
        VBox bottomBox = new VBox(10);
        bottomBox.setPadding(new Insets(10));

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        startDeliveryButton = new Button("Bắt đầu giao hàng");
        startDeliveryButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        startDeliveryButton.setOnAction(e -> startDelivery());

        deliverNextButton = new Button("Giao đơn tiếp theo");
        deliverNextButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        deliverNextButton.setDisable(true);
        deliverNextButton.setOnAction(e -> deliverNext());

        buttonBox.getChildren().addAll(startDeliveryButton, deliverNextButton);

        progressLabel = new Label("Chưa bắt đầu giao hàng");
        progressLabel.setStyle("-fx-font-size: 12px;");

        bottomBox.getChildren().addAll(buttonBox, progressLabel);
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
                        currentBatch = batches.get(idx);
                        currentOrders = currentBatch.getOrders();
                        currentOrderIndex = 0;
                        updateUIForNewBatch();
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
                    checkForNewBatch();
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

    private void checkForNewBatch() {
        // Load currently assigned batch (single) as before
        Batch assignedBatch = batchRepository.findByShipperAndStatus(shipperId, BatchStatus.ASSIGNED);

        if (assignedBatch != null && (currentBatch == null || currentBatch.getId() != assignedBatch.getId())) {
            // New batch assigned
            currentBatch = assignedBatch;
            currentOrders = assignedBatch.getOrders();
            currentOrderIndex = 0;

            Logger.log("SHIPPER", "Nhận batch " + assignedBatch.getId() + " với " + currentOrders.size() + " đơn");

            // Update UI
            javafx.application.Platform.runLater(() -> updateUIForNewBatch());
        }

        // Additionally update the left-side batch list (active batches)
        try {
            var batches = batchRepository.findActiveByShipper(shipperId);
            javafx.application.Platform.runLater(() -> {
                updateBatchListUI(batches);
            });
        } catch (Exception e) {
            Logger.error("SHIPPER", "Loi lay danh sach batch: " + e.getMessage());
        }
    }

    private void updateBatchListUI(java.util.List<Batch> batches) {
        if (batchListView == null) return;
        ObservableList<String> items = FXCollections.observableArrayList();
        for (Batch b : batches) {
            String label = "#" + b.getId() + " - " + b.getOrders().size() + " orders - " + b.getStatus();
            items.add(label);
        }
        batchListView.setItems(items);
        if (items.isEmpty()) {
            batchListView.setPlaceholder(new Label("No batches assigned"));
        }
    }

    private void updateUIForNewBatch() {
        shipperInfoLabel.setText("Shipper: " + shipperName + " - Batch: " + currentBatch.getId());

        orderListView.getItems().clear();
        for (Order order : currentOrders) {
            orderListView.getItems().add("Đơn #" + order.getId() + " - (" +
                    String.format("%.1f", order.getX()) + ", " +
                    String.format("%.1f", order.getY()) + ") - " + order.getStatus());
        }

        startDeliveryButton.setDisable(false);
        progressLabel.setText("Batch mới: " + currentOrders.size() + " đơn - Chưa bắt đầu");
    }

    private void startDelivery() {
        if (currentBatch == null) return;

        Logger.log("SHIPPER", "Bắt đầu giao batch " + currentBatch.getId());

        startDeliveryButton.setDisable(true);
        deliverNextButton.setDisable(false);

        progressLabel.setText("Đang giao đơn 1/" + currentOrders.size());

        // Select first order
        orderListView.getSelectionModel().select(0);
        if (!currentOrders.isEmpty()) {
            showOrderDetails(currentOrders.get(0));
        }
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
            // Select next order
            orderListView.getSelectionModel().select(currentOrderIndex);
            showOrderDetails(currentOrders.get(currentOrderIndex));
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
