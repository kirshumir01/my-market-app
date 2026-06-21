package ru.yandex.practicum.orders.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("orders_items")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {
    @Id
    private Long id;
    @Column("order_id")
    private Long orderId;
    @Column("item_id")
    private Long itemId;
    @Column("count")
    private Integer count;
    @Column("price")
    private Long price;
}
