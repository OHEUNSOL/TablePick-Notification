package com.goorm.tablepick.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableKafka
public class KafkaConfig {

    // 1. 비동기 처리를 위한 스레드 풀 정의
    @Bean
    public ThreadPoolTaskExecutor emailThreadPoolExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20); // 배치 사이즈(20)에 맞춰 스레드 확보
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(70);
        executor.setThreadNamePrefix("EmailWorker-");
        executor.initialize();
        return executor;
    }

    // 2. 배치 리스너 팩토리 설정
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> batchFactory(
            ConsumerFactory<String, String> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true); // 배치 리스너 활성화
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE); // 수동 커밋

        return factory;
    }
}