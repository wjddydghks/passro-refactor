package com.passro.passrobackend.domain.review.repository;

import com.passro.passrobackend.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByDeliveryId(Long deliveryId);

    @Query("""
            select avg(r.rating)
            from Review r
            where r.delivery.shipper.id = :userId
            """)
    Double findAverageRatingByShipperId(Long userId);
}
