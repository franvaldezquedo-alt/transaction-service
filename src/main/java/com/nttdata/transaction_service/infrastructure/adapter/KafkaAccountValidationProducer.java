package com.nttdata.transaction_service.infrastructure.adapter;

import com.ettdata.avro.AccountValidationRequest;
import com.ettdata.avro.AccountValidationResponse;
import com.nttdata.transaction_service.application.port.out.AccountValidationOutputPort;
import com.nttdata.transaction_service.infrastructure.config.KafkaTopicProperties;
import com.nttdata.transaction_service.infrastructure.handler.PendingResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaAccountValidationProducer implements AccountValidationOutputPort {

    private final KafkaTemplate<String, AccountValidationRequest> kafkaTemplate;
    private final PendingResponseHandler responseHandler;
    private final KafkaTopicProperties topicProperties;

    // ========== WITHDRAW ==========

    @Override
    public Mono<AccountValidationResponse> sendWithdrawRequest(
            String transactionId,
            String accountNumber,
            BigDecimal amount) {

        log.info("💰 Enviando solicitud de retiro: transactionId={}, account={}, amount={}",
                transactionId, accountNumber, amount);

        return sendValidationRequest(
                transactionId,
                accountNumber,
                amount,
                "WITHDRAWAL"
        );
    }

    // ========== DEPOSIT ==========

    @Override
    public Mono<AccountValidationResponse> sendDepositRequest(
            String transactionId,
            String accountNumber,
            BigDecimal amount) {

        log.info("💵 Enviando solicitud de depósito: transactionId={}, account={}, amount={}",
                transactionId, accountNumber, amount);

        return sendValidationRequest(
                transactionId,
                accountNumber,
                amount,
                "DEPOSIT"
        );
    }

    // ========== TRANSFER ==========

    @Override
    public Mono<AccountValidationResponse> sendTransferRequest(
            String transactionId,
            String sourceAccountNumber,
            String targetAccountNumber,
            BigDecimal amount) {

        log.info("💸 Enviando solicitud de transferencia: transactionId={}, from={}, to={}, amount={}",
                transactionId, sourceAccountNumber, targetAccountNumber, amount);

        // Para transferencias, validamos la cuenta origen (débito)
        return sendValidationRequest(
                transactionId,
                sourceAccountNumber,
                amount,
                "TRANSFER",
                targetAccountNumber
        );
    }

    // ========== PRIVATE HELPERS ==========

    /**
     * Método genérico para enviar solicitudes de validación (sin target account)
     */
    private Mono<AccountValidationResponse> sendValidationRequest(
            String transactionId,
            String accountNumber,
            BigDecimal amount,
            String transactionType) {

        return sendValidationRequest(transactionId, accountNumber, amount, transactionType, null);
    }

    /**
     * Método genérico para enviar solicitudes de validación (con target account opcional)
     */
    private Mono<AccountValidationResponse> sendValidationRequest(
            String transactionId,
            String accountNumber,
            BigDecimal amount,
            String transactionType,
            String targetAccountNumber) {

        AccountValidationRequest.Builder requestBuilder = AccountValidationRequest.newBuilder()
                .setTransactionId(transactionId)
                .setAccountNumber(accountNumber)
                .setTransactionType(transactionType)
                .setAmount(amount.doubleValue());

        // Agregar cuenta destino si es transferencia
        if (targetAccountNumber != null) {
            requestBuilder.setTargetAccountNumber(targetAccountNumber);
        }

        AccountValidationRequest request = requestBuilder.build();

        return Mono.create(sink -> {
            responseHandler.register(transactionId, sink);

            kafkaTemplate.send(topicProperties.getAccountValidationRequest(), accountNumber, request)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("❌ Error enviando mensaje a Kafka: transactionId={}, error={}",
                                    transactionId, ex.getMessage(), ex);
                            responseHandler.error(transactionId, ex);
                        } else {
                            log.info("✅ Solicitud enviada a Kafka: transactionId={}, type={}",
                                    transactionId, transactionType);
                        }
                    });
        });
    }
}