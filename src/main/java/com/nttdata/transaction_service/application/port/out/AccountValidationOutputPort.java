package com.nttdata.transaction_service.application.port.out;

import com.ettdata.avro.AccountValidationResponse;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Puerto de salida para validaciones de cuenta vía Kafka
 */
public interface AccountValidationOutputPort {

  /**
   * Envía solicitud de validación para retiro
   */
  Mono<AccountValidationResponse> sendWithdrawRequest(
          String transactionId,
          String accountNumber,
          BigDecimal amount);

  /**
   * Envía solicitud de validación para depósito
   */
  Mono<AccountValidationResponse> sendDepositRequest(
          String transactionId,
          String accountNumber,
          BigDecimal amount);

  /**
   * Envía solicitud de validación para transferencia (cuenta origen)
   */
  Mono<AccountValidationResponse> sendTransferRequest(
          String transactionId,
          String sourceAccountNumber,
          String targetAccountNumber,
          BigDecimal amount);
}