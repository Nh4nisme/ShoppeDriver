package com.logistics.ui.shipper;

import com.logistics.worker.ShipperWorker;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class ShipperAppWindow {
    private final Stage stage;
    private final ShipperWorker shipperWorker;

    public ShipperAppWindow(ShipperWorker shipperWorker) {
        this.shipperWorker = shipperWorker;
        this.stage = new Stage();

        // Create the view
        ShipperAppView view = new ShipperAppView(shipperWorker);

        // Create scene
        Scene scene = new Scene(view, 900, 600);

        // Configure stage
        stage.setTitle("Shipper: " + shipperWorker.getShipper().getName());
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            shipperWorker.stopDelivery();
            shipperWorker.stop();
        });
    }

    public void show() {
        stage.show();
    }

    public void hide() {
        stage.hide();
    }

    public boolean isShowing() {
        return stage.isShowing();
    }

    public Stage getStage() {
        return stage;
    }
}

