package il.co.dsp211;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    private static final String queueName = "queue" + System.currentTimeMillis();

    public static void main(String[] args) {
        try (/*Ec2Client ec2Client = Ec2Client.builder()
                .region(Region.US_EAST_1)
                .build();
             S3Client s3Client = S3Client.builder()
                     .region(Region.US_EAST_1)
                     .build();*/
                SqsClient sqsClient = SqsClient.builder()
                        .region(Region.US_EAST_1)
                        .build()) {
//            RunInstancesResponse response = createInstance(ec2Client);
//            System.out.println(printInstancesState(ec2Client, response));

            String queueUrl = createQueue(sqsClient, queueName);
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody("Hello World1")
                    .build());
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody("Hello World2")
                    .build());
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody("Hello World3")
                    .build());

            sqsClient.sendMessageBatch(SendMessageBatchRequest.builder()
                    .queueUrl(queueUrl)
                    .entries(SendMessageBatchRequestEntry.builder()
                                    .messageBody("Hello World4")
                                    .build(),
                            SendMessageBatchRequestEntry.builder()
                                    .messageBody("Hello World5")
                                    .build(),
                            SendMessageBatchRequestEntry.builder()
                                    .messageBody("Hello World6")
                                    .build())
                    .build());

            List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .build())
                    .messages();
            System.out.println(messages.stream()
                    .map(Message::body)
                    .collect(Collectors.toList()));

            messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .build())
                    .messages();
            System.out.println(messages.stream()
                    .map(Message::body)
                    .collect(Collectors.toList()));

            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(messages.get(0).receiptHandle())
                    .build());

            GetQueueAttributesResponse response = sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(QueueAttributeName.ALL)
                    .build());
            System.out.println(response.attributes());


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

    private static RunInstancesResponse createInstance(Ec2Client ec2Client) {
        return ec2Client.runInstances(RunInstancesRequest.builder()
                .instanceType(InstanceType.T2_MICRO)
                .imageId("ami-00acfbfd2e91ae1b0") // Ubuntu Server 20.04 LTS (HVM), SSD Volume Type
//				.imageId("ami-076515f20540e6e0b") // requested by assignment 1 but not working
//				.imageId("ami-04bf6dcdc9ab498ca") // Amazon Linux 2 AMI (HVM), SSD Volume Type
                .maxCount(1)
                .minCount(1)
                .keyName("RoysKey")
                .securityGroupIds("sg-0210d89a3003c1298")
                .userData(Base64.getEncoder().encodeToString("""
                        #!/bin/sh
                        echo hello world > /home/ubuntu/hello_world.txt""".getBytes()))
                .build());
    }

    private static Map<String, List<InstanceStateName>> printInstancesState(Ec2Client ec2, RunInstancesResponse response) {
        return ec2.describeInstances(DescribeInstancesRequest.builder()
                .instanceIds(response.instances().stream()
                        .map(Instance::instanceId)
                        .toArray(String[]::new))
                .build())
                .reservations().stream()
                .flatMap(reservation -> reservation.instances().stream())
                .collect(Collectors.groupingBy(Instance::instanceId, Collectors.mapping(instance -> instance.state().name(), Collectors.toList())));
    }

    public static String createQueue(SqsClient sqsClient, String queueName) {
        System.out.println("Create queue...");

        sqsClient.createQueue(CreateQueueRequest.builder()
                .queueName(queueName)
                .build());

        GetQueueUrlResponse getQueueUrlResponse = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build());

        System.out.println("queue created!!");

        return getQueueUrlResponse.queueUrl();
    }
}
