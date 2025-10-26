package com.nttdata.transaction_service.infrastructure.utils;

import com.nttdata.transaction_service.infrastructure.dto.DepositRequest;
import com.nttdata.transaction_service.infrastructure.dto.TransferRequest;
import com.nttdata.transaction_service.infrastructure.dto.WithdrawalRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@Component
public class TransactionValidator {

  // ========== CONSTANTS ==========

  private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
  private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999.99");
  private static final int ACCOUNT_NUMBER_MIN_LENGTH = 6;
  private static final int ACCOUNT_NUMBER_MAX_LENGTH = 20;

  // ========== WITHDRAWAL VALIDATION ==========

  /**
   * Valida una solicitud de retiro
   * @param request solicitud de retiro
   * @return mensaje de error o null si es válido
   */
  public String validateWithdrawal(WithdrawalRequest request) {
    if (request == null) {
      return "La solicitud de retiro no puede ser nula";
    }

    // Validar monto
    String amountError = validateAmount(request.getAmount(), "retiro");
    if (amountError != null) {
      return amountError;
    }

    // Validar número de cuenta
    String accountError = validateAccountNumber(request.getNumberAccount());
    if (accountError != null) {
      return accountError;
    }

    return null; // Validación exitosa
  }

  /**
   * Versión reactiva de validateWithdrawal
   */
  public Mono<String> validateWithdrawalReactive(WithdrawalRequest request) {
    String error = validateWithdrawal(request);
    return error != null ? Mono.just(error) : Mono.empty();
  }

  // ========== DEPOSIT VALIDATION ==========

  /**
   * Valida una solicitud de depósito
   * @param request solicitud de depósito
   * @return mensaje de error o null si es válido
   */
  public String validateDeposit(DepositRequest request) {
    if (request == null) {
      return "La solicitud de depósito no puede ser nula";
    }

    // Validar monto
    String amountError = validateAmount(request.getAmount(), "depósito");
    if (amountError != null) {
      return amountError;
    }

    // Validar número de cuenta
    String accountError = validateAccountNumber(request.getNumberAccount());
    if (accountError != null) {
      return accountError;
    }

    return null; // Validación exitosa
  }

  /**
   * Versión reactiva de validateDeposit
   */
  public Mono<String> validateDepositReactive(DepositRequest request) {
    String error = validateDeposit(request);
    return error != null ? Mono.just(error) : Mono.empty();
  }

  // ========== TRANSFER VALIDATION ==========

  /**
   * Valida una solicitud de transferencia
   * @param request solicitud de transferencia
   * @return mensaje de error o null si es válido
   */
  public String validateTransfer(TransferRequest request) {
    if (request == null) {
      return "La solicitud de transferencia no puede ser nula";
    }

    // Validar monto
    String amountError = validateAmount(request.getAmount(), "transferencia");
    if (amountError != null) {
      return amountError;
    }

    // Validar cuenta origen
    String sourceAccountError = validateAccountNumber(request.getSourceNumberAccount());
    if (sourceAccountError != null) {
      return "Cuenta origen: " + sourceAccountError;
    }

    // Validar cuenta destino
    String targetAccountError = validateAccountNumber(request.getTargetNumberAccount());
    if (targetAccountError != null) {
      return "Cuenta destino: " + targetAccountError;
    }

    // Validar que no sean la misma cuenta
    if (request.getSourceNumberAccount().equals(request.getTargetNumberAccount())) {
      return "Las cuentas de origen y destino no pueden ser iguales";
    }

    return null; // Validación exitosa
  }

  /**
   * Versión reactiva de validateTransfer
   */
  public Mono<String> validateTransferReactive(TransferRequest request) {
    String error = validateTransfer(request);
    return error != null ? Mono.just(error) : Mono.empty();
  }

  // ========== PRIVATE VALIDATION HELPERS ==========

  /**
   * Valida el monto de una transacción
   */
  private String validateAmount(BigDecimal amount, String operationType) {
    if (amount == null) {
      return String.format("El monto de %s no puede ser nulo", operationType);
    }

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      return String.format("El monto de %s debe ser mayor a cero", operationType);
    }

    if (amount.compareTo(MIN_AMOUNT) < 0) {
      return String.format("El monto mínimo para %s es %s", operationType, MIN_AMOUNT);
    }

    if (amount.compareTo(MAX_AMOUNT) > 0) {
      return String.format("El monto máximo para %s es %s", operationType, MAX_AMOUNT);
    }

    // Validar máximo 2 decimales
    if (amount.scale() > 2) {
      return String.format("El monto de %s no puede tener más de 2 decimales", operationType);
    }

    return null;
  }

  /**
   * Valida el número de cuenta
   */
  private String validateAccountNumber(String accountNumber) {
    if (accountNumber == null || accountNumber.isBlank()) {
      return "El número de cuenta no puede estar vacío";
    }

    String trimmedAccount = accountNumber.trim();

    if (trimmedAccount.length() < ACCOUNT_NUMBER_MIN_LENGTH) {
      return String.format("El número de cuenta debe tener al menos %d caracteres",
              ACCOUNT_NUMBER_MIN_LENGTH);
    }

    if (trimmedAccount.length() > ACCOUNT_NUMBER_MAX_LENGTH) {
      return String.format("El número de cuenta no puede exceder %d caracteres",
              ACCOUNT_NUMBER_MAX_LENGTH);
    }


    return null;
  }
}