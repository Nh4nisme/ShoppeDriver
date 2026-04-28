package com.logistics.auth;

import com.logistics.util.Logger;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Login view for authentication
 */
public class LoginView {
    private final LoginService loginService;
    private Stage loginStage;
    private TextField usernameField;
    private PasswordField passwordField;
    private Button loginButton;
    private Label statusLabel;

    public LoginView() {
        this.loginService = new LoginService();
    }

    /**
     * Show login dialog and wait for authentication
     * @return true if login successful
     */
    public boolean showAndWait() {
        Logger.log("UI", "Hiển thị màn hình đăng nhập");

        loginStage = new Stage();
        loginStage.setTitle("ShoppeDriver - Đăng nhập Admin");
        loginStage.initModality(Modality.APPLICATION_MODAL);
        loginStage.setResizable(false);

        // Create UI components
        Label titleLabel = new Label("Đăng nhập hệ thống");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label usernameLabel = new Label("Tên đăng nhập:");
        usernameField = new TextField();
        usernameField.setPromptText("Nhập tên đăng nhập");

        Label passwordLabel = new Label("Mật khẩu:");
        passwordField = new PasswordField();
        passwordField.setPromptText("Nhập mật khẩu");

        loginButton = new Button("Đăng nhập");
        loginButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        loginButton.setPrefWidth(200);

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: red;");

        // Layout
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(
                titleLabel,
                usernameLabel,
                usernameField,
                passwordLabel,
                passwordField,
                loginButton,
                statusLabel
        );

        // Event handlers
        loginButton.setOnAction(e -> handleLogin());

        passwordField.setOnAction(e -> handleLogin()); // Enter key

        // Scene
        Scene scene = new Scene(layout, 350, 300);
        loginStage.setScene(scene);
        loginStage.showAndWait();

        return SessionManager.getInstance().isAuthenticated();
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        statusLabel.setText("Đang xác thực...");
        loginButton.setDisable(true);

        // Perform authentication
        boolean success = loginService.authenticate(username, password);

        if (success) {
            statusLabel.setText("Đăng nhập thành công!");
            statusLabel.setStyle("-fx-text-fill: green;");
            // Close dialog after short delay
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    javafx.application.Platform.runLater(() -> loginStage.close());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        } else {
            statusLabel.setText("Tên đăng nhập hoặc mật khẩu không đúng!");
            statusLabel.setStyle("-fx-text-fill: red;");
            loginButton.setDisable(false);
        }
    }

    /**
     * Initialize default admin user
     */
    public void initializeDefaultAdmin() {
        loginService.initializeDefaultAdmin();
    }
}
