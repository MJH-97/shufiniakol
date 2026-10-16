package com.shofiniakol.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPhone(String phone);

    /** {@code q} must be a non-null string ("" matches everything). */
    @Query("""
            select u from User u
            where (:q = '' or u.phone like concat('%', :q, '%')
                   or lower(coalesce(u.fullName, '')) like lower(concat('%', :q, '%')))
            """)
    Page<User> searchAll(@Param("q") String q, Pageable pageable);

    @Query("""
            select u from User u
            where u.role = :role
              and (:q = '' or u.phone like concat('%', :q, '%')
                   or lower(coalesce(u.fullName, '')) like lower(concat('%', :q, '%')))
            """)
    Page<User> searchByRole(@Param("role") Role role, @Param("q") String q, Pageable pageable);
}
