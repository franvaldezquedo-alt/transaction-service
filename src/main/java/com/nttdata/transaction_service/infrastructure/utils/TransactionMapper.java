package com.nttdata.transaction_service.infrastructure.utils;

import com.ettdata.avro.AccountValidationResponse;
import com.nttdata.transaction_service.domain.dto.TransactionListResponse;
import com.nttdata.transaction_service.domain.dto.TransactionResponse;
import com.nttdata.transaction_service.domain.model.Transaction;
import com.nttdata.transaction_service.domain.model.enums.TransactionType;
import com.nttdata.transaction_service.infrastructure.dto.DepositRequest;
import com.nttdata.transaction_service.infrastructure.dto.TransferRequest;
import com.nttdata.transaction_service.infrastructure.dto.WithdrawalRequest;
import com.nttdata.transaction_service.infrastructure.entity.TransactionEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TransactionMapper {

  // ========== CONSTANTS ==========

  private static final String DEFAULT_DEPOSIT_DESC = "Cash deposit";
  private static final String DEFAULT_WITHDRAWAL_DESC = "Withdrawal";
  private static final String TRANSFER_TO_PREFIX = "Transfer to ";
  private static final String TRANSFER_FROM_PREFIX = "Transfer from ";

  // ========== ENTITY ↔ DOMAIN ==========

  /**
   * Convierte Entity a Domain
   */
  public Transaction toDomain(TransactionEntity entity) {
    if (entity == null) return null;

    return Transaction.builder()
            .transactionId(entity.getTransactionId())
            .accountNumber(entity.getAccountNumber())
            .transactionType(entity.getTransactionType())
            .amount(entity.getAmount())
            .transactionDate(entity.getTransactionDate())
            .description(entity.getDescription())
            .build();
  }

  /**
   * Convierte Domain a Entity
   */
  public TransactionEntity toEntity(Transaction domain) {
    if (domain == null) return null;

    return TransactionEntity.builder()
            .transactionId(domain.getTransactionId())
            .accountNumber(domain.getAccountNumber())
            .transactionType(domain.getTransactionType())
            .amount(domain.getAmount())
            .transactionDate(domain.getTransactionDate())
            .description(domain.getDescription())
            .build();
  }

  /**
   * Convierte lista de Entities a Domain
   */
  public List<Transaction> toDomainList(List<TransactionEntity> entities) {
    if (entities == null || entities.isEmpty()) {
      return Collections.emptyList();
    }

    return entities.stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
  }

  /**
   * Convierte lista de Domain a Entities
   */
  public List<TransactionEntity> toEntityList(List<Transaction> transactions) {
    if (transactions == null || transactions.isEmpty()) {
      return Collections.emptyList();
    }

    return transactions.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
  }

  // ========== REQUEST → DOMAIN ==========

  /**
   * Convierte DepositRequest a Transaction
   */
  public Transaction toDepositTransaction(DepositRequest request) {
    return Transaction.builder()
            .transactionId(generateTransactionId())
            .accountNumber(request.getNumberAccount())
            .transactionType(TransactionType.DEPOSIT)
            .amount(request.getAmount())
            .transactionDate(LocalDateTime.now())
            .description(getOrDefault(request.getDescription(), DEFAULT_DEPOSIT_DESC))
            .build();
  }

  /**
   * Convierte WithdrawalRequest a Transaction
   */
  public Transaction toWithdrawalTransaction(WithdrawalRequest request) {
    return Transaction.builder()
            .transactionId(generateTransactionId())
            .accountNumber(request.getNumberAccount())
            .transactionType(TransactionType.WITHDRAWAL)
            .amount(request.getAmount().negate()) // Negativo para retiros
            .transactionDate(LocalDateTime.now())
            .description(getOrDefault(request.getDescription(), DEFAULT_WITHDRAWAL_DESC))
            .build();
  }

  /**
   * Convierte TransferRequest a Transaction de salida (débito)
   */
  public Transaction toTransferOutTransaction(TransferRequest request) {
    return Transaction.builder()
            .transactionId(generateTransactionId())
            .accountNumber(request.getSourceNumberAccount())
            .transactionType(TransactionType.TRANSFER)
            .amount(request.getAmount().negate()) // Negativo para débito
            .transactionDate(LocalDateTime.now())
            .description(getOrDefault(request.getDescription(),
                    TRANSFER_TO_PREFIX + request.getTargetNumberAccount()))
            .build();
  }

  /**
   * Convierte TransferRequest a Transaction de entrada (crédito)
   */
  public Transaction toTransferInTransaction(TransferRequest request) {
    return Transaction.builder()
            .transactionId(generateTransactionId())
            .accountNumber(request.getTargetNumberAccount())
            .transactionType(TransactionType.TRANSFER)
            .amount(request.getAmount()) // Positivo para crédito
            .transactionDate(LocalDateTime.now())
            .description(getOrDefault(request.getDescription(),
                    TRANSFER_FROM_PREFIX + request.getSourceNumberAccount()))
            .build();
  }

  // ========== DOMAIN → RESPONSE ==========

  /**
   * Crea respuesta exitosa desde Transaction
   */
  public TransactionResponse toSuccessResponse(Transaction transaction) {
    return TransactionResponse.builder()
            .codResponse(200)
            .status("COMPLETED")
            .messageResponse("Transacción procesada exitosamente")
            .transactionId(transaction.getTransactionId())
            .accountNumber(transaction.getAccountNumber())
            .amount(transaction.getAmount())
            .transactionType(transaction.getTransactionType())
            .description(transaction.getDescription())
            .timestamp(transaction.getTransactionDate())
            .codEntity(transaction.getTransactionId()) // Legacy
            .build();
  }

  /**
   * Crea respuesta rechazada desde Transaction
   */
  public TransactionResponse toRejectedResponse(Transaction transaction, int statusCode, String reason) {
    return TransactionResponse.builder()
            .codResponse(statusCode)
            .status("REJECTED")
            .messageResponse(reason)
            .transactionId(transaction.getTransactionId())
            .accountNumber(transaction.getAccountNumber())
            .amount(transaction.getAmount())
            .transactionType(transaction.getTransactionType())
            .timestamp(transaction.getTransactionDate())
            .codEntity(transaction.getTransactionId()) // Legacy
            .build();
  }

  /**
   * Crea respuesta de error genérica
   */
  public TransactionResponse toErrorResponse(int statusCode, String errorMessage) {
    return TransactionResponse.builder()
            .codResponse(statusCode)
            .status("ERROR")
            .messageResponse(errorMessage)
            .timestamp(LocalDateTime.now())
            .codEntity(null) // Legacy
            .build();
  }

  /**
   * Convierte respuesta de Kafka a TransactionResponse
   */
  public TransactionResponse toResponseFromKafka(AccountValidationResponse kafkaResponse, Transaction transaction) {
    if (kafkaResponse.getCodResponse() == 200) {
      return toSuccessResponse(transaction);
    } else {
      return toRejectedResponse(
              transaction,
              kafkaResponse.getCodResponse(),
              kafkaResponse.getMessageResponse() != null
                      ? kafkaResponse.getMessageResponse().toString()
                      : "Sin mensaje"
      );
    }
  }

  // ========== LIST RESPONSES ==========

  /**
   * Convierte lista de Transactions a TransactionListResponse
   */
  public TransactionListResponse toTransactionListResponse(List<Transaction> transactions) {
    if (transactions == null || transactions.isEmpty()) {
      return TransactionListResponse.builder()
              .data(Collections.emptyList())
              .Error(null)
              .build();
    }

    return TransactionListResponse.builder()
            .data(transactions)
            .Error(null)
            .build();
  }

  // ========== LEGACY METHODS (Deprecated) ==========

  /**
   * @deprecated Usar toSuccessResponse(Transaction) en su lugar
   */
  @Deprecated
  public TransactionResponse toSuccessResponse(String message, String transactionId) {
    return TransactionResponse.builder()
            .codResponse(200)
            .messageResponse(message)
            .codEntity(transactionId)
            .transactionId(transactionId)
            .status("COMPLETED")
            .timestamp(LocalDateTime.now())
            .build();
  }

  // ========== PRIVATE HELPERS ==========

  /**
   * Genera un ID único para transacciones
   */
  private String generateTransactionId() {
    return UUID.randomUUID().toString();
  }

  /**
   * Retorna el valor o un default si es null
   */
  private String getOrDefault(String value, String defaultValue) {
    return value != null && !value.isBlank() ? value : defaultValue;
  }
}