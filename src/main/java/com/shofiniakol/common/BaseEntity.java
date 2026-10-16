package com.shofiniakol.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/** Id + creation timestamp. Used for append-only tables. */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity extends IdEntity {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
