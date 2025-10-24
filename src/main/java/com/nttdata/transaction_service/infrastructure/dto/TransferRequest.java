package com.nttdata.transaction_service.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class TransferRequest {
  private String sourceNumberAccount;
  private String targetNumberAccount;
  private BigDecimal amount;
  private String description;
}
