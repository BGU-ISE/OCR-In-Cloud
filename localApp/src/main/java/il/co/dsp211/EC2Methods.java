package il.co.dsp211;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class EC2Methods implements AutoCloseable
{
	private final static String instanceJobKey = "JOB";
	private final Properties properties = new Properties();
	private final Ec2Client ec2Client = Ec2Client.builder()
			.region(Region.US_EAST_1)
			.build();

	public EC2Methods() throws IOException
	{
		try (InputStream input = getClass().getClassLoader().getResourceAsStream("secure_info.properties"))
		{
			properties.load(input);
		}
	}

	public Map<String, List<InstanceStateName>> printInstancesState()
	{
		return ec2Client.describeInstances(DescribeInstancesRequest.builder()
//                .instanceIds(response.instances().stream()
//                        .map(Instance::instanceId)
//                        .toArray(String[]::new))
				.build())
				.reservations().stream()
				.map(Reservation::instances)
				.flatMap(Collection::stream)
				.collect(Collectors.groupingBy(Instance::instanceId, Collectors.mapping(instance -> instance.state().name(), Collectors.toList())));
	}

//	"""
//	#!/bin/sh
//	echo hello world > /home/ubuntu/hello_world.txt"""

	public void findOrCreateInstancesByJob(String imageId, int maxCount, Job job, String userData)
	{
		createInstanceByJob(imageId, maxCount - (int) findInstanceByJob(job).count(), job, userData);
	}

	private Stream<Instance> findInstanceByJob(Job job)
	{
		return ec2Client.describeInstances(DescribeInstancesRequest.builder()
				.filters(Filter.builder()
						.name("tag:" + instanceJobKey)
						.values(job.toString())
						.build())
				.build())
				.reservations().stream()
				.map(Reservation::instances)
				.flatMap(Collection::stream)
				.filter(instance -> instance.state().name().equals(InstanceStateName.RUNNING) ||
				                    instance.state().name().equals(InstanceStateName.PENDING));
	}

	private void createInstanceByJob(String imageId, int maxCount, Job job, String userData)
	{
		if (maxCount == 0)
		{
			System.out.println("No need to create instances with job " + job);
			return;
		}

		System.out.println("Creating " + maxCount + " instances with job " + job + "...");

		System.out.println(ec2Client.runInstances(RunInstancesRequest.builder()
				.instanceType(InstanceType.T2_MICRO)
				.imageId(imageId)
				.minCount(1)
				.maxCount(maxCount)
				.iamInstanceProfile(IamInstanceProfileSpecification.builder()
						.arn(properties.getProperty("arn"))
						.build())
				.keyName(properties.getProperty("keyName"))
				.securityGroupIds(properties.getProperty("securityGroupIds"))
				.userData(Base64.getEncoder().encodeToString(userData.getBytes()))
				.tagSpecifications(TagSpecification.builder()
						.resourceType(ResourceType.INSTANCE)
						.tags(Tag.builder()
								.key(instanceJobKey)
								.value(job.toString())
								.build())
						.build())
				.build()).instances().stream()
				                   .map(Instance::instanceId)
				                   .collect(Collectors.toList()) + " created successfully as " + job);
	}

	public void terminateInstancesByJob(Job job)
	{
		System.out.println("Terminating manager if exists...");

		final List<String> instanceIds = findInstanceByJob(job)
				.map(Instance::instanceId)
				.collect(Collectors.toList());

		if (instanceIds.size() > 0)
		{
			System.out.println("Found instances with id " + instanceIds);

			ec2Client.terminateInstances(TerminateInstancesRequest.builder()
					.instanceIds(instanceIds)
					.build());

			System.out.println("Instances terminated");
		}

		System.out.println("Done");
	}

	public Properties getProperties()
	{
		return properties;
	}

	@Override
	public void close()
	{
		ec2Client.close();
	}

	public enum Job
	{
		MANAGER, WORKER
	}
}
