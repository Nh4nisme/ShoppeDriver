package com.logistics.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.*;

@Entity
@Table(name = "batch")
@Data
@NoArgsConstructor
public class Batch implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private BatchStatus status;

    @Column(name = "shipper_id")
    private int shipperId;





    public int getOrderCount() {
        return orders.size();
    }

    public int getDeliveredCount() {
        return (int) orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .count();
    }


}
