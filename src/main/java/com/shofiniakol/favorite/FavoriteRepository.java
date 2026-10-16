package com.shofiniakol.favorite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    List<Favorite> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Favorite> findByUserIdAndOfferId(UUID userId, UUID offerId);

    Optional<Favorite> findByUserIdAndRestaurantId(UUID userId, UUID restaurantId);

    Optional<Favorite> findByIdAndUserId(UUID id, UUID userId);

    @Query("select f.offerId from Favorite f where f.userId = :userId and f.offerId is not null")
    List<UUID> findOfferIdsByUser(@Param("userId") UUID userId);

    @Query("select f.restaurantId from Favorite f where f.userId = :userId and f.restaurantId is not null")
    List<UUID> findRestaurantIdsByUser(@Param("userId") UUID userId);

    @Query("select f.userId from Favorite f where f.offerId = :offerId")
    List<UUID> findUserIdsByOffer(@Param("offerId") UUID offerId);

    @Query("select f.userId from Favorite f where f.restaurantId = :restaurantId")
    List<UUID> findUserIdsByRestaurant(@Param("restaurantId") UUID restaurantId);

    @Modifying
    @Query("delete from Favorite f where f.userId = :userId")
    int deleteByUser(@Param("userId") UUID userId);
}
