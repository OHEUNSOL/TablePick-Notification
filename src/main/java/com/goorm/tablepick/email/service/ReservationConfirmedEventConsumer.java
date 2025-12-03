package com.goorm.tablepick.email.service;

import com.goorm.tablepick.email.entity.EmailLog;
import com.goorm.tablepick.email.entity.MailStatus;
import com.goorm.tablepick.email.repository.EmailLogRepository;
import com.goorm.tablepick.event.ReservationConfirmedEvent;
import com.goorm.tablepick.infra.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationConfirmedEventConsumer {

    private final EmailSender emailSender;
    private final EmailLogRepository emailLogRepository;

    @KafkaListener(
            topics = "reservation.confirmed",
            groupId = "email-service",
            concurrency = "3"
    )
    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltTopicSuffix = ".dlt"
    )
    private void consume(String message) throws InterruptedException{
        ReservationConfirmedEvent reservationConfirmedEvent = ReservationConfirmedEvent.fromJson(message);

        emailSender.sendReservationEmail(reservationConfirmedEvent);
        log.info("예약 완료 메일 발송");
        emailLogRepository.save(
                EmailLog.builder()
                        .reservationId(reservationConfirmedEvent.getReservationId())
                        .email(reservationConfirmedEvent.getEmail())
                        .subject("[TablePick] 예약이 완료되었습니다.")
                        .status(MailStatus.SUCCESS)
                        .sentAt(LocalDateTime.now())
                        .build()
        );

    }
}