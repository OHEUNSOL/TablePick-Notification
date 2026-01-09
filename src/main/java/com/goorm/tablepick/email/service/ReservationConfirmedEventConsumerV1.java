package com.goorm.tablepick.email.service;

import com.goorm.tablepick.email.entity.EmailLog;
import com.goorm.tablepick.email.entity.MailStatus;
import com.goorm.tablepick.email.repository.EmailLogRepository;
import com.goorm.tablepick.event.ReservationConfirmedEvent;
import com.goorm.tablepick.infra.EmailSender;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationConfirmedEventConsumerV1 {

    private final EmailSender emailSender;
    private final EmailLogRepository emailLogRepository;
    private final MeterRegistry meterRegistry;

    // [추가] DLT로 직접 메시지를 쏘기 위한 템플릿
    private final KafkaTemplate<String, String> kafkaTemplate;

    // DLT 토픽 이름 상수 (DLT 컨슈머의 토픽명과 일치해야 함)
    private static final String DLT_TOPIC = "reservation.confirmed.dlt";

    private final ThreadPoolTaskExecutor emailThreadPoolExecutor;

    @KafkaListener(
            topics = "reservation.confirmed",
            groupId = "email-service",
            containerFactory = "batchFactory",
            concurrency = "2"
    )
    public void consumeBatch(List<String> messages, Acknowledgment ack) {

        List<CompletableFuture<Void>> futures = messages.stream()
                .map(message -> CompletableFuture.runAsync(() -> {
                    // 비동기 작업 시작
                    processOneEmailWithRetry(message);
                }, emailThreadPoolExecutor))
                .collect(Collectors.toList());

        // 모든 스레드 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 모든 처리가 끝났으므로(성공하든, DLT로 갔든) 현재 배치는 커밋 처리
        ack.acknowledge();
    }

    /**
     * 재시도 및 DLT 전송 로직이 포함된 메서드
     */
    private void processOneEmailWithRetry(String message) {
        int maxAttempts = 2; // 최초 시도(1) + 재시도(1) = 총 2회

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // 실제 비즈니스 로직 실행
                doProcess(message);

                // 성공하면 루프 종료
                return;

            } catch (Exception e) {
                log.warn("메일 발송 실패 (시도 {}/{}). message={}", attempt, maxAttempts, message);

                // 마지막 시도였다면 (재시도까지 실패) -> DLT 전송
                if (attempt == maxAttempts) {
                    log.error("재시도 모두 실패. DLT로 이동합니다.", e);
                    sendToDlt(message, e);
                } else {
                    // 재시도 전 잠깐 대기 (Backoff) - 선택사항 (예: 0.5초 대기)
                    try { Thread.sleep(500); } catch (InterruptedException ig) {}
                }
            }
        }
    }

    /**
     * 실제 메일 발송 및 DB 저장 로직 (핵심 로직)
     * 예외 발생 시 호출부(processOneEmailWithRetry)로 throw
     */
    private void doProcess(String message) throws Exception {
        ReservationConfirmedEvent event = ReservationConfirmedEvent.fromJson(message);

        // 1. 메일 발송
        emailSender.sendReservationEmail(event);

        // 2. DB 성공 로그 저장
        emailLogRepository.save(
                EmailLog.builder()
                        .reservationId(event.getReservationId())
                        .email(event.getEmail())
                        .subject("[TablePick] 예약이 완료되었습니다.")
                        .status(MailStatus.SUCCESS)
                        .sentAt(LocalDateTime.now())
                        .build()
        );

        meterRegistry.counter("mail.sent.success", "type", "kafka_batch").increment();
    }

    /**
     * DLT 토픽으로 메시지 전송
     */
    private void sendToDlt(String message, Exception originalException) {
        try {
            // DLT 토픽으로 원본 메시지 그대로 전송
            // (필요하다면 헤더에 에러 내용을 추가할 수도 있음)
            kafkaTemplate.send(DLT_TOPIC, message).get();

            log.info("DLT 전송 완료: {}", message);

        } catch (Exception e) {
            log.error("DLT 전송조차 실패했습니다. (메시지 유실 가능성 있음) message={}", message, e);
            // 최악의 경우: 파일로 로그를 남기거나, 슬랙 알림을 보내야 함
        }
    }
}