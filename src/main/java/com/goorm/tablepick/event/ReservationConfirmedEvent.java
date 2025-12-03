package com.goorm.tablepick.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReservationConfirmedEvent {
    private Long reservationId;
    private String email;
    private String restaurantName;
    private LocalDateTime confirmedAt;
    private int partySize;

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules(); // 또는 .registerModule(new JavaTimeModule())

    public static ReservationConfirmedEvent fromJson(String json) {
        try {
            return objectMapper.readValue(json, ReservationConfirmedEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 파싱 실패", e);
        }
    }
}

