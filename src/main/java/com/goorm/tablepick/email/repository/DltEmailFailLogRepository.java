package com.goorm.tablepick.email.repository;

import com.goorm.tablepick.email.entity.DltEmailFailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DltEmailFailLogRepository extends JpaRepository<DltEmailFailLog, Long> {
}
