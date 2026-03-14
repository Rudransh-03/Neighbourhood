package com.neighbourhood.intelligence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;

@Entity
@Table(name = "geocoding_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeocodingCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_key", nullable = false, unique = true, length = 500)
    private String queryKey;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(name = "formatted_address", length = 500)
    private String formattedAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;
}
