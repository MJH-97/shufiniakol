package com.shofiniakol.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {

    List<Restaurant> findByActiveTrue();

    @Query("""
            select r from Restaurant r
            where (:q = '' or lower(r.name) like lower(concat('%', :q, '%')))
            """)
    Page<Restaurant> searchAll(@Param("q") String q, Pageable pageable);

    @Query("""
            select r from Restaurant r
            where r.ownerId = :ownerId
              and (:q = '' or lower(r.name) like lower(concat('%', :q, '%')))
            """)
    Page<Restaurant> searchByOwner(@Param("ownerId") UUID ownerId, @Param("q") String q, Pageable pageable);

    List<Restaurant> findByOwnerId(UUID ownerId);

    @Modifying
    @Query("update Restaurant r set r.ratingAvg = :avg, r.ratingCount = :cnt where r.id = :id")
    void updateRating(@Param("id") UUID id, @Param("avg") double avg, @Param("cnt") int cnt);
}
