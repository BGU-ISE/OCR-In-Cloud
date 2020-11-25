package il.co.dsp211;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EC2Methods
{

	private static final String managerProcessKey = "Process";
	private static final String managerProcessValue = "Manager";

	private static final String managerImageID = "ami-00acfbfd2e91ae1b0";
	private static final String managerScript = """
	                                            #!/bin/sh
	                                            echo hello world > /home/ubuntu/hello_world.txt""";

	private final static Region region = Region.US_EAST_1;

	private static void createManager(Ec2Client ec2Client)
	{
		System.out.println("Creating manager...");

		ec2Client.runInstances(RunInstancesRequest.builder()
				.instanceType(InstanceType.T2_MICRO)
				.imageId(managerImageID) // Ubuntu Server 20.04 LTS (HVM), SSD Volume Type
				.maxCount(1)
				.minCount(1)
				.keyName("RoysKey")
				.securityGroupIds("sg-0210d89a3003c1298")
				.userData(Base64.getEncoder().encodeToString(managerScript.getBytes()))
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

	public static void findOrCreateManager(Ec2Client ec2Client)
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
						() -> createManager(ec2Client));
	}

	public static Map<String, List<InstanceStateName>> printInstancesState(Ec2Client ec2/*, RunInstancesResponse response*/)
	{
		return ec2.describeInstances(DescribeInstancesRequest.builder()
//                .instanceIds(response.instances().stream()
//                        .map(Instance::instanceId)
//                        .toArray(String[]::new))
				.build())
				.reservations().stream()
				.flatMap(reservation -> reservation.instances().stream())
				.collect(Collectors.groupingBy(Instance::instanceId, Collectors.mapping(instance -> instance.state().name(), Collectors.toList())));
	}
}
