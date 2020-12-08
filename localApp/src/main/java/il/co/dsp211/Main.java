package il.co.dsp211;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Main
{
	/**
	 * <ul>
	 *     <li>args[0]->input image file path</li>
	 *     <li>args[1]->output file path</li>
	 *     <li>args[2]->number of workers needed</li>
	 *     <li>args[3]->(optional) must be equals to "terminate"</li>
	 * </ul>
	 *
	 * @param args
	 */
	public static void main(String... args) throws IOException
	{
		if (args.length != 3 && args.length != 4)
			throw new IllegalArgumentException("""
			                                   Please provide valid input:
			                                   java -jar localApp.jar <inputFileName> <outputFileName> <n> [terminate]""");

		try (EC2Methods ec2Methods = new EC2Methods();
		     S3Methods s3Methods = new S3Methods();
		     SQSMethods sqsMethods = new SQSMethods())
		{
			final String
					managerToLocalAppQueueName = "manager-to-local-app-queue" + System.currentTimeMillis(),
					localAppToManagerQueueUrl = sqsMethods.createQueue("localAppToManagerQueue"),
					managerToLocalAppQueueUrl = sqsMethods.createQueue(managerToLocalAppQueueName);
			ec2Methods.findOrCreateInstancesByJob("ami-0528f602e9e84129f"/*TODO:<manager AMI>*/, 1, EC2Methods.Job.MANAGER, """
			                                                                                                                #!/bin/sh
			                                                                                                                java -jar /home/ubuntu/managerApp.jar ami-0528f602e9e84129f"""/*TODO: <workers AMI>*/ + " " + ec2Methods.getProperties().getProperty("arn") + " " + ec2Methods.getProperties().getProperty("keyName") + " " + ec2Methods.getProperties().getProperty("securityGroupIds"));
			s3Methods.createBucket();
//			new task🤠<manager to local app queue url>🤠<input/output bucket name>🤠<input file name>🤠<n>[🤠terminate]
			final StringBuilder stringBuilder = new StringBuilder("new task").append(SQSMethods.getSPLITERATOR())
					.append(managerToLocalAppQueueName).append(SQSMethods.getSPLITERATOR())
					.append(s3Methods.getBucketName()).append(SQSMethods.getSPLITERATOR())
					.append(s3Methods.uploadFileToS3Bucket(args[0])).append(SQSMethods.getSPLITERATOR())
					.append(args[2]); // 4
			if (args.length == 4 && args[3].equals("terminate"))
				stringBuilder.append(SQSMethods.getSPLITERATOR())
						.append(args[3]); // 5
			sqsMethods.sendSingleMessage(localAppToManagerQueueUrl, stringBuilder.toString());

//			done task
			sqsMethods.receiveMessage(managerToLocalAppQueueUrl);

			final Path outputFilePath = Path.of(args[1]);
			Files.writeString(outputFilePath, """
			                                  <html>
			                                  	<head>
			                                  		<title>
			                                  			OCR
			                                  		</title>
			                                  	</head>
			                                  	<body>
			                                  	""", StandardOpenOption.CREATE);
			Files.write(outputFilePath, s3Methods.getAllObjectsWith()/*p(...)[]*/, StandardOpenOption.APPEND);
			Files.writeString(outputFilePath, """
			                                  	</body>
			                                  </html>""", StandardOpenOption.APPEND);

			s3Methods.deleteBucketBatch();
			sqsMethods.deleteQueue(managerToLocalAppQueueUrl);
			System.out.println("Closing resources...");
		}
		System.out.println("Bye bye");
	}
}
