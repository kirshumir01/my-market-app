package ru.yandex.practicum.items.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.items.model.Item;

@Repository
public interface ItemRepository extends ReactiveCrudRepository<Item, Long> {

    Flux<Item> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByPriceAsc(
            String title,
            String description,
            Pageable pageable
    );

    Flux<Item> findAllBy(Pageable pageable);

    Mono<Long> countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title,
            String description
    );
}
