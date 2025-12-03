package com.goorm.tablepick.email.repository;

import com.goorm.tablepick.email.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
}
