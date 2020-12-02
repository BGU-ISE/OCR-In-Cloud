package il.co.dsp211;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQSMethods implements AutoCloseable
{
	private static final String SPLITERATOR = "🤠";
	private final SqsClient sqsClient = SqsClient.builder()
			.region(Region.US_EAST_1)
			.build();

	public static String getSPLITERATOR()
	{
		return SPLITERATOR;
	}

	public void deleteQueue(String queueURL)
	{
		System.out.println("Deleting queue...");

		sqsClient.deleteQueue(DeleteQueueRequest.builder()
				.queueUrl(queueURL)
				.build());

		System.out.println("queue deleted");
	}

	public void deleteMessageBatch(String queueURL, List<Message> messages)
	{
		if (messages.isEmpty())
			return;

		System.out.println("Deleting messages " + messages.stream()
				.map(Message::body)
				.collect(Collectors.toList()) + "\"...");

		sqsClient.deleteMessageBatch(DeleteMessageBatchRequest.builder()
				.queueUrl(queueURL)
				.entries(messages.stream()
						.map(message -> DeleteMessageBatchRequestEntry.builder()
								.id(message.messageId())
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

		List<Message> messages;
//		do
//		{
		messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
				.queueUrl(queueURL)
				.maxNumberOfMessages(10)
				.waitTimeSeconds(20)
				.build())
				.messages();
//		} while (messages.isEmpty());


		System.out.println("Received " + messages.size() + " messages: " + messages.stream()
				.map(Message::body)
				.collect(Collectors.toList()));
		return messages;
	}

	public void sendMessageBatch(String queueURL, String... messages)
	{
		if (messages.length == 0)
			return;

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
		sendMessageBatch(queueURL, messages.toArray(String[]::new));
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

		final String queueUrl = sqsClient.createQueue(CreateQueueRequest.builder()
				.queueName(queueName)
				.build())
				.queueUrl();

		System.out.println("Queue \"" + queueName + "\" was created successfully");
		return queueUrl;
	}

	public Optional<String> getQueueUrl(String queueName)
	{
		System.out.println("Getting queue URL for queue " + queueName + "...");

		try
		{
			GetQueueUrlResponse getQueueUrlResponse = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
					.queueName(queueName)
					.build());

			System.out.println("Got URL");
			return Optional.of(getQueueUrlResponse.queueUrl());
		}
		catch (QueueDoesNotExistException queueDoesNotExistException)
		{
			System.out.println("Queue doesn't exist");
			return Optional.empty();
		}
	}

	@Override
	public void close()
	{
		sqsClient.close();
	}
}
