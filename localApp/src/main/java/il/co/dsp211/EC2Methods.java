package il.co.dsp211;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.utils.SdkAutoCloseable;

import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EC2Methods implements SdkAutoCloseable
{
	private final Ec2Client ec2Client = Ec2Client.builder()
			.region(Region.US_EAST_1)
			.build();
	private final String
			managerProcessKey = "Process",
			managerProcessValue = "Manager";

//	"""
//					                       #!/bin/sh
//					                       echo hello world > /home/ubuntu/hello_world.txt"""

	public Map<String, List<InstanceStateName>> printInstancesState()
	{
		return ec2Client.describeInstances(DescribeInstancesRequest.builder()
//                .instanceIds(response.instances().stream()
//                        .map(Instance::instanceId)
//                        .toArray(String[]::new))
				.build())
				.reservations().stream()
				.flatMap(reservation -> reservation.instances().stream())
				.collect(Collectors.groupingBy(Instance::instanceId, Collectors.mapping(instance -> instance.state().name(), Collectors.toList())));
	}

	public void findOrCreateManager(String userData)
	{
		ec2Client.describeInstances(DescribeInstancesRequest.builder()
				.filters(Filter.builder()
						.name("tag:" + managerProcessKey)
						.values(managerProcessValue)
						.build())
				.build())
				.reservations().stream()
				.map(Reservation::instances)
				.flatMap(Collection::stream)
				.filter(instance -> instance.state().name().equals(InstanceStateName.RUNNING) ||
				                    instance.state().name().equals(InstanceStateName.PENDING))
				.findAny()
				.ifPresentOrElse(instance -> System.out.println("Manager already exists with id " + instance.instanceId() + " with state " + instance.state().name()),
						() -> new EC2Methods().createManager(userData));
	}

	private void createManager(String userData)
	{
		System.out.println("Creating manager...");

		ec2Client.runInstances(RunInstancesRequest.builder()
				.instanceType(InstanceType.T2_MICRO)
				.imageId("ami-00acfbfd2e91ae1b0") // Ubuntu Server 20.04 LTS (HVM), SSD Volume Type
				.maxCount(1)
				.minCount(1)
				.keyName("RoysKey")
				.securityGroupIds("sg-0210d89a3003c1298")
				.userData(Base64.getEncoder().encodeToString(userData.getBytes()))
				.tagSpecifications(TagSpecification.builder()
						.resourceType(ResourceType.INSTANCE)
						.tags(Tag.builder()
								.key(managerProcessKey)
								.value(managerProcessValue)
								.build())
						.build())
				.build());

		System.out.println("Manager created successfully");
	}

	@Override
	public void close()
	{
		ec2Client.close();
	}
}
