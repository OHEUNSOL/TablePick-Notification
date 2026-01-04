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

    // 비동기 처리를 담당할 커스텀 스레드 풀 (Config에서 주입)
    private final ThreadPoolTaskExecutor emailThreadPoolExecutor;

    @KafkaListener(
            topics = "reservation.confirmed",
            groupId = "email-service",
            containerFactory = "batchFactory", // 배치 리스너 팩토리 사용
            concurrency = "2" // 파티션 수에 맞춰 병렬 처리 (Consumer 스레드 2개 생성)
    )
    public void consumeBatch(List<String> messages, Acknowledgment ack) {

        // 1. [비동기] 받아온 메시지 리스트를 스레드 풀에 던져서 병렬 처리
        List<CompletableFuture<Void>> futures = messages.stream()
                .map(message -> CompletableFuture.runAsync(() -> {
                    processOneEmail(message);
                }, emailThreadPoolExecutor))
                .collect(Collectors.toList());

        // 2. [동기 대기] 모든 비동기 작업이 끝날 때까지 메인 리스너 스레드 대기 (Blocking)
        // 이유: 작업이 다 끝나기도 전에 커밋해버리면, 서버 장애 시 데이터 유실 발생
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 3. [수동 커밋] 모든 처리가 안전하게 끝났음을 확인 후 오프셋 커밋
        ack.acknowledge();
    }

    // 개별 메일 발송 로직 (비동기 스레드 내부에서 실행됨)
    private void processOneEmail(String message) {
        try {
            ReservationConfirmedEvent event = ReservationConfirmedEvent.fromJson(message);

            // 메일 발송 (I/O Blocking 발생 구간 -> 비동기로 해결)
            emailSender.sendReservationEmail(event);

            // DB 로그 저장
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

        } catch (Exception e) {
            log.error("메일 발송 처리 중 에러 발생: message={}", message, e);
            // DLT(Dead Letter Topic)로 보내거나 별도 실패 처리가 필요함
        }
    }
}