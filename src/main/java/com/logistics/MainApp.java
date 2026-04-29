package com.logistics;

import com.logistics.auth.LoginView;
import com.logistics.chat.ChatServer;
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
    private ChatServer chatServer;
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
            logPanel = dashboardView.getLogPanel();
            Logger.log("UI", "Dashboard loaded");

            Scene scene = new Scene(dashboardView, 1400, 800);
            primaryStage.setTitle("Shoppe Driver - Admin Dashboard");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.setFullScreen(true);
            primaryStage.setFullScreenExitHint("");
            primaryStage.setOnCloseRequest(e -> shutdown());
            primaryStage.show();
            Logger.log("UI", "Admin app opened in fullscreen mode");
            appLog("Dashboard san sang");

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
        chatServer = ChatServer.getInstance();
        chatServer.start();
        Logger.log("SYSTEM", "Services initialized");
    }

    private void startBackgroundServices() {
        threadPoolManager.setRunning(true);
        threadPoolManager.execute(orderService);
        Logger.log("SYSTEM", "Background services started");
        appLog("Da khoi dong background services");
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
        Logger.log("SYSTEM", "Shutting down application");
        appLog("Dang dong ung dung");

        try {
            orderService.stop();
            trackingService.stop();
            chatServer.stop();
            threadPoolManager.shutdown();
            Logger.log("SYSTEM", "Shutdown complete");
            appLog("Ung dung da dung");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void appLog(String message) {
        if (logPanel != null) {
            logPanel.log(message);
        }
    }
}
