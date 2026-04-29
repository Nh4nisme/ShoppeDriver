package com.logistics.ui.admin;

import com.logistics.chat.ChatServer;
import com.logistics.model.ChatMessage;
import com.logistics.model.Shipper;
import com.logistics.service.ShipperTrackingService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class AdminChatPanel extends BorderPane implements ChatServer.ChatMessageListener {
    private final ChatServer chatServer;
    private final ShipperTrackingService shipperTrackingService;

    private final ComboBox<String> shipperCombo;
    private final TextArea messageArea;
    private final TextField inputField;
    private final Label statusLabel;
    private final Button sendButton;

    public AdminChatPanel() {
        this.chatServer = ChatServer.getInstance();
        this.shipperTrackingService = ShipperTrackingService.getInstance();

        // Top: Title, Server Status, and Shipper Selector
        VBox topBox = new VBox(10);
        topBox.setPadding(new Insets(10));
        topBox.setStyle("-fx-background-color: #2c3e50; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");

        Label titleLabel = new Label("Chat with Shippers");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox statusBox = new HBox(15);
        statusBox.setStyle("-fx-padding: 5;");

        statusLabel = new Label(chatServer.isRunning() ? "Server: RUNNING" : "Server: READY");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #27ae60;");

        Button startServerBtn = new Button("Start Server");
        startServerBtn.setStyle("-fx-font-size: 11px;");
        startServerBtn.setOnAction(e -> startChatServer());

        shipperCombo = new ComboBox<>();
        shipperCombo.setPromptText("Select Shipper");
        shipperCombo.setPrefWidth(200);
        shipperCombo.setOnAction(e -> onShipperSelected());

        Button refreshShippersBtn = new Button("Refresh");
        refreshShippersBtn.setStyle("-fx-font-size: 11px;");
        refreshShippersBtn.setOnAction(e -> refreshShipperList());

        statusBox.getChildren().addAll(statusLabel, startServerBtn,
                new Label("Select Shipper:"), shipperCombo, refreshShippersBtn);

        topBox.getChildren().addAll(titleLabel, statusBox);
        this.setTop(topBox);

        // Center: Message Display
        messageArea = new TextArea();
        messageArea.setEditable(false);
        messageArea.setWrapText(true);
        messageArea.setPrefHeight(300);
        messageArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px;");

        ScrollPane scrollPane = new ScrollPane(messageArea);
        scrollPane.setFitToWidth(true);
        this.setCenter(scrollPane);

        // Bottom: Input
        VBox bottomBox = new VBox(5);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");

        inputField = new TextField();
        inputField.setPromptText("Type message here...");
        inputField.setPrefHeight(40);
        inputField.setDisable(true);

        sendButton = new Button("Send");
        sendButton.setPrefWidth(80);
        sendButton.setDisable(true);
        sendButton.setOnAction(e -> sendMessage());

        inputField.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER") && sendButton.isDisabled() == false) {
                sendMessage();
            }
        });

        HBox inputBox = new HBox(10);
        inputBox.getChildren().addAll(inputField, sendButton);
        HBox.setHgrow(inputField, javafx.scene.layout.Priority.ALWAYS);

        bottomBox.getChildren().add(inputBox);
        this.setBottom(bottomBox);

        // Register as listener
        chatServer.addListener(this);

        // Initialize shipper list
        refreshShipperList();
    }

    private void startChatServer() {
        if (!chatServer.isRunning()) {
            chatServer.start();
            statusLabel.setText("Server: RUNNING");
            statusLabel.setStyle("-fx-text-fill: #27ae60;");
            appendMessage("System", "Chat server started on port 9999");
        }
    }

    private void refreshShipperList() {
        List<Shipper> shippers = shipperTrackingService.getAllShippers();
        Platform.runLater(() -> {
            shipperCombo.getItems().clear();
            for (Shipper shipper : shippers) {
                String status = chatServer.isShipperConnected(shipper.getId()) ? " (online)" : "";
                shipperCombo.getItems().add(shipper.getId() + " - " + shipper.getName() + status);
            }
        });
    }

    private void onShipperSelected() {
        String selected = shipperCombo.getValue();
        if (selected != null) {
            messageArea.clear();
            inputField.setDisable(false);
            sendButton.setDisable(false);
        }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        String selected = shipperCombo.getValue();

        if (text.isEmpty() || selected == null) {
            return;
        }

        try {
            int shipperId = Integer.parseInt(selected.split(" -")[0].trim());
            ChatMessage message = new ChatMessage(shipperId, 1, "Admin", text, java.time.LocalDateTime.now(), true);

            if (chatServer.isShipperConnected(shipperId)) {
                chatServer.sendMessageToShipper(shipperId, message);
                inputField.clear();
            } else {
                appendMessage("System", "Shipper not connected");
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            appendMessage("System", "Invalid shipper selection");
        }
    }

    private void appendMessage(String sender, String message) {
        Platform.runLater(() -> {
            messageArea.appendText("[" + sender + "] " + message + "\n");
        });
    }

    @Override
    public void onMessageReceived(ChatMessage message) {
        Platform.runLater(() -> {
            String selected = shipperCombo.getValue();
            if (selected != null) {
                int shipperId = Integer.parseInt(selected.split(" -")[0].trim());
                if (message.getShipperId() == shipperId) {
                    appendMessage(message.getSenderName(), message.getMessageContent());
                }
            }
        });
    }

    public boolean isServerRunning() {
        return chatServer.isRunning();
    }
}

