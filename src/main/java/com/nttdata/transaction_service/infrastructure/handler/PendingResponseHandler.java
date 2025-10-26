package com.nttdata.transaction_service.infrastructure.handler;

import com.ettdata.avro.AccountValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.MonoSink;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Slf4j
public class PendingResponseHandler {
    private final ConcurrentMap<String, MonoSink<AccountValidationResponse>> pendingResponses =
            new ConcurrentHashMap<>();

    /**
     * Registra un sink para una transacción pendiente
     */
    public void register(String transactionId, MonoSink<AccountValidationResponse> sink) {
        pendingResponses.put(transactionId, sink);
        log.debug("🔑 Sink registrado para transactionId: {}. Total pendientes: {}",
                transactionId, pendingResponses.size());
    }

    /**
     * Completa un sink cuando llega la respuesta
     */
    public boolean complete(String transactionId, AccountValidationResponse response) {
        MonoSink<AccountValidationResponse> sink = pendingResponses.remove(transactionId);

        if (sink != null) {
            log.info("✅ Completando sink para transactionId: {}", transactionId);
            sink.success(response);
            return true;
        } else {
            log.warn("⚠️ No se encontró sink para transactionId: {}. Pendientes actuales: {}",
                    transactionId, pendingResponses.keySet());
            return false;
        }
    }

    /**
     * Cancela un sink por error
     */
    public void error(String transactionId, Throwable error) {
        MonoSink<AccountValidationResponse> sink = pendingResponses.remove(transactionId);

        if (sink != null) {
            log.error("❌ Error en sink para transactionId: {}", transactionId, error);
            sink.error(error);
        }
    }
    /**
     * Obtiene el número de respuestas pendientes
     */
    public int getPendingCount() {
        return pendingResponses.size();
    }

    /**
     * Limpia sinks antiguos (útil para evitar memory leaks)
     */
    public void cleanup() {
        int count = pendingResponses.size();
        if (count > 0) {
            log.warn("🧹 Limpiando {} sinks pendientes", count);
            pendingResponses.clear();
        }
    }
}
