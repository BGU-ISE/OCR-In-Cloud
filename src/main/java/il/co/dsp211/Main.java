package il.co.dsp211;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;

import java.util.Base64;
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

		System.out.println(response.instances().stream()
				                   .map(Instance::instanceId)
				                   .collect(Collectors.toList()) + " created!\nBye Bye");
	}
}
