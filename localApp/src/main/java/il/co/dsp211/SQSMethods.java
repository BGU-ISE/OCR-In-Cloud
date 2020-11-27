package il.co.dsp211;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQSMethods implements AutoCloseable
{
	private final SqsClient sqsClient = SqsClient.builder()
			.region(Region.US_EAST_1)
			.build();

	public void deleteQueue(String queueURL)
	{
		sqsClient.deleteQueue(DeleteQueueRequest.builder()
				.queueUrl(queueURL)
				.build());
	}

	public void deleteMessageBatch(String queueURL, List<Message> messages)
	{
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

	public void deleteMessage(String queueURL, Message message)
	{
		System.out.println("Deleting message \"" + message.body() + "\"...");
		sqsClient.deleteMessage(DeleteMessageRequest.builder()
				.queueUrl(queueURL)
				.receiptHandle(message.receiptHandle())
				.build());
		System.out.println("Message deleted");
	}

	public List<Message> receiveMessage(String queueURL)
	{
		System.out.println("Receiving messages...");
		List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
				.queueUrl(queueURL)
				.build())
				.messages();
		System.out.println("Received " + messages.size() + " messages: " + messages.stream()
				.map(Message::body)
				.collect(Collectors.toList()));
		return messages;
	}

	public void sendMessageBatch(String queueURL, String... messages)
	{
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

	public void sendMessageBatch(String queueURL, List<String> messages)
	{
		sendMessageBatch(queueURL, messages.toArray(new String[0]));
	}

	public void sendSingleMessage(String queueURL, String message)
	{
		System.out.println("Sending message...");

		sqsClient.sendMessage(SendMessageRequest.builder()
				.queueUrl(queueURL)
				.messageBody(message)
				.build());

		System.out.println("Message sent to SQS url: " + queueURL + " | Message: " + message);
	}

	public String createQueue(String queueName)
	{
		System.out.println("Creating queue...");

		sqsClient.createQueue(CreateQueueRequest.builder()
				.queueName(queueName)
				.build());

		System.out.println("Queue \"" + queueName + "\" was created successfully");

		return getQueueUrl(queueName);
	}

	public String getQueueUrl(String queueName)
	{
		System.out.println("Getting queue URL for queue " + queueName + "...");

		GetQueueUrlResponse getQueueUrlResponse = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
				.queueName(queueName)
				.build());

		System.out.println("Got URL");
		return getQueueUrlResponse.queueUrl();
	}

	@Override
	public void close()
	{
		sqsClient.close();
	}
}
