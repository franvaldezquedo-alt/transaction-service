package com.nttdata.transaction_service.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nttdata.transaction_service.domain.model.Transaction;
import com.nttdata.transaction_service.domain.model.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {

  // ========== Información de Estado ==========
  private Integer codResponse;        // Código HTTP (200, 400, 500, etc.)
  private String status;              // COMPLETED, REJECTED, ERROR, PENDING
  private String messageResponse;     // Mensaje descriptivo

  // ========== Información de la Transacción ==========
  private String transactionId;
  private String accountNumber;
  private BigDecimal amount;
  private TransactionType transactionType;
  private String description;
  private LocalDateTime timestamp;

  // ========== Campos Legacy (Compatibilidad) ==========
  @Deprecated
  private String codEntity;           // Mantener por compatibilidad, usar transactionId

  // ========== Factory Methods ==========

  public static TransactionResponse success(Transaction transaction) {
    return TransactionResponse.builder()
            .codResponse(200)
            .status("COMPLETED")
            .messageResponse("Operación procesada exitosamente")
            .transactionId(transaction.getTransactionId())
            .accountNumber(transaction.getAccountNumber())
            .amount(transaction.getAmount())
            .transactionType(transaction.getTransactionType())
            .description(transaction.getDescription())
            .timestamp(LocalDateTime.now())
            .codEntity(transaction.getTransactionId()) // Legacy
            .build();
  }

  public static TransactionResponse rejected(Transaction transaction, int code, String reason) {
    return TransactionResponse.builder()
            .codResponse(code)
            .status("REJECTED")
            .messageResponse(reason)
            .transactionId(transaction.getTransactionId())
            .accountNumber(transaction.getAccountNumber())
            .amount(transaction.getAmount())
            .transactionType(transaction.getTransactionType())
            .timestamp(LocalDateTime.now())
            .codEntity(transaction.getTransactionId()) // Legacy
            .build();
  }

  public static TransactionResponse error(int code, String message) {
    return TransactionResponse.builder()
            .codResponse(code)
            .status("ERROR")
            .messageResponse(message)
            .timestamp(LocalDateTime.now())
            .codEntity(null) // Legacy
            .build();
  }
}