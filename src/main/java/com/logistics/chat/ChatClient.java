package com.logistics.chat;

import com.logistics.model.ChatMessage;
import com.logistics.util.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ChatClient {
    private static ChatClient instance;

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9999;
    private static final int RECONNECT_DELAY_MS = 5000;
    private static final String CONNECT_MESSAGE = "__CONNECT__";

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean running = false;
    private volatile boolean connected = false;

    private int shipperId = -1;
    private String shipperName = "Unknown";

    private final BlockingQueue<ChatMessage> messageQueue = new LinkedBlockingQueue<>();
    private final List<ChatClientListener> listeners = new CopyOnWriteArrayList<>();

    private ChatClient() {
    }

    public static synchronized ChatClient getInstance() {
        if (instance == null) {
            instance = new ChatClient();
        }
        return instance;
    }

    public void initialize(int shipperId, String shipperName) {
        this.shipperId = shipperId;
        this.shipperName = shipperName;
    }

    public void connect() {
        if (running) {
            Logger.log("CHAT_CLIENT", "Client da dang ket noi");
            return;
        }

        new Thread(() -> {
            running = true;
            while (running) {
                try {
                    attemptConnection();
                    if (connected) {
                        readMessages();
                    }
                } catch (Exception e) {
                    Logger.error("CHAT_CLIENT", "Connection error: " + e.getMessage());
                    connected = false;
                    try {
                        Thread.sleep(RECONNECT_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "ChatClient-Thread").start();
    }

    private void attemptConnection() throws IOException {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            out.writeObject(new ChatMessage(shipperId, shipperName, CONNECT_MESSAGE, java.time.LocalDateTime.now(), false));
            out.flush();
            connected = true;
            Logger.log("CHAT_CLIENT", "Da ket noi den server: " + SERVER_HOST + ":" + SERVER_PORT);
            notifyConnectionStatus(true);
        } catch (IOException e) {
            Logger.error("CHAT_CLIENT", "Failed to connect: " + e.getMessage());
            connected = false;
            throw e;
        }
    }

    private void readMessages() {
        try {
            while (running && connected) {
                Object payload = in.readObject();
                if (payload instanceof ChatMessage chatMessage) {
                    messageQueue.offer(chatMessage);
                    notifyListeners(chatMessage);
                }
            }
        } catch (EOFException | SocketException e) {
            if (running) {
                Logger.error("CHAT_CLIENT", "Socket closed: " + e.getMessage());
            }
        } catch (IOException e) {
            Logger.error("CHAT_CLIENT", "Read error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            Logger.error("CHAT_CLIENT", "Invalid chat payload: " + e.getMessage());
        } finally {
            connected = false;
            notifyConnectionStatus(false);
            closeConnection();
        }
    }

    public void sendMessage(String messageContent) {
        if (!connected || out == null) {
            Logger.error("CHAT_CLIENT", "Not connected to server");
            return;
        }

        try {
            ChatMessage localMsg = new ChatMessage(shipperId, shipperName, messageContent, java.time.LocalDateTime.now(), false);
            synchronized (out) {
                out.writeObject(localMsg);
                out.flush();
            }
        } catch (Exception e) {
            Logger.error("CHAT_CLIENT", "Error sending message: " + e.getMessage());
        }
    }

    public void disconnect() {
        running = false;
        connected = false;
        closeConnection();
        Logger.log("CHAT_CLIENT", "Ngat ket noi toi server");
    }

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            Logger.error("CHAT_CLIENT", "Error closing connection: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public List<ChatMessage> getMessageHistory() {
        List<ChatMessage> messages = new ArrayList<>();
        messageQueue.drainTo(messages);
        // Put them back for UI to keep history
        messages.forEach(messageQueue::offer);
        return messages;
    }

    public void addListener(ChatClientListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ChatClientListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(ChatMessage message) {
        for (ChatClientListener listener : listeners) {
            listener.onMessageReceived(message);
        }
    }

    private void notifyConnectionStatus(boolean connected) {
        for (ChatClientListener listener : listeners) {
            listener.onConnectionStatusChanged(connected);
        }
    }

    public interface ChatClientListener {
        void onMessageReceived(ChatMessage message);
        void onConnectionStatusChanged(boolean connected);
    }
}

