package com.nttdata.transaction_service.infrastructure.kafka.consumer;

import com.ettdata.avro.AccountValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AccountValidationResponseConsumer {
    @KafkaListener(topics = "account-validation-response", groupId = "transaction-service-group")
    public void consumeResponse(AccountValidationResponse response) {
        log.info("Recibida respuesta de cuenta: {} - {}", response.getCodResponse(), response.getMessageResponse());

        if(response.getCodResponse() == 200){
            // Continuar con la transacción
            log.info("Transacción aprobada para la cuenta {}", response.getAccountNumber());
        } else {
            // Manejar fallo (por ejemplo, fondos insuficientes)
            log.warn("Transacción rechazada: {}", response.getMessageResponse());
        }
    }
}
