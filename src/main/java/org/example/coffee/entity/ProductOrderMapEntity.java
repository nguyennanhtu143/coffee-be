package org.example.coffee.entity;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "tbl_product_order_map")
@Builder
public class ProductOrderMapEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private Long productSizeId;
    private String nameProduct;
    private String size;
    private Integer price;
    private String image;
    private Integer quantityOrder;
    private Integer totalPrice;
}
