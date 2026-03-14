package com.neighbourhood.intelligence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "connectivity_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectivityData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false, unique = true)
    private Region region;

    @Column(name = "nearest_airport_name", length = 500)
    private String nearestAirportName;

    @Column(name = "nearest_airport_distance_m")
    private Double nearestAirportDistanceM;

    @Column(name = "metro_station_1_name", length = 500)
    private String metroStation1Name;

    @Column(name = "metro_station_1_distance_m")
    private Double metroStation1DistanceM;

    @Column(name = "metro_station_2_name", length = 500)
    private String metroStation2Name;

    @Column(name = "metro_station_2_distance_m")
    private Double metroStation2DistanceM;

    @Column(name = "connectivity_score", precision = 3, scale = 1)
    private BigDecimal connectivityScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;
}
