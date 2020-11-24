package il.co.dsp211;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.Base64;

public class EC2Methods {

    private static final String managerProcessKey = "Process";
    private static final String managerProcessValue = "Manager";

    private static final String managerImageID = "ami-00acfbfd2e91ae1b0";
    private static final String managerScript = """
                        #!/bin/sh
                        echo hello world > /home/ubuntu/hello_world.txt""";

    private final static Region region = Region.US_EAST_1;

    public static void createManagerIfNotOn(Ec2Client ec2Client)
    {
        Instance manager = findManager(ec2Client);
        if (manager == null) {
            createManager(ec2Client);
        }
        else {
            System.out.println("Manager is already running!");
        }
    }

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

    private static Instance findManager(Ec2Client ec2Client)
    {
        // TODO: check if correct cuz my brain is dead.
        for (Reservation res : ec2Client.describeInstances().reservations()) {
            for (Instance instance : res.instances()) {
                for (Tag tag : instance.tags()) {
                    if (tag.key().equals(managerProcessKey) &&
                            tag.value().equals(managerProcessValue) &&
                            (instance.state().name().toString().equals(InstanceStateName.PENDING.toString()) ||
                                    instance.state().name().toString().equals(InstanceStateName.RUNNING.toString()))) {
                        return instance;
                    }
                }
            }
        }
        return null;
    }


}
