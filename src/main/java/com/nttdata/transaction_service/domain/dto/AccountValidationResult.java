package com.nttdata.transaction_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountValidationResult {
  private String transactionId;
  private String accountNumber;
  private boolean isValid;
  private String reason;
  private Double currentBalance;
}
