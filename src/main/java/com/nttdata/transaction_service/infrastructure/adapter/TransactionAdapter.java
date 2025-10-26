package com.nttdata.transaction_service.infrastructure.adapter;

import com.nttdata.transaction_service.application.port.out.TransactionRepositoryOutputPort;
import com.nttdata.transaction_service.domain.model.Transaction;
import com.nttdata.transaction_service.infrastructure.entity.TransactionEntity;
import com.nttdata.transaction_service.infrastructure.repository.TransactionRepository;
import com.nttdata.transaction_service.infrastructure.utils.TransactionMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class TransactionAdapter implements TransactionRepositoryOutputPort {

  private final TransactionRepository repository;
  private final TransactionMapper mapper;

    public TransactionAdapter(TransactionRepository repository, TransactionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
  public Flux<Transaction> findAllTransactionByAccountNumber(String accountNumber) {
    return null;
  }

  @Override
  public Mono<Transaction> saveTransaction(Transaction transaction) {
    TransactionEntity entity = mapper.toEntity(transaction);
    return repository.save(entity)
            .map(mapper::toDomain);
  }
}
