package com.logistics.util;

public class LocationUtil {
    private static final double MOVE_SPEED = 0.1;

    public static double calculateDistance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static double[] moveTowards(double fromX, double fromY, double toX, double toY) {
        double distance = calculateDistance(fromX, fromY, toX, toY);
        if (distance < MOVE_SPEED) {
            return new double[]{toX, toY};
        }

        double ratio = MOVE_SPEED / distance;
        double newX = fromX + (toX - fromX) * ratio;
        double newY = fromY + (toY - fromY) * ratio;

        return new double[]{newX, newY};
    }

    public static boolean isCloseEnough(double x1, double y1, double x2, double y2) {
        return calculateDistance(x1, y1, x2, y2) < 0.2;
    }
}

