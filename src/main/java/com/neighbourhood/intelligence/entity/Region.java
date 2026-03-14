package com.neighbourhood.intelligence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "regions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String geohash;

    @Column(name = "centroid_lat", nullable = false)
    private Double centroidLat;

    @Column(name = "centroid_lng", nullable = false)
    private Double centroidLng;

    @Column(name = "centroid_point", columnDefinition = "GEOMETRY(Point, 4326)")
    private Point centroidPoint;

    @Column(name = "formatted_address", length = 500)
    private String formattedAddress;

    @Column(name = "locality_score", precision = 3, scale = 1)
    private BigDecimal localityScore;

    @Column(name = "amenities_score", precision = 3, scale = 1)
    private BigDecimal amenitiesScore;

    @Column(name = "connectivity_score", precision = 3, scale = 1)
    private BigDecimal connectivityScore;

    @Column(name = "traffic_score", precision = 3, scale = 1)
    private BigDecimal trafficScore;

    @Column(name = "user_rating_score", precision = 3, scale = 1)
    private BigDecimal userRatingScore;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "last_summary_updated")
    private ZonedDateTime lastSummaryUpdated;

    @Column(name = "last_updated")
    private ZonedDateTime lastUpdated;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Facility> facilities = new ArrayList<>();

    @OneToOne(mappedBy = "region", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ConnectivityData connectivityData;

    @OneToOne(mappedBy = "region", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TrafficData trafficData;

    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();
}