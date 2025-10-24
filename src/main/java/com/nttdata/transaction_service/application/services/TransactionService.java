package com.nttdata.transaction_service.application.services;

import com.nttdata.transaction_service.application.port.in.TransactionInputPort;
import com.nttdata.transaction_service.application.port.out.TransactionRepositoryOutputPort;
import com.nttdata.transaction_service.domain.dto.TransactionListResponse;
import com.nttdata.transaction_service.domain.dto.TransactionResponse;
import com.nttdata.transaction_service.infrastructure.dto.DepositRequest;
import com.nttdata.transaction_service.infrastructure.dto.TransferRequest;
import com.nttdata.transaction_service.infrastructure.dto.WithdrawalRequest;
import com.nttdata.transaction_service.infrastructure.utils.TransactionMapper;
import com.nttdata.transaction_service.infrastructure.utils.TransactionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService implements TransactionInputPort {

  private final TransactionRepositoryOutputPort transactionRepository;
  private final TransactionValidator validator;
  private final TransactionMapper transactionMapper;

  @Override
  public Mono<TransactionListResponse> getAllTransactionsByAccountNumber(String accountNumber) {
    log.info("Retrieving transactions for account: {}", accountNumber);

    return transactionRepository.findAllTransactionByAccountNumber(accountNumber)
          .collectList()
          .map(transactionMapper::toTransactionListResponse)
          .doOnSuccess(response ->
                log.debug("Found {} transactions for account {}",
                      response.getData().size(), accountNumber))
          .doOnError(error ->
                log.error("Error retrieving transactions for account {}: {}",
                      accountNumber, error.getMessage()));
  }

  @Override
  public Mono<TransactionResponse> deposit(DepositRequest transactionResponse) {
    return null;
  }

  @Override
  public Mono<TransactionResponse> transfer(TransferRequest transferRequest) {
    return null;
  }

  @Override
  public Mono<TransactionResponse> withdraw(WithdrawalRequest withdrawalRequest) {
    return null;
  }
}
