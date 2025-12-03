package com.goorm.tablepick.email.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.tablepick.email.entity.EmailLog;
import com.goorm.tablepick.email.entity.MailStatus;
import com.goorm.tablepick.email.repository.EmailLogRepository;
import com.goorm.tablepick.event.ReservationConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationConfirmedEventDltConsumer {

    private final ObjectMapper objectMapper;
    private final EmailLogRepository emailLogRepository;

    @KafkaListener(
            topics = "reservation.confirmed.dlt",
            groupId = "email-service-dlt"
    )
    public void consume(String message) {
        try {
            // 1. DLT 메시지 역직렬화
            ReservationConfirmedEvent event =
                    objectMapper.readValue(message, ReservationConfirmedEvent.class);

            // 2. 최종 실패 이력 DB 저장
            emailLogRepository.save(
                    EmailLog.builder()
                            .reservationId(event.getReservationId())
                            .email(event.getEmail())
                            .subject("[TablePick] 예약 메일 발송 최종 실패(DLT)")
                            .status(MailStatus.FAILURE)
                            .errorMessage("메일 발송 재시도 5회 실패 후 DLT로 이동")
                            .sentAt(LocalDateTime.now())
                            .build()
            );

            // 3. 로그 남기기
            log.error(
                    "[DLT] 예약 완료 메일 발송 최종 실패 처리. reservationId={}, email={}",
                    event.getReservationId(),
                    event.getEmail()
            );

        } catch (Exception e) {
            // DLT 처리 자체가 실패한 경우
            log.error("[DLT] 메시지 처리 중 예외 발생. rawMessage={}", message, e);
        }
    }
}