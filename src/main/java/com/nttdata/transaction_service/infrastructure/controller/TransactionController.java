package com.nttdata.transaction_service.infrastructure.controller;

import com.nttdata.transaction_service.application.port.in.TransactionInputPort;
import com.nttdata.transaction_service.domain.dto.TransactionListResponse;
import com.nttdata.transaction_service.domain.dto.TransactionResponse;
import com.nttdata.transaction_service.infrastructure.dto.DepositRequest;
import com.nttdata.transaction_service.infrastructure.dto.TransferRequest;
import com.nttdata.transaction_service.infrastructure.dto.WithdrawalRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

  private final TransactionInputPort transactionInputService;

  public TransactionController(TransactionInputPort transactionInputService) {
    this.transactionInputService = transactionInputService;
  }

  @PostMapping("/deposit")
  Mono<TransactionResponse> deposit(@Valid @RequestBody DepositRequest depositRequest) {
    return transactionInputService.deposit(depositRequest);
  }

  @GetMapping("/{accountNumber}")
  Mono<TransactionListResponse> getAllTransactionsByAccountNumber(@PathVariable String accountNumber) {
    return transactionInputService.getAllTransactionsByAccountNumber(accountNumber);
  }

  @PostMapping("/transfer")
  Mono<TransactionResponse> transfer(@Valid  @RequestBody TransferRequest transferRequest) {
    return transactionInputService.transfer(transferRequest);
  }

  @PostMapping("/withdraw")
  Mono<TransactionResponse> withdraw(@Valid  @RequestBody WithdrawalRequest withdrawalRequest) {
    return transactionInputService.withdraw(withdrawalRequest);
  }
}
