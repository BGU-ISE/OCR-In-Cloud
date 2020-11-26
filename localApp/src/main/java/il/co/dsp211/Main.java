package il.co.dsp211;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;

public class Main
{
	public static void main(String[] args) throws IOException
	{
//		String inputImgFile, outputHtmlFile;
//		int workersFilesRatio;
//		boolean terminate;
//
//		if (args.length == 3)
//		{
//			inputImgFile = args[0];
//			outputHtmlFile = args[1];
//			workersFilesRatio = Integer.parseInt(args[2]);
//			terminate = false;
//		} else if (args.length == 4)
//		{
//			inputImgFile = args[0];
//			outputHtmlFile = args[1];
//			workersFilesRatio = Integer.parseInt(args[2]);
//			terminate = true;
//		} else
//		{
//			throw new IllegalArgumentException("""
//			                                   Please provide valid input:
//			                                   java -jar localApp.jar <inputFileName> <outputFileName> <n> [terminate]""");
//		}

		try (/*EC2Methods ec2Methods = new EC2Methods();*/
				S3Methods s3Methods = new S3Methods();
                /*SQSMethods sqsMethods = new SQSMethods()*/)
		{
			s3Methods.createBucket();
			s3Methods.uploadFileToS3Bucket("src/main/resources/text.images.txt");
			s3Methods.downloadFileFromS3Bucket("text.images.txt", "src/main/resources/text.images.output.txt");
			System.out.println(s3Methods.readObjectToString("text.images.txt"));
			s3Methods.deleteBucketBatch();


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
