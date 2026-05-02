package com.logistics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private double x;
    private double y;

    @Column(name = "customer_name")
    private String customerName;

    private String address;
    private String phone;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;






    // Keep latitude/longitude aliases
    public double getLatitude() {
        return x;
    }

    public void setLatitude(double latitude) {
        this.x = latitude;
    }

    public double getLongitude() {
        return y;
    }

    public void setLongitude(double longitude) {
        this.y = longitude;
    }


}
