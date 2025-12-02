package com.goorm.tablepick.email.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reservationId;
    private String email;
    private String subject;

    @Enumerated(EnumType.STRING)
    private MailStatus status; // SUCCESS / FAILURE

    private String errorMessage;

    private LocalDateTime sentAt;
}