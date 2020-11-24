package il.co.dsp211;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQSMethods {

    private final static Region region = Region.US_EAST_1;

    public static String createQueue(SqsClient sqsClient, String queueName) {
        System.out.println("Creating queue...");

        sqsClient.createQueue(CreateQueueRequest.builder()
                .queueName(queueName)
                .build());

        GetQueueUrlResponse getQueueUrlResponse = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build());

        System.out.println("Queue \"" + queueName + "\" was created successfully");

        return getQueueUrlResponse.queueUrl();
    }

    public static void sendSingleMessage(SqsClient sqsClient, String queueURL, String message) {
        System.out.println("Sending message...");
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueURL)
                .messageBody(message)
                .build());
        System.out.println("Message sent to SQS url: " + queueURL + " | Message: " + message);
    }

    public static void sendMessageBatch(SqsClient sqsClient, String queueURL, List<String> messages) {
        sendMessageBatch(sqsClient, queueURL, messages.toArray(new String[0]));
    }

    public static void sendMessageBatch(SqsClient sqsClient, String queueURL, String... messages) {
        System.out.println("Sending message...");
        sqsClient.sendMessageBatch(SendMessageBatchRequest.builder()
                .queueUrl(queueURL)
                .entries(Stream.of(messages)
                        .map(message -> SendMessageBatchRequestEntry.builder()
                                .messageBody(message)
                                .build())
                        .toArray(SendMessageBatchRequestEntry[]::new))
                .build());
        System.out.println("Messages sent to SQS url: " + queueURL + " | Messages: " + Arrays.toString(messages));
    }

    public static List<Message> receiveMessage(SqsClient sqsClient, String queueURL) {
        System.out.println("Sending message...");
        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueURL)
                .build())
                .messages();
        System.out.println("Received " + messages.size() + " messages: " + messages.stream()
                .map(Message::body)
                .collect(Collectors.toList()));
        return messages;
    }

    public static void deleteMessage(SqsClient sqsClient, String queueURL, Message message) {
        System.out.println("Deleting message \"" + message.body() + "\"...");
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueURL)
                .receiptHandle(message.receiptHandle())
                .build());
        System.out.println("Message deleted");
    }

    public static void deleteMessageBatch(SqsClient sqsClient, String queueURL, List<Message> messages) {
        System.out.println("Deleting messages " + messages.stream()
                .map(Message::body)
                .collect(Collectors.toList()) + "\"...");
        sqsClient.deleteMessageBatch(DeleteMessageBatchRequest.builder()
                .queueUrl(queueURL)
                .entries(messages.stream()
                        .map(message -> DeleteMessageBatchRequestEntry.builder()
                                .receiptHandle(message.receiptHandle())
                                .build())
                        .toArray(DeleteMessageBatchRequestEntry[]::new))
                .build());
        System.out.println("Messages deleted");
    }
}
