package com.logistics.ui.shipper;

import com.logistics.chat.ChatClient;
import com.logistics.model.ChatMessage;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ChatPanel extends BorderPane implements ChatClient.ChatClientListener {
    private final ChatClient chatClient;
    private final TextArea messageArea;
    private final TextField inputField;
    private final Label statusLabel;
    private final Button sendButton;

    public ChatPanel() {
        this.chatClient = ChatClient.getInstance();

        // Top: Title and Status
        VBox topBox = new VBox(5);
        topBox.setPadding(new Insets(10));
        topBox.setStyle("-fx-background-color: #2c3e50;");

        Label titleLabel = new Label("Admin Chat");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

        statusLabel = new Label("Disconnected");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");

        Button connectButton = new Button("Connect");
        connectButton.setStyle("-fx-font-size: 11px;");
        connectButton.setOnAction(e -> connectToServer());

        HBox statusBox = new HBox(10);
        statusBox.setStyle("-fx-padding: 5;");
        statusBox.getChildren().addAll(statusLabel, connectButton);

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

        sendButton = new Button("Send");
        sendButton.setPrefWidth(80);
        sendButton.setDisable(true);
        sendButton.setOnAction(e -> sendMessage());

        inputField.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER")) {
                sendMessage();
            }
        });

        HBox inputBox = new HBox(10);
        inputBox.getChildren().addAll(inputField, sendButton);
        HBox.setHgrow(inputField, javafx.scene.layout.Priority.ALWAYS);

        bottomBox.getChildren().add(inputBox);
        this.setBottom(bottomBox);

        // Register as listener
        chatClient.addListener(this);
    }

    private void connectToServer() {
        if (!chatClient.isConnected()) {
            chatClient.connect();
        }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        if (!chatClient.isConnected()) {
            appendMessage("System", "Not connected to admin");
            return;
        }

        chatClient.sendMessage(text);
        inputField.clear();
    }

    private void appendMessage(String sender, String message) {
        Platform.runLater(() -> {
            messageArea.appendText("[" + sender + "] " + message + "\n");
        });
    }

    @Override
    public void onMessageReceived(ChatMessage message) {
        appendMessage(message.getSenderName(), message.getMessageContent());
    }

    @Override
    public void onConnectionStatusChanged(boolean connected) {
        Platform.runLater(() -> {
            if (connected) {
                statusLabel.setText("Connected");
                statusLabel.setStyle("-fx-text-fill: #27ae60;");
                sendButton.setDisable(false);
                appendMessage("System", "Connected to admin");
            } else {
                statusLabel.setText("Disconnected");
                statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                sendButton.setDisable(true);
                appendMessage("System", "Disconnected from admin");
            }
        });
    }
}

