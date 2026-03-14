package com.neighbourhood.intelligence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "traffic_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false, unique = true)
    private Region region;

    @Column(name = "baseline_time_secs")
    private Integer baselineTimeSecs;

    @Column(name = "peak_time_secs")
    private Integer peakTimeSecs;

    @Column(precision = 4, scale = 2)
    private BigDecimal multiplier;

    @Column(name = "congestion_level", length = 20)
    private String congestionLevel;

    @Column(name = "traffic_score", precision = 3, scale = 1)
    private BigDecimal trafficScore;

    @Column(name = "last_traffic_updated")
    private ZonedDateTime lastTrafficUpdated;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;
}
