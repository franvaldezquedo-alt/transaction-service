package com.nttdata.transaction_service.application.services;

import com.ettdata.avro.AccountValidationResponse;
import com.nttdata.transaction_service.application.port.in.ValidateAccountUseCase;
import com.nttdata.transaction_service.application.port.out.AccountValidationOutputPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountValidationService implements ValidateAccountUseCase {

    private final AccountValidationOutputPort accountValidationPort;

    @Value("${transaction.validation.timeout-seconds}")
    private int timeoutSeconds;

    @Override
    public Mono<AccountValidationResponse> validateWithdraw(
            String transactionId,
            String accountNumber,
            BigDecimal amount) {

        log.info("🔄 Validando: transactionId={}, account={}, amount={}",
                transactionId, accountNumber, amount);

        return accountValidationPort.sendWithdrawRequest(transactionId, accountNumber, amount)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnSuccess(response ->
                        log.info("✅ Validación exitosa: transactionId={}", transactionId))
                .doOnError(error ->
                        log.error("❌ Error validando: transactionId={}", transactionId))
                .onErrorResume(error -> Mono.just(
                        AccountValidationResponse.newBuilder()
                                .setTransactionId(transactionId)
                                .setAccountNumber(accountNumber)
                                .setCodResponse(503)
                                .setMessageResponse("Servicio no disponible")
                                .build()
                ));
    }

    @Override
    public Mono<AccountValidationResponse> validateDeposit(
            String transactionId,
            String accountNumber,
            BigDecimal amount) {

        log.info("🔄 Validando Deposito: transactionId={}, account={}, amount={}",
                transactionId, accountNumber, amount);

        return accountValidationPort.sendDepositRequest(transactionId, accountNumber, amount)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnSuccess(response ->
                        log.info("✅ Validación de deposito exitosa: transactionId={}", transactionId))
                .doOnError(error ->
                        log.error("❌ Error validando deposito: transactionId={}", transactionId))
                .onErrorResume(error -> Mono.just(
                        AccountValidationResponse.newBuilder()
                                .setTransactionId(transactionId)
                                .setAccountNumber(accountNumber)
                                .setCodResponse(503)
                                .setMessageResponse("Servicio no disponible")
                                .build()
                ));
    }


    @Override
    public Mono<AccountValidationResponse> validateTransfer(
            String transactionId,
            String fromAccountNumber,
            String toAccountNumber,
            BigDecimal amount) {

        log.info("🔄 Validando Transferencia: transactionId={}, fromAccountNumber={}, toAccountNumber={}, amount={}",
                transactionId, fromAccountNumber, toAccountNumber, amount);

        return accountValidationPort.sendTransferRequest(transactionId, fromAccountNumber, toAccountNumber, amount)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnSuccess(response ->
                        log.info("✅ Validación de transferencia exitosa: transactionId={}", transactionId))
                .doOnError(error ->
                        log.error("❌ Error validando transferencia: transactionId={}", transactionId))
                .onErrorResume(error -> Mono.just(
                        AccountValidationResponse.newBuilder()
                                .setTransactionId(transactionId)
                                .setAccountNumber(fromAccountNumber)
                                .setCodResponse(503)
                                .setMessageResponse("Servicio no disponible")
                                .build()
                ));
    }

}
