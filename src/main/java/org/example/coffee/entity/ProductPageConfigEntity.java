package org.example.coffee.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "tbl_product_page_config")
@Builder
public class ProductPageConfigEntity {
    @Id
    private Long id;
    private Integer userPageSize;
    private Integer adminPageSize;
}
