package com.logistics;

import com.logistics.model.Shipper;
import com.logistics.service.*;
import com.logistics.ui.admin.DashboardView;
import com.logistics.ui.admin.LogPanel;
import com.logistics.ui.shipper.ShipperAppWindow;
import com.logistics.util.ThreadPoolManager;
import com.logistics.worker.ShipperWorker;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.util.HashMap;
import java.util.Map;

public class MainApp extends Application {
    private ThreadPoolManager threadPoolManager;
    private ShipperTrackingService trackingService;
    private OrderService orderService;
    private RouteBuilderService routeBuilderService;
    private DispatcherService dispatcherService;
    private DashboardView dashboardView;
    private LogPanel logPanel;
    private final Map<String, ShipperWorker> shipperWorkers = new HashMap<>();
    private final Map<String, ShipperAppWindow> shipperWindows = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        System.out.println("[MainApp] Starting application...");

        try {
            // Initialize services
            initializeServices();

            // Create dashboard
            dashboardView = new DashboardView();
            logPanel = LogPanel.getInstance();

            logPanel.log("Application started");

            // Create scene and show
            Scene scene = new Scene(dashboardView, 1400, 800);
            primaryStage.setTitle("Shoppe Driver - Admin Dashboard");
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(e -> shutdown());
            primaryStage.show();

            logPanel.log("Dashboard loaded");

            // Start background services
            startBackgroundServices();

            // Create shippers and their workers
            createShippersAndWorkers();

            // Start map update timer
            startMapUpdateTimer();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[MainApp] Error starting application: " + e.getMessage());
        }
    }

    private void initializeServices() {
        threadPoolManager = ThreadPoolManager.getInstance();
        trackingService = ShipperTrackingService.getInstance();
        orderService = OrderService.getInstance();
        routeBuilderService = RouteBuilderService.getInstance();
        dispatcherService = DispatcherService.getInstance();

        System.out.println("[MainApp] Services initialized");
    }

    private void startBackgroundServices() {
        threadPoolManager.setRunning(true);

        // Start services in executor
        threadPoolManager.execute(orderService);
        threadPoolManager.execute(routeBuilderService);
        threadPoolManager.execute(dispatcherService);

        logPanel.log("Background services started");
        System.out.println("[MainApp] Background services started");
    }

    private void createShippersAndWorkers() {
        // Create 4 shippers
        String[] names = {"Alice", "Bob", "Charlie", "Diana"};
        double[] startX = {10, 20, 30, 40};
        double[] startY = {10, 20, 30, 40};

        for (int i = 0; i < names.length; i++) {
            String shipperId = "SHIPPER-" + (i + 1);
            Shipper shipper = new Shipper(shipperId, names[i], startX[i], startY[i]);

            // Register with tracking service
            trackingService.registerShipper(shipper);

            // Create worker
            ShipperWorker worker = new ShipperWorker(shipper);
            shipperWorkers.put(shipperId, worker);

            // Register with dispatcher
            dispatcherService.registerShipperWorker(shipperId, worker);

            // Start worker thread
            threadPoolManager.execute(worker);

            // Create window
            ShipperAppWindow window = new ShipperAppWindow(worker);
            shipperWindows.put(shipperId, window);
            window.show();

            logPanel.log("Created shipper: " + names[i]);
            System.out.println("[MainApp] Created shipper: " + names[i]);
        }
    }

    private void startMapUpdateTimer() {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(1000),
                e -> dashboardView.getMapPanel().updateMap()
            )
        );
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }

    private void shutdown() {
        System.out.println("[MainApp] Shutting down...");
        logPanel.log("Application shutting down...");

        try {
            // Stop all services
            orderService.stop();
            routeBuilderService.stop();
            dispatcherService.stop();

            // Stop all shipper workers
            shipperWorkers.values().forEach(ShipperWorker::stop);

            // Close all shipper windows
            shipperWindows.values().forEach(ShipperAppWindow::hide);

            // Shutdown thread pool
            threadPoolManager.shutdown();

            System.out.println("[MainApp] Shutdown complete");
            logPanel.log("Application stopped");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

