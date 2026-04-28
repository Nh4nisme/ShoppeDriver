package com.logistics.ui.admin;

import com.logistics.model.Batch;
import com.logistics.model.Order;
import com.logistics.service.RouteBuilderService;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.application.Platform;

public class BatchCreationPanel extends VBox {
    private final RouteBuilderService routeBuilderService;
    private TextArea previewArea;
    private Label statusLabel;

    public BatchCreationPanel() {
        this.routeBuilderService = RouteBuilderService.getInstance();
        this.setPrefHeight(300);
        this.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1 1 1 1; -fx-padding: 10;");

        // Title
        Label titleLabel = new Label("Tạo Batch Mới (User-Driven)");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // Input section
        VBox inputBox = createInputSection();

        // Preview section
        VBox previewBox = createPreviewSection();

        this.getChildren().addAll(titleLabel, inputBox, previewBox);
        this.setSpacing(10);
    }

    private VBox createInputSection() {
        VBox box = new VBox(10);
        box.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 10;");
        box.setStyle("-fx-background-color: #f9f9f9;");

        // Start coordinates
        HBox startBox = new HBox(10);
        startBox.setAlignment(Pos.CENTER_LEFT);
        Label startLabel = new Label("Điểm A:");
        startLabel.setPrefWidth(60);
        TextField startXField = new TextField();
        startXField.setPromptText("X");
        startXField.setPrefWidth(80);
        TextField startYField = new TextField();
        startYField.setPromptText("Y");
        startYField.setPrefWidth(80);
        startBox.getChildren().addAll(startLabel, startXField, startYField);

        // End coordinates
        HBox endBox = new HBox(10);
        endBox.setAlignment(Pos.CENTER_LEFT);
        Label endLabel = new Label("Điểm B:");
        endLabel.setPrefWidth(60);
        TextField endXField = new TextField();
        endXField.setPromptText("X");
        endXField.setPrefWidth(80);
        TextField endYField = new TextField();
        endYField.setPromptText("Y");
        endYField.setPrefWidth(80);
        endBox.getChildren().addAll(endLabel, endXField, endYField);

        // Batch size
        HBox sizeBox = new HBox(10);
        sizeBox.setAlignment(Pos.CENTER_LEFT);
        Label sizeLabel = new Label("Kích thước:");
        sizeLabel.setPrefWidth(60);
        Spinner<Integer> sizeSpinner = new Spinner<>(3, 20, 5);
        sizeSpinner.setPrefWidth(100);
        sizeBox.getChildren().addAll(sizeLabel, sizeSpinner);

        // Create button
        Button createButton = new Button("Tạo Batch");
        createButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 12px;");
        createButton.setPrefWidth(150);
        createButton.setOnAction(e -> {
            try {
                double startX = Double.parseDouble(startXField.getText());
                double startY = Double.parseDouble(startYField.getText());
                double endX = Double.parseDouble(endXField.getText());
                double endY = Double.parseDouble(endYField.getText());
                int maxSize = sizeSpinner.getValue();

                statusLabel.setText("Đang tạo batch...");
                statusLabel.setStyle("-fx-text-fill: #FF9800;");

                // Create batch on background thread
                new Thread(() -> {
                    Batch batch = routeBuilderService.createBatchFromRoute(startX, startY, endX, endY, maxSize);
                    Platform.runLater(() -> {
                        if (batch != null) {
                            statusLabel.setText("✓ Batch " + batch.getId() + " tạo thành công! (" + batch.getOrderCount() + " đơn hàng)");
                            statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                            updatePreview(batch);
                        } else {
                            statusLabel.setText("✗ Lỗi: Không tìm thấy đơn hàng trong vùng");
                            statusLabel.setStyle("-fx-text-fill: #f44336;");
                        }
                    });
                }).start();

            } catch (NumberFormatException ex) {
                statusLabel.setText("✗ Lỗi: Vui lòng nhập các tọa độ hợp lệ");
                statusLabel.setStyle("-fx-text-fill: #f44336;");
            }
        });

        box.getChildren().addAll(startBox, endBox, sizeBox, createButton);
        return box;
    }

    private VBox createPreviewSection() {
        VBox box = new VBox(5);
        box.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 10;");

        Label previewLabel = new Label("Preview Đơn Hàng:");
        previewLabel.setStyle("-fx-font-weight: bold;");

        previewArea = new TextArea();
        previewArea.setEditable(false);
        previewArea.setPrefRowCount(4);
        previewArea.setWrapText(true);
        previewArea.setText("Tạo batch để xem danh sách đơn hàng...");

        statusLabel = new Label("Sẵn sàng");
        statusLabel.setStyle("-fx-font-size: 10px;");

        box.getChildren().addAll(previewLabel, previewArea, statusLabel);
        return box;
    }

    private void updatePreview(Batch batch) {
        StringBuilder preview = new StringBuilder();
        preview.append("Batch ID: ").append(batch.getId()).append("\n");
        preview.append("Số đơn hàng: ").append(batch.getOrderCount()).append("\n");
        preview.append("Đã optimize route...\n\n");

        for (Order order : batch.getOrders()) {
            preview.append("Đơn #").append(order.getId())
                    .append(" (").append(String.format("%.1f", order.getX()))
                    .append(", ").append(String.format("%.1f", order.getY())).append(")\n");
        }

        previewArea.setText(preview.toString());
    }
}


