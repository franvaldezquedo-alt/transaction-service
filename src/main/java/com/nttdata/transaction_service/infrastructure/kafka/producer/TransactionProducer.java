package com.nttdata.transaction_service.infrastructure.kafka.producer;

import com.ettdata.avro.AccountValidationRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionProducer {
    /*private static final String TOPIC = "account-validation-request";

    private final KafkaTemplate<String, AccountValidationRequest> kafkaTemplate;

    public TransactionProducer(KafkaTemplate<String, AccountValidationRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendWithdrawRequest(String transactionId, String accountNumber, double amount) {
        AccountValidationRequest request = AccountValidationRequest.newBuilder()
                .setTransactionId(transactionId)
                .setAccountNumber(accountNumber)
                .setTransactionType("WITHDRAW")
                .setAmount(amount)
                .build();

        kafkaTemplate.send(TOPIC, accountNumber, request);
        System.out.println("Mensaje enviado a Kafka: Retiro de " + amount + " de la cuenta " + accountNumber);
    }*/
}
