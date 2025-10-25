package com.nttdata.transaction_service.application.port.out;

import com.nttdata.transaction_service.domain.dto.AccountValidationResult;
import reactor.core.publisher.Mono;

public interface AccountValidationOutputPort {
  Mono<AccountValidationResult> validateAccount(String accountNumber,
                                                String transactionType,
                                                Double amount);
}
