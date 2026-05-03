package com.logistics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "shippers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shipper implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @Column(name = "current_x")
    private double currentX;

    @Column(name = "current_y")
    private double currentY;

    @Enumerated(EnumType.STRING)
    private ShipperStatus status;

    private boolean active;

    public double distanceTo(double x, double y) {
        double dx = currentX - x;
        double dy = currentY - y;
        return Math.sqrt(dx * dx + dy * dy);
    }


}
