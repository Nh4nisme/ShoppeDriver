package com.logistics;

import com.logistics.auth.LoginView;
import com.logistics.auth.SessionManager;
import com.logistics.db.DatabaseInitializer;
import com.logistics.service.*;
import com.logistics.ui.admin.DashboardView;
import com.logistics.ui.admin.LogPanel;
import com.logistics.util.Logger;
import com.logistics.util.ThreadPoolManager;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class MainApp extends Application {
    private ThreadPoolManager threadPoolManager;
    private ShipperTrackingService trackingService;
    private OrderService orderService;
    private RouteBuilderService routeBuilderService;
    private DispatcherService dispatcherService;
    private DashboardView dashboardView;
    private LogPanel logPanel;

    @Override
    public void start(Stage primaryStage) {
        Logger.log("SYSTEM", "Khởi động hệ thống");

        try {
            // Initialize database first
            DatabaseInitializer.initialize();

            // Show login screen
            LoginView loginView = new LoginView();
            loginView.initializeDefaultAdmin();

            boolean loginSuccess = loginView.showAndWait();

            if (!loginSuccess) {
                Logger.log("SYSTEM", "Đăng nhập thất bại, thoát ứng dụng");
                System.exit(0);
                return;
            }

            Logger.log("SYSTEM", "Đăng nhập thành công, khởi tạo dashboard");

            // Initialize services
            initializeServices();

            // Create dashboard
            dashboardView = new DashboardView();
            logPanel = LogPanel.getInstance();

            Logger.log("UI", "Dashboard loaded");

            // Create scene and show
            Scene scene = new Scene(dashboardView, 1400, 800);
            primaryStage.setTitle("Shoppe Driver - Admin Dashboard");
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(e -> shutdown());
            primaryStage.show();

            // Start background services
            startBackgroundServices();

            // Start tracking service
            ShipperTrackingService.getInstance().start();

            // Note: Shippers now run as separate applications
            // Run: java com.logistics.shipper.ShipperApp <shipperId>
            Logger.log("SYSTEM", "Shipper apps chạy riêng biệt với lệnh: java ShipperApp <shipperId>");

            // Start map update timer
            startMapUpdateTimer();

        } catch (Exception e) {
            Logger.error("SYSTEM", "Lỗi khởi động ứng dụng: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
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
