package com.logistics.ui.admin;

import javafx.scene.layout.BorderPane;
import javafx.scene.control.TextArea;
import javafx.geometry.Insets;
import javafx.application.Platform;

public class LogPanel extends BorderPane {
    private final TextArea logArea;
    private static volatile LogPanel instance;

    private LogPanel() {
        this.logArea = new TextArea();
        logArea.setWrapText(true);
        logArea.setEditable(false);
        logArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11;");
        logArea.setPadding(new Insets(5));

        this.setCenter(logArea);
        this.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1;");
    }

    public static LogPanel getInstance() {
        if (instance == null) {
            synchronized (LogPanel.class) {
                if (instance == null) {
                    instance = new LogPanel();
                }
            }
        }
        return instance;
    }

    public void log(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            logArea.appendText("[" + timestamp + "] " + message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    public void clear() {
        Platform.runLater(() -> logArea.clear());
    }
}

