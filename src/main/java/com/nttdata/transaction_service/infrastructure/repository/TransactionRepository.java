package com.nttdata.transaction_service.infrastructure.repository;

import com.nttdata.transaction_service.infrastructure.entity.TransactionEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface TransactionRepository extends ReactiveMongoRepository<TransactionEntity, String> {
 Flux<TransactionEntity> findAllTransactionByAccountNumber(String accountNumber);
}
