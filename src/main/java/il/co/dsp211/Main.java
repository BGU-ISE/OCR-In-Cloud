package il.co.dsp211;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main
{
	private final static Ec2Client ec2Client = Ec2Client.builder()
			.region(Region.US_EAST_1)
			.build();

	private final static S3Client s3Client = S3Client.builder()
			.region(Region.US_EAST_1)
			.build();

	private final static SqsClient sqsClient = SqsClient.builder()
		.region(Region.US_EAST_1)
		.build();

	String queueName = "queue" + System.currentTimeMillis();
	String queueUrl = createQueue(sqsClient, queueName);


	public static void main(String[] args)
	{
		RunInstancesResponse response = createInstance();

		System.out.println(printInstancesState(ec2Client, response));

		ec2Client.waiter().waitUntilInstanceRunning(DescribeInstancesRequest.builder()
				.instanceIds(response.instances().stream()
						.map(Instance::instanceId)
						.toArray(String[]::new))
				.build())
				.matched()
				.exception()
				.ifPresent(System.out::println);

		System.out.println(printInstancesState(ec2Client, response) + "\nBye Bye");


//		String bucket = "bucket" + System.currentTimeMillis();
//		createBucket(bucket, Region.US_EAST_1);
	}

	private static RunInstancesResponse createInstance()
	{
		RunInstancesResponse response = ec2Client.runInstances(RunInstancesRequest.builder()
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
		return response;
	}

	private static Map<String, List<InstanceStateName>> printInstancesState(Ec2Client ec2, RunInstancesResponse response)
	{
		return ec2.describeInstances(DescribeInstancesRequest.builder()
				.instanceIds(response.instances().stream()
						.map(Instance::instanceId)
						.toArray(String[]::new))
				.build())
				.reservations().stream()
				.flatMap(reservation -> reservation.instances().stream())
				.collect(Collectors.groupingBy(Instance::instanceId, Collectors.mapping(instance -> instance.state().name(), Collectors.toList())));
	}

	private static void createBucket(String bucketName, Region region)
	{
		s3Client.createBucket(CreateBucketRequest.builder()
				.bucket(bucketName)
				.createBucketConfiguration(CreateBucketConfiguration.builder()
						.locationConstraint(region.id())
						.build())
				.build());

		s3Client.waiter().waitUntilBucketExists(HeadBucketRequest.builder()
				.bucket(bucketName)
				.build())
				.matched()
				.exception()
				.ifPresent(System.out::println);
		System.out.println(bucketName + " is ready");

		System.out.println(bucketName);
	}

	public static String createQueue(SqsClient sqsClient, String queueName)
	{
		System.out.println("Create queue...");
		CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
				.queueName(queueName)
				.build();

		sqsClient.createQueue(createQueueRequest);

		GetQueueUrlResponse getQueueUrlResponse = sqsClient.getQueueUrl(GetQueueUrlRequest.builder().
				queueName(queueName).
				build());

		String queueUrl = getQueueUrlResponse.queueUrl();
		return queueUrl;
	}
}
