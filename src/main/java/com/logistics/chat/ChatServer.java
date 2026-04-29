package com.logistics.chat;

import com.logistics.model.ChatMessage;
import com.logistics.util.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {
    private static ChatServer instance;
    private static final int PORT = 9999;
    private static final String CONNECT_MESSAGE = "__CONNECT__";

    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    private final List<ChatMessageListener> listeners = new CopyOnWriteArrayList<>();

    private ChatServer() {
    }

    public static synchronized ChatServer getInstance() {
        if (instance == null) {
            instance = new ChatServer();
        }
        return instance;
    }

    public void start() {
        if (running) {
            Logger.log("CHAT_SERVER", "Server da dang chay");
            return;
        }

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                running = true;
                Logger.log("CHAT_SERVER", "Chat server khoi dong tren port " + PORT);

                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    Logger.log("CHAT_SERVER", "Shipper ket noi tu: " + clientSocket.getInetAddress());

                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    connectedClients.add(handler);
                    new Thread(handler).start();
                }
            } catch (SocketException e) {
                if (!running) {
                    Logger.log("CHAT_SERVER", "Server dung tap tai tinh");
                } else {
                    Logger.error("CHAT_SERVER", "Socket error: " + e.getMessage());
                }
            } catch (IOException e) {
                Logger.error("CHAT_SERVER", "Server error: " + e.getMessage());
            } finally {
                running = false;
                Logger.log("CHAT_SERVER", "Chat server dung");
            }
        }, "ChatServer-Thread").start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Logger.error("CHAT_SERVER", "Error closing server: " + e.getMessage());
        }
        connectedClients.forEach(ClientHandler::close);
    }

    public boolean isRunning() {
        return running;
    }

    public void broadcastMessage(ChatMessage message) {
        for (ClientHandler client : connectedClients) {
            client.sendMessage(message);
        }
        notifyListeners(message);
    }

    public void sendMessageToShipper(int shipperId, ChatMessage message) {
        for (ClientHandler client : connectedClients) {
            if (client.getShipperId() == shipperId) {
                client.sendMessage(message);
                break;
            }
        }
        notifyListeners(message);
    }

    public boolean isShipperConnected(int shipperId) {
        return connectedClients.stream().anyMatch(client -> client.getShipperId() == shipperId);
    }

    public List<Integer> getConnectedShipperIds() {
        List<Integer> ids = new ArrayList<>();
        for (ClientHandler client : connectedClients) {
            ids.add(client.getShipperId());
        }
        return ids;
    }

    void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
        Logger.log("CHAT_SERVER", "Shipper " + handler.getShipperId() + " ngat ket noi");
    }

    public void addListener(ChatMessageListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ChatMessageListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(ChatMessage message) {
        for (ChatMessageListener listener : listeners) {
            listener.onMessageReceived(message);
        }
    }

    public interface ChatMessageListener {
        void onMessageReceived(ChatMessage message);
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final ChatServer server;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private int shipperId = -1;

        ClientHandler(Socket socket, ChatServer server) {
            this.socket = socket;
            this.server = server;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                while (server.running) {
                    Object payload = in.readObject();
                    if (payload instanceof ChatMessage chatMessage) {
                        shipperId = chatMessage.getShipperId();
                        if (CONNECT_MESSAGE.equals(chatMessage.getMessageContent())) {
                            Logger.log("CHAT_SERVER", "Shipper " + shipperId + " dang nhap");
                            continue;
                        }
                        Logger.log("CHAT_SERVER", "Nhan tin nhan tu shipper " + shipperId);
                        server.broadcastMessage(chatMessage);
                    }
                }
            } catch (EOFException | SocketException e) {
                Logger.log("CHAT_SERVER", "Shipper " + shipperId + " ngat ket noi");
            } catch (IOException e) {
                Logger.error("CHAT_SERVER", "Client handler error: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                Logger.error("CHAT_SERVER", "Invalid chat payload: " + e.getMessage());
            } finally {
                close();
            }
        }

        void sendMessage(ChatMessage message) {
            if (out != null) {
                synchronized (out) {
                    try {
                        out.writeObject(message);
                        out.flush();
                    } catch (IOException e) {
                        Logger.error("CHAT_SERVER", "Error sending message: " + e.getMessage());
                        close();
                    }
                }
            }
        }

        void close() {
            try {
                if (socket != null) socket.close();
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                Logger.error("CHAT_SERVER", "Error closing client: " + e.getMessage());
            }
            server.removeClient(this);
        }

        int getShipperId() {
            return shipperId;
        }
    }
}


