package com.nttdata.transaction_service.infrastructure.adapter;

import com.ettdata.avro.AccountValidationRequest;
import com.ettdata.avro.AccountValidationResponse;
import com.nttdata.transaction_service.application.port.out.AccountValidationOutputPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class KafkaAccountValidationAdapter implements AccountValidationOutputPort {

    private static final String REQUEST_TOPIC = "account-validation-request";
    private static final String RESPONSE_TOPIC = "account-validation-response";

    private final KafkaTemplate<String, AccountValidationRequest> kafkaTemplate;

    // Map para asociar transactionId a MonoSink
    private final ConcurrentMap<String, MonoSink<AccountValidationResponse>> pendingResponses = new ConcurrentHashMap<>();

    @Override
    public Mono<AccountValidationResponse> sendWithdrawRequest(String transactionId, String accountNumber, BigDecimal amount) {
        AccountValidationRequest request = AccountValidationRequest.newBuilder()
                .setTransactionId(transactionId)
                .setAccountNumber(accountNumber)
                .setTransactionType("WITHDRAW")
                .setAmount(amount.doubleValue())
                .build();

        return Mono.create(sink -> {
            // Guardamos el sink para completar cuando llegue la respuesta
            pendingResponses.put(transactionId, sink);
            log.info("🔑 Sink registrado para transactionId: {}. Total pendientes: {}",
                    transactionId, pendingResponses.size());

            // ✅ JAVA 17 - CompletableFuture con whenComplete
            kafkaTemplate.send(REQUEST_TOPIC, accountNumber, request)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("❌ Error enviando mensaje a Kafka: {}", ex.getMessage(), ex);
                            pendingResponses.remove(transactionId);
                            sink.error(ex);
                        } else {
                            log.info("✅ Mensaje de retiro enviado a Kafka: transactionId={}, accountNumber={}, amount={}",
                                    transactionId, accountNumber, amount);
                        }
                    });
        });
    }

    // Consumer para escuchar respuestas
    @KafkaListener(topics = RESPONSE_TOPIC, groupId = "transaction-service-group")
    public void consumeAccountValidationResponse(AccountValidationResponse response) {
        String transactionId = response.getTransactionId().toString(); // ⚠️ Convertir CharSequence a String

        log.info("📨 Mensaje recibido de Kafka - TransactionId: {}", transactionId);
        log.info("📨 Contenido: codResponse={}, messageResponse={}",
                response.getCodResponse(), response.getMessageResponse());

        MonoSink<AccountValidationResponse> sink = pendingResponses.remove(transactionId);

        if (sink != null) {
            log.info("✅ Sink encontrado y completado para transactionId: {}", transactionId);
            sink.success(response);
        } else {
            log.warn("⚠️ No se encontró sink para transactionId: {}. Sinks pendientes: {}",
                    transactionId, pendingResponses.keySet());
        }
    }
}