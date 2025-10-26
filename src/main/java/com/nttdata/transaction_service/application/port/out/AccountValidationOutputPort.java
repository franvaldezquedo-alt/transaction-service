package com.nttdata.transaction_service.application.port.out;

import com.ettdata.avro.AccountValidationResponse;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface AccountValidationOutputPort {
  Mono<AccountValidationResponse> sendWithdrawRequest(String transactionId, String accountNumber, BigDecimal amount);

}
