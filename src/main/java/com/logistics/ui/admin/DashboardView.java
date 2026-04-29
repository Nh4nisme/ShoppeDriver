package com.logistics.ui.admin;

import com.logistics.ui.GoogleMapsPanel;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DashboardView extends BorderPane {
    private final Sidebar sidebar;
    private final KPIBar kpiBar;
    private final GoogleMapsPanel mapPanel;
    private final ShipperStatusPanel shipperPanel;
    private final LogPanel logPanel;

    public DashboardView() {
        this.sidebar = new Sidebar();
        this.kpiBar = new KPIBar();
        this.mapPanel = new GoogleMapsPanel();
        this.shipperPanel = new ShipperStatusPanel();
        this.logPanel = LogPanel.getInstance();

        this.setLeft(sidebar);
        this.setTop(kpiBar);

        HBox centerBox = new HBox();
        centerBox.getChildren().addAll(mapPanel, shipperPanel);
        HBox.setHgrow(mapPanel, javafx.scene.layout.Priority.ALWAYS);
        shipperPanel.setPrefWidth(300);
        this.setCenter(centerBox);

        VBox logContainer = new VBox(logPanel);
        logContainer.setPrefHeight(130);
        logContainer.setMinHeight(130);
        this.setBottom(logContainer);
    }

    public GoogleMapsPanel getMapPanel() {
        return mapPanel;
    }

    public LogPanel getLogPanel() {
        return logPanel;
    }
}

