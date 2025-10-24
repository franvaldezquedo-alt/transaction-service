package com.nttdata.transaction_service.domain.model;

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
public class Transaction {
  private String transactionId;
  private String accountNumber;
  private LocalDateTime transactionDate;
  private TransactionType transactionType;
  private BigDecimal amount;
  private String description;
}
