package com.goorm.tablepick.infra;

import com.goorm.tablepick.event.ReservationConfirmedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary // 기존 MailhogEmailSender 대신 우선순위 높게
@Slf4j
public class MockEmailSender implements EmailSender {

    @Override
    public void sendReservationEmail(ReservationConfirmedEvent event) {
        try {
            // 1초 대기
            Thread.sleep(1000);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("메일 전송 대기 중 인터럽트 발생");
        }
    }
}