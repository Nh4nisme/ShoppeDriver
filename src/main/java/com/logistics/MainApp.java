package com.logistics;

import com.logistics.auth.LoginView;
import com.logistics.db.DatabaseInitializer;
import com.logistics.service.DispatcherService;
import com.logistics.service.OrderService;
import com.logistics.service.RouteBuilderService;
import com.logistics.service.ShipperTrackingService;
import com.logistics.ui.admin.DashboardView;
import com.logistics.ui.admin.LogPanel;
import com.logistics.util.Logger;
import com.logistics.util.ThreadPoolManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
        Logger.log("SYSTEM", "Khoi dong he thong");

        try {
            DatabaseInitializer.initialize();

            LoginView loginView = new LoginView();
            loginView.initializeDefaultAdmin();

            boolean loginSuccess = loginView.showAndWait();
            if (!loginSuccess) {
                Logger.log("SYSTEM", "Dang nhap that bai, thoat ung dung");
                System.exit(0);
                return;
            }

            Logger.log("SYSTEM", "Dang nhap thanh cong, khoi tao dashboard");

            initializeServices();

            dashboardView = new DashboardView();
            logPanel = LogPanel.getInstance();
            Logger.log("UI", "Dashboard loaded");

            Scene scene = new Scene(dashboardView, 1400, 800);
            primaryStage.setTitle("Shoppe Driver - Admin Dashboard");
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(e -> shutdown());
            primaryStage.show();

            startBackgroundServices();
            ShipperTrackingService.getInstance().start();
            Logger.log("SYSTEM", "Shipper apps chay rieng biet voi lenh: java ShipperApp <shipperId>");
            startMapUpdateTimer();
        } catch (Exception e) {
            Logger.error("SYSTEM", "Loi khoi dong ung dung: " + e.getMessage());
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
        threadPoolManager.execute(orderService);
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
            orderService.stop();
            trackingService.stop();
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
