package com.goorm.tablepick.infra;

import com.goorm.tablepick.event.ReservationConfirmedEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailhogEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Override
    public void sendReservationEmail(ReservationConfirmedEvent event) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(event.getEmail());
            helper.setFrom("no-reply@tablepick.com");
            helper.setSubject("[TablePick] 예약이 완료되었습니다.");

            String dateText = event.getConfirmedAt()
                    .toLocalDate()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);
            String timeText = event.getConfirmedAt()
                    .toLocalTime()
                    .format(DateTimeFormatter.ofPattern("HH:mm"));

            String text = String.format(
                    "고객님의 %s 예약이 완료되었습니다.\n\n" +
                            "- 매장: %s\n" +
                            "- 예약 확정 시각: %s %s\n" +
                            "- 인원: %d명\n" +
                            "- 예약 번호: %d\n",
                    event.getRestaurantName(),
                    event.getRestaurantName(),
                    dateText,
                    timeText,
                    event.getPartySize(),
                    event.getReservationId()
            );

            helper.setText(text, false); // 텍스트 메일

            mailSender.send(message);
            log.info("예약 완료 메일 발송 성공. reservationId={}, to={}",
                    event.getReservationId(), event.getEmail());

        } catch (MessagingException e) {
            log.error("예약 완료 메일 발송 실패. reservationId={}, to={}",
                    event.getReservationId(), event.getEmail(), e);
        }
    }
}