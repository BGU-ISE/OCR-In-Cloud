package il.co.dsp211;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.stream.Collectors;

public class Main
{
	public static void main(String[] args)
	{
		Ec2Client ec2 = Ec2Client.builder()
				.region(Region.US_EAST_1)
				.build();

		RunInstancesResponse response = ec2.runInstances(RunInstancesRequest.builder()
				.instanceType(InstanceType.T2_MICRO)
				.imageId("ami-00acfbfd2e91ae1b0")
				.maxCount(1)
				.minCount(1)
				.userData(Base64.getEncoder().encodeToString("echo hello world > hello_world.txt\n".getBytes()))
				.build());

//		String instanceId = response.instances().get(0).instanceId();
		System.out.println(response.instances().stream()
				                   .map(Instance::instanceId)
				                   .collect(Collectors.toList()) + " created!\nBye Bye");
	}
}
