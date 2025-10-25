package com.nttdata.transaction_service.infrastructure.adapter;

import com.ettdata.avro.AccountValidationRequest;
import com.ettdata.avro.AccountValidationResponse;
import com.nttdata.transaction_service.application.port.out.AccountValidationOutputPort;
import com.nttdata.transaction_service.domain.dto.AccountValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaAccountValidationAdapter implements AccountValidationOutputPort {

  @Value("${transaction.validation.timeout-seconds:10}")
  private int timeoutSeconds;

  private static final String REQUEST_TOPIC = "account-validation-request";

  // Cache para almacenar las respuestas pendientes
  private final Map<String, Sinks.One<AccountValidationResponse>> pendingValidations =
        new ConcurrentHashMap<>();

  @Override
  public Mono<AccountValidationResult> validateAccount(String accountNumber, String transactionType, Double amount) {
    String transactionId = UUID.randomUUID().toString();
    log.info("🔍 Solicitando validación - Account: {}, Type: {}, Amount: {}",
          accountNumber, transactionType, amount);

    Sinks.One<AccountValidationResponse> responseSink = Sinks.one();

    return Mono.<AccountValidationResult>create(sink -> {
      try {
        AccountValidationRequest request = AccountValidationRequest.newBuilder()
              .setTransactionId(transactionId)
              .setAccountNumber(accountNumber)
              .setCustomerId("")
              .setAccountType("")
              .setAmount(amount)
              .setBalance(null)
              .setMinimumOpeningAmount(null)
              .setTransactionType(transactionType)
              .build();

        pendingValidations.put(transactionId, responseSink);

        kafkaTemplate.send(REQUEST_TOPIC, accountNumber, request)
              .addCallback(
                    result -> log.info("✅ Solicitud enviada - TxId: {}", transactionId),
                    ex -> {
                      log.error("❌ Error enviando solicitud", ex);
                      pendingValidations.remove(transactionId);
                      sink.error(ex);
                    }
              );

        responseSink.asMono()
              .timeout(Duration.ofSeconds(timeoutSeconds))
              .map(this::mapToResult)
              .doOnSuccess(result -> {
                log.info("✅ Validación completada - Valid: {}", result.isValid());
                sink.success(result);
              })
              .doOnError(error -> {
                log.error("❌ Timeout o error: {}", error.getMessage());
                pendingValidations.remove(transactionId);
                sink.error(new RuntimeException("Validation timeout", error));
              })
              .subscribe();

      } catch (Exception e) {
        log.error("❌ Error creando solicitud", e);
        sink.error(e);
      }
    });
  }
}
