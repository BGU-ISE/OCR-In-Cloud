package il.co.dsp211;

//TODO: change the manager by the following changes:
// SQSMethods: "createQueue" , "getQueueUrl"

public class Main
{
	/**
	 * args[0]->input image file path
	 * args[1]->output file path
	 * args[2]->number of workers needed
	 * args[3]->(optional) must be equals to "terminate"
	 *
	 * @param args
	 */
	public static void main(String[] args)
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

		try (EC2Methods ec2Methods = new EC2Methods();
		     S3Methods s3Methods = new S3Methods();
		     SQSMethods sqsMethods = new SQSMethods())
		{
			final String
					localAppToManagerQueueUrl = sqsMethods.createQueue("localAppToManagerQueue"),
					managerToLocalAppQueueUrl = sqsMethods.createQueue("managerToLocalAppQueue" + System.currentTimeMillis());
			ec2Methods.findOrCreateInstancesByJob(""/*TODO*/, 1, EC2Methods.Job.MANAGER, ""/*TODO*/);
			s3Methods.createBucket();
//			new task🤠<manager to local app queue url>🤠<input/output bucket name>🤠< URLs file name>🤠<n>[🤠terminate]
			sqsMethods.sendSingleMessage(localAppToManagerQueueUrl,
					"new task" + SQSMethods.getSPLITERATOR() +
					managerToLocalAppQueueUrl + SQSMethods.getSPLITERATOR() +
					s3Methods.getBucketName() + SQSMethods.getSPLITERATOR() +
					s3Methods.uploadFileToS3Bucket(args[0]) + SQSMethods.getSPLITERATOR() +
					args[2] + SQSMethods.getSPLITERATOR() +
					(args.length == 4 && args[3].equals("terminate") ? args[3] : ""));

//			done task🤠<output file name>
			s3Methods.downloadFileFromS3Bucket(sqsMethods.receiveMessage(managerToLocalAppQueueUrl)
					.split(SQSMethods.getSPLITERATOR())[1]);

			s3Methods.deleteBucketBatch();
			sqsMethods.deleteQueue(managerToLocalAppQueueUrl);
			System.out.println("Closing resources...");
		}
		System.out.println("Bye bye");
	}
}
