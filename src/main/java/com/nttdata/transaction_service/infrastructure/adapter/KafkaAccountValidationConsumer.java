package com.nttdata.transaction_service.infrastructure.adapter;


import com.ettdata.avro.AccountValidationResponse;
import com.nttdata.transaction_service.infrastructure.handler.PendingResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaAccountValidationConsumer {

    private final PendingResponseHandler responseHandler;

    @KafkaListener(
            topics = "${kafka.topics.account-validation-response}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeAccountValidationResponse(AccountValidationResponse response) {
        String transactionId = String.valueOf(response.getTransactionId());

        log.info("📨 Respuesta recibida: transactionId={}, status={}",
                transactionId, response.getCodResponse());

        boolean completed = responseHandler.complete(transactionId, response);

        if (!completed) {
            log.warn("⚠️ Respuesta para transacción no esperada: {}", transactionId);
        }
    }

}
