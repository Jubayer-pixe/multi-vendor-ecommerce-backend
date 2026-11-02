
package com.abc.SpringBootSecqurityEx.entity.delivery;

import com.abc.SpringBootSecqurityEx.entity.order.Order;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "deliveries",
    indexes = {
        @Index(name = "idx_delivery_agent_status",
               columnList = "delivery_agent_id,status"),
        @Index(name = "idx_delivery_assigned_at",
               columnList = "assigned_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_delivery_order",
            columnNames = "order_id"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_agent_id", nullable = false)
    private DeliveryAgent deliveryAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryStatus status = DeliveryStatus.ASSIGNED;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "delivery_address", nullable = false, length = 1000)
    private String deliveryAddress;

    @Column(name = "tracking_note", length = 1000)
    private String trackingNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (assignedAt == null) {
            assignedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    
}
