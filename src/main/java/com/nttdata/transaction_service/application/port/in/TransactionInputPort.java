package com.nttdata.transaction_service.application.port.in;

import com.nttdata.transaction_service.domain.dto.TransactionListResponse;
import com.nttdata.transaction_service.domain.dto.TransactionResponse;
import com.nttdata.transaction_service.infrastructure.dto.DepositRequest;
import com.nttdata.transaction_service.infrastructure.dto.TransferRequest;
import com.nttdata.transaction_service.infrastructure.dto.WithdrawalRequest;
import reactor.core.publisher.Mono;

public interface TransactionInputPort {
  Mono<TransactionListResponse> getAllTransactionsByAccountNumber(String accountNumber);
  Mono<TransactionResponse> deposit(DepositRequest transactionResponse);
  Mono<TransactionResponse> transfer(TransferRequest transferRequest);
  Mono<TransactionResponse> withdraw(WithdrawalRequest withdrawalRequest );
}
