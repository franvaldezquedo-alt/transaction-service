package com.nttdata.transaction_service.infrastructure.adapter;

import com.ettdata.avro.AccountValidationRequest;
import com.ettdata.avro.AccountValidationResponse;
import com.nttdata.transaction_service.application.port.out.AccountValidationOutputPort;
import com.nttdata.transaction_service.infrastructure.config.KafkaTopicProperties;
import com.nttdata.transaction_service.infrastructure.handler.PendingResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaAccountValidationProducer implements AccountValidationOutputPort {

    private final KafkaTemplate<String, AccountValidationRequest> kafkaTemplate;
    private final PendingResponseHandler responseHandler;
    private final KafkaTopicProperties topicProperties;

    @Override
    public Mono<AccountValidationResponse> sendWithdrawRequest(
            String transactionId,
            String accountNumber,
            BigDecimal amount) {

        AccountValidationRequest request = AccountValidationRequest.newBuilder()
                .setTransactionId(transactionId)
                .setAccountNumber(accountNumber)
                .setTransactionType("WITHDRAW")
                .setAmount(amount.doubleValue())
                .build();

        return Mono.create(sink -> {
            responseHandler.register(transactionId, sink);

            kafkaTemplate.send(topicProperties.getAccountValidationRequest(), accountNumber, request)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("❌ Error enviando mensaje a Kafka: {}", ex.getMessage(), ex);
                            responseHandler.error(transactionId, ex);
                        } else {
                            log.info("📤 Solicitud enviada: transactionId={}, account={}, amount={}",
                                    transactionId, accountNumber, amount);
                        }
                    });
        });
    }

}