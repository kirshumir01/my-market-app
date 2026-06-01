package ru.yandex.practicum.items.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "items")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "title", nullable = false, length = 50)
    String title;
    @Column(name = "description", nullable = false, length = 256)
    String description;
    @Column(name = "img_path")
    String imgPath;
    @Column(name = "price", nullable = false)
    Long price;
}
