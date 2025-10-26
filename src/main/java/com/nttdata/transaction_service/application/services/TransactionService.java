package com.nttdata.transaction_service.application.services;

import com.nttdata.transaction_service.application.port.in.TransactionInputPort;
import com.nttdata.transaction_service.application.port.out.AccountValidationOutputPort;
import com.nttdata.transaction_service.application.port.out.TransactionRepositoryOutputPort;
import com.nttdata.transaction_service.domain.dto.TransactionListResponse;
import com.nttdata.transaction_service.domain.dto.TransactionResponse;
import com.nttdata.transaction_service.domain.error.AccountValidationException;
import com.nttdata.transaction_service.domain.error.TransactionPersistenceException;
import com.nttdata.transaction_service.domain.model.Transaction;
import com.nttdata.transaction_service.infrastructure.dto.DepositRequest;
import com.nttdata.transaction_service.infrastructure.dto.TransferRequest;
import com.nttdata.transaction_service.infrastructure.dto.WithdrawalRequest;
import com.nttdata.transaction_service.infrastructure.utils.TransactionMapper;
import com.nttdata.transaction_service.infrastructure.utils.TransactionValidator;
import jakarta.validation.ValidationException;
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
    private final TransactionMapper mapper;
    private final AccountValidationOutputPort accountValidation;

    // ========== PUBLIC METHODS ==========

    @Override
    public Mono<TransactionListResponse> getAllTransactionsByAccountNumber(String accountNumber) {
        log.info("📋 Consultando transacciones: cuenta={}", accountNumber);

        return transactionRepository.findAllTransactionByAccountNumber(accountNumber)
                .collectList()
                .map(mapper::toTransactionListResponse)
                .doOnSuccess(response ->
                        log.debug("✅ {} transacciones encontradas", response.getData().size()))
                .doOnError(error ->
                        log.error("❌ Error consultando transacciones: {}", error.getMessage()));
    }

    @Override
    public Mono<TransactionResponse> deposit(DepositRequest request) {
        log.info("💵 Iniciando depósito: cuenta={}, monto={}",
                request.getNumberAccount(), request.getAmount());

        return validateDepositRequest(request)
                .flatMap(this::createAndSaveDepositTransaction)
                .flatMap(this::validateDepositWithAccount)
                .onErrorResume(this::handleDepositError);
    }

    @Override
    public Mono<TransactionResponse> transfer(TransferRequest request) {
        log.info("💸 Iniciando transferencia: desde={}, hacia={}, monto={}",
                request.getSourceNumberAccount(),
                request.getTargetNumberAccount(),
                request.getAmount());

        return validateTransferRequest(request)
                .flatMap(this::executeTransfer)
                .onErrorResume(this::handleTransferError);
    }

    @Override
    public Mono<TransactionResponse> withdraw(WithdrawalRequest request) {
        log.info("💰 Iniciando retiro: cuenta={}, monto={}",
                request.getNumberAccount(), request.getAmount());

        return validateWithdrawalRequest(request)
                .flatMap(this::createAndSaveWithdrawTransaction)
                .flatMap(this::validateWithdrawWithAccount)
                .onErrorResume(this::handleWithdrawalError);
    }

    // ========== DEPOSIT FLOW ==========

    private Mono<DepositRequest> validateDepositRequest(DepositRequest request) {
        return validator.validateDepositReactive(request)
                .flatMap(error -> {
                    log.warn("⚠️ Validación de depósito fallida: {}", error);
                    return Mono.error(new ValidationException(error));
                })
                .then(Mono.just(request));
    }

    private Mono<Transaction> createAndSaveDepositTransaction(DepositRequest request) {
        return Mono.fromCallable(() -> mapper.toDepositTransaction(request))
                .doOnNext(tx -> log.debug("🔄 Depósito mapeado: id={}", tx.getTransactionId()))
                .flatMap(this::saveTransaction);
    }

    private Mono<TransactionResponse> validateDepositWithAccount(Transaction transaction) {
        log.info("📤 Validando depósito con account-service: transactionId={}",
                transaction.getTransactionId());

        return accountValidation.sendDepositRequest(
                        transaction.getTransactionId(),
                        transaction.getAccountNumber(),
                        transaction.getAmount()
                )
                .doOnNext(response ->
                        log.info("📨 Respuesta de account-service: status={}",
                                response.getCodResponse()))
                .map(kafkaResponse -> mapper.toResponseFromKafka(kafkaResponse, transaction))
                .onErrorMap(e -> {
                    log.error("❌ Error validando depósito", e);
                    return new AccountValidationException("Error validando depósito: " + e.getMessage());
                });
    }

    private Mono<TransactionResponse> handleDepositError(Throwable error) {
        log.error("💥 Error en depósito: {}", error.getMessage(), error);
        return handleError(error, "depósito");
    }

    // ========== TRANSFER FLOW ==========

    private Mono<TransferRequest> validateTransferRequest(TransferRequest request) {
        return validator.validateTransferReactive(request)
                .flatMap(error -> {
                    log.warn("⚠️ Validación de transferencia fallida: {}", error);
                    return Mono.error(new ValidationException(error));
                })
                .then(Mono.just(request));
    }

    private Mono<TransactionResponse> executeTransfer(TransferRequest request) {
        Transaction outTransaction = mapper.toTransferOutTransaction(request);

        return saveTransaction(outTransaction)
                .flatMap(savedOut -> validateTransferWithAccount(savedOut, request))
                .flatMap(response -> {
                    // Si la validación fue exitosa, crear la transacción de entrada
                    if (response.getCodResponse() == 200) {
                        Transaction inTransaction = mapper.toTransferInTransaction(request);
                        return saveTransaction(inTransaction)
                                .map(savedIn -> {
                                    log.info("✅ Transferencia completada: {} → {}",
                                            request.getSourceNumberAccount(),
                                            request.getTargetNumberAccount());
                                    return response;
                                });
                    }
                    return Mono.just(response);
                });
    }

    private Mono<TransactionResponse> validateTransferWithAccount(
            Transaction transaction,
            TransferRequest request) {

        log.info("📤 Validando transferencia con account-service: transactionId={}",
                transaction.getTransactionId());

        return accountValidation.sendTransferRequest(
                        transaction.getTransactionId(),
                        request.getSourceNumberAccount(),
                        request.getTargetNumberAccount(),
                        transaction.getAmount().abs()
                )
                .doOnNext(response ->
                        log.info("📨 Respuesta de account-service: status={}",
                                response.getCodResponse()))
                .map(kafkaResponse -> mapper.toResponseFromKafka(kafkaResponse, transaction))
                .onErrorMap(e -> {
                    log.error("❌ Error validando transferencia", e);
                    return new AccountValidationException("Error validando transferencia: " + e.getMessage());
                });
    }

    private Mono<TransactionResponse> handleTransferError(Throwable error) {
        log.error("💥 Error en transferencia: {}", error.getMessage(), error);
        return handleError(error, "transferencia");
    }

    // ========== WITHDRAWAL FLOW ==========

    private Mono<WithdrawalRequest> validateWithdrawalRequest(WithdrawalRequest request) {
        return validator.validateWithdrawalReactive(request)
                .flatMap(error -> {
                    log.warn("⚠️ Validación de retiro fallida: {}", error);
                    return Mono.error(new ValidationException(error));
                })
                .then(Mono.just(request));
    }

    private Mono<Transaction> createAndSaveWithdrawTransaction(WithdrawalRequest request) {
        return Mono.fromCallable(() -> mapper.toWithdrawalTransaction(request))
                .doOnNext(tx -> log.debug("🔄 Retiro mapeado: id={}", tx.getTransactionId()))
                .flatMap(this::saveTransaction);
    }

    private Mono<TransactionResponse> validateWithdrawWithAccount(Transaction transaction) {
        log.info("📤 Validando retiro con account-service: transactionId={}",
                transaction.getTransactionId());

        return accountValidation.sendWithdrawRequest(
                        transaction.getTransactionId(),
                        transaction.getAccountNumber(),
                        transaction.getAmount().abs()
                )
                .doOnNext(response ->
                        log.info("📨 Respuesta de account-service: status={}",
                                response.getCodResponse()))
                .map(kafkaResponse -> mapper.toResponseFromKafka(kafkaResponse, transaction))
                .onErrorMap(e -> {
                    log.error("❌ Error validando retiro", e);
                    return new AccountValidationException("Error validando retiro: " + e.getMessage());
                });
    }

    private Mono<TransactionResponse> handleWithdrawalError(Throwable error) {
        log.error("💥 Error en retiro: {}", error.getMessage(), error);
        return handleError(error, "retiro");
    }

    // ========== SHARED HELPERS ==========

    private Mono<Transaction> saveTransaction(Transaction transaction) {
        return transactionRepository.saveTransaction(transaction)
                .doOnSuccess(saved ->
                        log.info("✅ Transacción guardada: id={}, tipo={}",
                                saved.getTransactionId(),
                                saved.getTransactionType()))
                .onErrorMap(e -> {
                    log.error("❌ Error guardando transacción: {}", e.getMessage());
                    return new TransactionPersistenceException(
                            "Error guardando transacción: " + e.getMessage());
                });
    }

    private Mono<TransactionResponse> handleError(Throwable error, String operationType) {
        if (error instanceof ValidationException) {
            return Mono.just(mapper.toErrorResponse(400, error.getMessage()));
        }

        if (error instanceof TransactionPersistenceException) {
            return Mono.just(mapper.toErrorResponse(500,
                    "Error guardando " + operationType));
        }

        if (error instanceof AccountValidationException) {
            return Mono.just(mapper.toErrorResponse(503,
                    "Servicio de cuentas no disponible"));
        }

        return Mono.just(mapper.toErrorResponse(500,
                "Error interno procesando " + operationType));
    }
}