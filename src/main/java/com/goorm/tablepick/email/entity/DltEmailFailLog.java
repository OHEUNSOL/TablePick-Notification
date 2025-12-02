package com.goorm.tablepick.email.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DltEmailFailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reservationId;
    private String email;
    private String restaurantName;
    private LocalDateTime confirmedAt;
    private int partySize;

    private String originalTopic;
    private Integer partition;
    private Long offset;

    private String exceptionType;
    @Column(length = 1000)
    private String exceptionMessage;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}