package com.nttdata.transaction_service.domain.dto;

import com.nttdata.transaction_service.domain.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionListResponse {
  private List<Transaction> data;
  private String Error;
}
