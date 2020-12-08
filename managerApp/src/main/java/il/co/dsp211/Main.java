package il.co.dsp211;

import software.amazon.awssdk.services.sqs.model.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <ul>
 *     <li>The manager responsible to determine the manager->workers and workers<-manager queues names</li>
 *     <li>The local app responsible to determine the localApp->manager and manager<-localApp queues names</li>
 * </ul>
 */
public class Main
{
	public static void main(String... args)
	{
		final AtomicLong localAppsCounter = new AtomicLong();

		final EC2Methods ec2Methods = new EC2Methods();
		final SQSMethods sqsMethods = new SQSMethods();
		final S3Methods s3Methods = new S3Methods();

		System.out.println("Args: " + Arrays.toString(args)); // TODO: TESTING

		final var box = new Object()
		{
			boolean isTermination;
		};

		final String
				managerToWorkersQueueUrl = sqsMethods.createQueue("managerToWorkersQueue"),
				localAppToManagerQueueUrl = sqsMethods.createQueue("localAppToManagerQueue");

//			new task🤠<manager to local app queue url>🤠<input/output bucket name>🤠<input file name>🤠<n>[🤠terminate] (local->manager)
//			new image task🤠<manager to local app queue url>🤠<image url> (manager->worker)
//			done OCR task🤠<manager to local app queue url>🤠<image url>🤠<text> (worker->manager)
//			done task (manager->local)

		new Thread(() ->
		{
			try (ec2Methods; sqsMethods; s3Methods)
			{
				final String workerToManagerQueueUrl = sqsMethods.createQueue("workerToManagerQueue");
				while (!(box.isTermination && localAppsCounter.get() == 0L))
				{
					final List<Message> messages = sqsMethods.receiveMessage(workerToManagerQueueUrl);
					messages.stream()
							.map(message -> message.body().split(SQSMethods.getSPLITERATOR())/*gives string array with length 4*/)
							.peek(strings ->
							{
								s3Methods.uploadLongToS3Bucket(strings[1]/*queue url*/,
										"numOfUndoneURLs",
										s3Methods.readLongToS3Bucket(strings[1]/*queue url*/, "numOfUndoneURLs") - 1);

								s3Methods.uploadStringToS3Bucket(s3Methods.readObjectToString(strings[1]/*queue url*/, "outputBucket"),
										strings[2]/*image url*/,
										strings[3]/*text*/);
							})
							.filter(strings -> s3Methods.readLongToS3Bucket(strings[1]/*queue url*/, "numOfUndoneURLs") == 0L)
							.forEach(strings ->
							{
								sqsMethods.sendSingleMessage(strings[1]/*queue url*/, "done task");
								s3Methods.deleteBucketBatch(strings[1]/*queue url*/);
								localAppsCounter.decrementAndGet();
							});
					sqsMethods.deleteMessageBatch(workerToManagerQueueUrl, messages);
				}
				ec2Methods.terminateInstancesByJob(EC2Methods.Job.WORKER);
				sqsMethods.deleteQueue(managerToWorkersQueueUrl);
				sqsMethods.deleteQueue(workerToManagerQueueUrl);
				ec2Methods.terminateInstancesByJob(EC2Methods.Job.MANAGER); //גול עצמי
				System.out.println("Cleaning resources...");
			}
			System.out.println("Exiting \"" + Thread.currentThread().getName() + "\" thread and JVM process...");
		}, "WorkerToManager").start();

		while (!box.isTermination)
		{
			final List<Message> messages = sqsMethods.receiveMessage(localAppToManagerQueueUrl);
			messages.stream()
					.map(message -> message.body().split(SQSMethods.getSPLITERATOR())/*gives string array with length 4/5*/)
					.forEach(strings ->
					{
						box.isTermination = box.isTermination || (strings.length == 6 && strings[5].equals("terminate"));

						ec2Methods.findOrCreateInstancesByJob(args[0]/*worker AMI*/, Integer.parseInt(strings[4]/*n*/), EC2Methods.Job.WORKER, """
						                                                                                                                       #!/bin/sh
						                                                                                                                       java -jar /home/ubuntu/workerApp.jar""", args[1], args[2], args[3]);

						try (BufferedReader links = s3Methods.readObjectToBufferedReader(strings[2]/*input/output bucket name*/, strings[3]/*URLs file name*/))
						{
							s3Methods.createBucket(strings[1]/*queue url*/);
							s3Methods.uploadStringToS3Bucket(strings[1]/*queue url*/,
									"outputBucket",
									strings[2]/*input/output bucket name*/);
							s3Methods.uploadLongToS3Bucket(strings[1]/*queue url*/,
									"numOfUndoneURLs",
									links.lines()
											.peek(imageUrl -> sqsMethods.sendSingleMessage(managerToWorkersQueueUrl,
													"new image task" + SQSMethods.getSPLITERATOR() + strings[1]/*queue url*/ + SQSMethods.getSPLITERATOR() + imageUrl))
											.count());
							localAppsCounter.incrementAndGet();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
						s3Methods.deleteObjects(strings[2]/*input/output bucket name*/, strings[3]/*URLs file name*/);
					});
			sqsMethods.deleteMessageBatch(localAppToManagerQueueUrl, messages);
		}
		sqsMethods.deleteQueue(localAppToManagerQueueUrl);
		System.out.println("Exiting \"" + Thread.currentThread().getName() + "\" thread...");
	}
}
