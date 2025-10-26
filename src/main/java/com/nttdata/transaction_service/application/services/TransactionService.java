package com.nttdata.transaction_service.application.services;

import com.nttdata.transaction_service.application.port.in.TransactionInputPort;
import com.nttdata.transaction_service.application.port.out.AccountValidationOutputPort;
import com.nttdata.transaction_service.application.port.out.TransactionRepositoryOutputPort;
import com.nttdata.transaction_service.domain.dto.TransactionListResponse;
import com.nttdata.transaction_service.domain.dto.TransactionResponse;
import com.nttdata.transaction_service.domain.model.Transaction;
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
    private final AccountValidationOutputPort accountValidationOutputPort;

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
        log.info("Iniciando proceso de retiro para cuenta: {}, monto: {}",
                withdrawalRequest.getNumberAccount(),
                withdrawalRequest.getAmount());

        return validator.validateWithdrawalReactive(withdrawalRequest)
                // 1️⃣ Validación de request
                .flatMap(validationError -> {
                    log.warn("Validación fallida: {}", validationError);
                    return Mono.just(transactionMapper.toErrorResponse(400, validationError));
                })
                // 2️⃣ Si no hay error, continuar
                .switchIfEmpty(
                        Mono.defer(() -> {
                            // Mapear a dominio
                            Transaction transaction;
                            try {
                                transaction = transactionMapper.toWithdrawalTransaction(withdrawalRequest);
                                log.debug("Transaction mapeada: {}", transaction);
                            } catch (Exception e) {
                                log.error("Error mapeando WithdrawalRequest a Transaction", e);
                                return Mono.just(transactionMapper.toErrorResponse(500,
                                        "Error interno mapeando transacción"));
                            }

                            // Guardar en repositorio
                            return transactionRepository.saveTransaction(transaction)
                                    .doOnNext(tx -> log.info("Transacción guardada con ID: {}", tx.getTransactionId()))
                                    .flatMap(savedTx -> {
                                        // Enviar evento a Kafka y ESPERAR respuesta
                                        return accountValidationOutputPort.sendWithdrawRequest(
                                                        savedTx.getTransactionId(),
                                                        savedTx.getAccountNumber(),
                                                        savedTx.getAmount().abs()
                                                )
                                                .doOnNext(response -> log.info("Respuesta recibida de Kafka: código={}, mensaje={}",
                                                        response.getCodResponse(), response.getMessageResponse()))
                                                // Mapear la respuesta de Kafka a TransactionResponse
                                                .map(kafkaResponse ->
                                                        transactionMapper.toResponseFromKafka(kafkaResponse, savedTx)
                                                );
                                    })
                                    // Captura errores inesperados en todo el flujo
                                    .onErrorResume(err -> {
                                        log.error("Error procesando retiro: {}", err.getMessage(), err);
                                        return Mono.just(transactionMapper.toErrorResponse(500,
                                                "Error interno procesando retiro: " + err.getMessage()));
                                    });
                        })
                );
    }


}
