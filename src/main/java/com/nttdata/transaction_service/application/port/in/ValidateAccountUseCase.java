package com.nttdata.transaction_service.application.port.in;

import com.ettdata.avro.AccountValidationResponse;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface ValidateAccountUseCase {
    /**
     * Válida si una cuenta puede realizar un retiro
     * @param transactionId ID de la transacción
     * @param accountNumber número de cuenta
     * @param amount monto a retirar
     * @return respuesta de validación
     */
    Mono<AccountValidationResponse> validateWithdraw(String transactionId, String accountNumber, BigDecimal amount);
}
