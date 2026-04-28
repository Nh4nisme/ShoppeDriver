package com.logistics.shipper;

import com.logistics.auth.LoginView;
import com.logistics.db.DatabaseInitializer;
import com.logistics.model.Batch;
import com.logistics.model.BatchStatus;
import com.logistics.model.Order;
import com.logistics.model.OrderStatus;
import com.logistics.repository.BatchRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.ShipperRepository;
import com.logistics.util.Logger;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
    private String shipperId;
    private String shipperName;
    private BatchRepository batchRepository;
    private OrderRepository orderRepository;
    private ShipperRepository shipperRepository;

    // UI Components
    private Label shipperInfoLabel;
    private ListView<String> orderListView;
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

        this.shipperId = args.get(0);

        try {
            // Initialize database
            DatabaseInitializer.initialize();

            // Initialize repositories
            batchRepository = new BatchRepository();
            orderRepository = new OrderRepository();
            shipperRepository = new ShipperRepository();

            // Get shipper info
            var shipper = shipperRepository.findById(shipperId);
            if (shipper == null) {
                Logger.error("SHIPPER", "Không tìm thấy shipper với ID: " + shipperId);
                System.exit(1);
                return;
            }
            this.shipperName = shipper.getName();

            Logger.log("SHIPPER", "Khởi tạo shipper: " + shipperName + " (ID: " + shipperId + ")");

            // Create UI
            createUI(primaryStage);

            // Start polling for batches
            startBatchPolling();

        } catch (Exception e) {
            Logger.error("SHIPPER", "Lỗi khởi tạo ứng dụng shipper: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void createUI(Stage stage) {
        stage.setTitle("ShippeDriver - " + shipperName);

        BorderPane root = new BorderPane();

        // Top: Shipper info
        shipperInfoLabel = new Label("Shipper: " + shipperName + " - Chưa có batch");
        shipperInfoLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        BorderPane.setMargin(shipperInfoLabel, new Insets(10));
        root.setTop(shipperInfoLabel);

        // Center: Order list
        orderListView = new ListView<>();
        orderListView.setPrefHeight(200);
        root.setCenter(orderListView);

        // Right: Order details
        orderDetailArea = new TextArea();
        orderDetailArea.setEditable(false);
        orderDetailArea.setPromptText("Chi tiết đơn hàng sẽ hiển thị ở đây");
        orderDetailArea.setPrefWidth(250);
        root.setRight(orderDetailArea);

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

        Scene scene = new Scene(root, 600, 500);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            Logger.log("SHIPPER", "Đóng ứng dụng shipper: " + shipperName);
            System.exit(0);
        });
        stage.show();

        Logger.log("UI", "Giao diện shipper đã tải: " + shipperName);
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
        Batch assignedBatch = batchRepository.findByShipperAndStatus(shipperId, BatchStatus.ASSIGNED);

        if (assignedBatch != null && (currentBatch == null || currentBatch.getId() != assignedBatch.getId())) {
            // New batch assigned
            currentBatch = assignedBatch;
            currentOrders = assignedBatch.getOrders();
            currentOrderIndex = 0;

            Logger.log("SHIPPER", "Nhận batch " + assignedBatch.getId() + " với " + currentOrders.size() + " đơn");

            // Update UI
            javafx.application.Platform.runLater(() -> {
                updateUIForNewBatch();
            });
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
        orderRepository.updateStatus(currentOrder.getId(), OrderStatus.DONE);

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
        shipperRepository.updateStatus(Integer.parseInt(shipperId), com.logistics.model.ShipperStatus.AVAILABLE);

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
