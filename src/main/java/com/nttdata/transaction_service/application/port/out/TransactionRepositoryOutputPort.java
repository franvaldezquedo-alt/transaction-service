package com.nttdata.transaction_service.application.port.out;

import com.nttdata.transaction_service.domain.model.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionRepositoryOutputPort {

  Flux<Transaction> findAllTransactionByAccountNumber(String accountNumber);
  Mono<Transaction> saveTransaction(Transaction transaction);
}
