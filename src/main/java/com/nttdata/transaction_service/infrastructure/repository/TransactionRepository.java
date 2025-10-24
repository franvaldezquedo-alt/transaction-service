package com.nttdata.transaction_service.infrastructure.repository;

import com.nttdata.transaction_service.infrastructure.entity.TransactionEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface TransactionRepository extends ReactiveMongoRepository<TransactionEntity, String> {

}
