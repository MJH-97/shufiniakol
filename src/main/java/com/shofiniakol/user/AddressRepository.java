package com.shofiniakol.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserIdOrderByPrimaryDescCreatedAtDesc(UUID userId);

    Optional<Address> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    @Modifying
    @Query("update Address a set a.primary = false where a.userId = :userId and a.primary = true")
    int clearPrimary(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from Address a where a.userId = :userId")
    int deleteByUser(@Param("userId") UUID userId);
}
