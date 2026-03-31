package org.example.coffee.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "tbl_user_order")
@Builder
public class UserOrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String fullName;
    private String address;
    private String phoneNumber;
    private String email;
    private String state;
    private String paymentMethod;
    private Integer totalPrice;
    private LocalDateTime createdAt;
    private String reasonCancellation;
    private Long cancelerId;
    private String pendingCartIds;

    // GHN shipping fields
    private Integer toDistrictId;
    private String toWardCode;
    private String ghnOrderCode;
    private Integer shippingFee;
    private String shippingStatus;
    private String expectedDelivery;
}
