package il.co.dsp211;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class Main
{
	private static final String queueName = "queue" + System.currentTimeMillis();


	public static void main(String[] args)
	{
		String inputImgFile, outputHtmlFile;
		int workersFilesRatio;
		boolean terminate;

		if (args.length == 3)
		{
			inputImgFile = args[0];
			outputHtmlFile = args[1];
			workersFilesRatio = Integer.parseInt(args[2]);
			terminate = false;
		} else if (args.length == 4)
		{
			inputImgFile = args[0];
			outputHtmlFile = args[1];
			workersFilesRatio = Integer.parseInt(args[2]);
			terminate = true;
		} else
		{
			throw new IllegalArgumentException("""
			                                   Please provide valid input:
			                                   java -jar localApp.jar <inputFileName> <outputFileName> <n> [terminate]""");
		}

		try (/*Ec2Client ec2Client = Ec2Client.builder()
                .region(Region.US_EAST_1)
                .build();*/
				S3Client s3Client = S3Client.builder()
						.region(Region.US_EAST_1)
						.build()
                /*SqsClient sqsClient = SqsClient.builder()
                        .region(Region.US_EAST_1)
                        .build()*/)
		{

			String bucketName = "bucky" + System.currentTimeMillis();
			S3Methods.createBucket(s3Client, bucketName);
			S3Methods.uploadFileToS3Bucket(s3Client, bucketName, "localApp/src/main/resources/text.images.txt");


//            RunInstancesResponse response = createInstance(ec2Client);
//            System.out.println(printInstancesState(ec2Client, response));


//            String queueUrl = createQueue(sqsClient, queueName);
//            sqsClient.sendMessage(SendMessageRequest.builder()
//                    .queueUrl(queueUrl)
//                    .messageBody("Hello World1")
//                    .build());
//            sqsClient.sendMessage(SendMessageRequest.builder()
//                    .queueUrl(queueUrl)
//                    .messageBody("Hello World2")
//                    .build());
//            sqsClient.sendMessage(SendMessageRequest.builder()
//                    .queueUrl(queueUrl)
//                    .messageBody("Hello World3")
//                    .build());
//
//            sqsClient.sendMessageBatch(SendMessageBatchRequest.builder()
//                    .queueUrl(queueUrl)
//                    .entries(SendMessageBatchRequestEntry.builder()
//                                    .messageBody("Hello World4")
//                                    .build(),
//                            SendMessageBatchRequestEntry.builder()
//                                    .messageBody("Hello World5")
//                                    .build(),
//                            SendMessageBatchRequestEntry.builder()
//                                    .messageBody("Hello World6")
//                                    .build())
//                    .build());
//
//            List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
//                    .queueUrl(queueUrl)
//                    .build())
//                    .messages();
//            System.out.println(messages.stream()
//                    .map(Message::body)
//                    .collect(Collectors.toList()));
//
//            messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
//                    .queueUrl(queueUrl)
//                    .build())
//                    .messages();
//            System.out.println(messages.stream()
//                    .map(Message::body)
//                    .collect(Collectors.toList()));
//
//            sqsClient.deleteMessage(DeleteMessageRequest.builder()
//                    .queueUrl(queueUrl)
//                    .receiptHandle(messages.get(0).receiptHandle())
//                    .build());
//
//            GetQueueAttributesResponse response = sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
//                    .queueUrl(queueUrl)
//                    .attributeNames(QueueAttributeName.ALL)
//                    .build());
//            System.out.println(response.attributes());


//            ec2Client.waiter().waitUntilInstanceRunning(DescribeInstancesRequest.builder()
//                    .instanceIds(response.instances().stream()
//                            .map(Instance::instanceId)
//                            .toArray(String[]::new))
//                    .build())
//                    .matched()
//                    .exception()
//                    .ifPresent(System.out::println);
//
//            System.out.println(printInstancesState(ec2Client, response) + "\nBye Bye");


//		String bucket = "bucket" + System.currentTimeMillis();
//		createBucket(bucket, Region.US_EAST_1);
			System.out.println("Closing resources...");
		}
		System.out.println("Bye bye");
	}
}
