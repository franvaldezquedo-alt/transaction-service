package com.nttdata.transaction_service.infrastructure.adapter;

import com.nttdata.transaction_service.application.port.out.TransactionRepositoryOutputPort;
import com.nttdata.transaction_service.domain.model.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TransactionAdapter implements TransactionRepositoryOutputPort {
  @Override
  public Flux<Transaction> findAllTransactionByAccountNumber(String accountNumber) {
    return null;
  }

  @Override
  public Mono<Transaction> saveTransaction(Transaction transaction) {
    return null;
  }
}
