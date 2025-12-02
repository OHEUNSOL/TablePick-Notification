package com.goorm.tablepick.infra;

import com.goorm.tablepick.event.ReservationConfirmedEvent;

public interface EmailSender {

    void sendReservationEmail(ReservationConfirmedEvent reservationConfirmedEvent);
}

