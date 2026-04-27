package com.logistics.util;

import java.util.concurrent.*;

public class ThreadPoolManager {
    private static final ThreadPoolManager instance = new ThreadPoolManager();

    private final ExecutorService executorService;
    private volatile boolean running = false;

    private ThreadPoolManager() {
        this.executorService = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(false);
            return thread;
        });
    }

    public static ThreadPoolManager getInstance() {
        return instance;
    }

    public void execute(Runnable task) {
        executorService.execute(task);
    }

    public void shutdown() {
        running = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}

