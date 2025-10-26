package com.nttdata.transaction_service.infrastructure.utils;

import com.nttdata.transaction_service.infrastructure.dto.WithdrawalRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;

@Component
public class TransactionValidator {

  // Validación "normal" que devuelve String o null
  public String validateWithdrawal(WithdrawalRequest request) {
    if (request.getAmount() == null || request.getAmount().signum() <= 0) {
      return "El monto de retiro debe ser mayor a cero";
    }
    if (request.getNumberAccount() == null || request.getNumberAccount().isBlank()) {
      return "Número de cuenta inválido";
    }
    return null; // validación exitosa
  }

  // Versión reactiva
  public Mono<String> validateWithdrawalReactive(WithdrawalRequest request) {
    String error = validateWithdrawal(request);
    if (error != null) {
      return Mono.just(error);
    }
    return Mono.empty();
  }
}
