package il.co.dsp211;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import static j2html.TagCreator.*;

/**
 * <ul>
 *     <li>The manager responsible to determine the manager->workers and workers<-manager queues names</li>
 *     <li>The local app responsible to determine the localApp->manager and manager<-localApp queues names</li>
 * </ul>
 */
public class Main
{
	static
	{
		j2html.Config.indenter = (level, text) -> String.join("", Collections.nCopies(level, "\t")) + text;
	}

	public static void main(String[] args)
	{
		final Map<String /*local app <- manager URL*/, Quadruple<String /*input/output bucket name*/, String/*output file name*/, Long /*remaining tasks*/, Queue<ContainerTag>>> map = new ConcurrentHashMap<>();

		final EC2Methods ec2Methods = new EC2Methods();
		final SQSMethods sqsMethods = new SQSMethods();
		final S3Methods s3Methods = new S3Methods();

		final var box = new Object()
		{
			boolean isTermination;
		};

		final String
				managerToWorkersQueueUrl = sqsMethods.createQueue("managerToWorkersQueue"),
				localAppToManagerQueueUrl = sqsMethods.createQueue("localAppToManagerQueue");

//			new task🤠<manager to local app queue url>🤠<input/output bucket name>🤠<input file name>🤠<output file name>🤠<n>[🤠terminate] (local->manager)
//			new image task🤠<manager to local app queue url>🤠<image url> (manager->worker)
//			done OCR task🤠<manager to local app queue url>🤠<image url>🤠<text> (worker->manager)
//			done task (manager->local)

		new Thread(() ->
		{
			try (ec2Methods; sqsMethods; s3Methods)
			{
				final String workerToManagerQueueUrl = sqsMethods.createQueue("workerToManagerQueue");
				while (!(box.isTermination && map.isEmpty()))
				{
					final List<Message> messages = sqsMethods.receiveMessage(workerToManagerQueueUrl);
					messages.stream()
							.map(message -> message.body().split(SQSMethods.getSPLITERATOR())/*gives string array with length 4*/)
							.peek(strings ->
							{
								final Quadruple<String, String, Long, Queue<ContainerTag>> value = map.get(strings[1]/*queue url*/);
								--value.t3;
								value.getT4().add(
										p(
												Stream.of(Stream.of(
														img().withSrc(strings[2]/*image url*/),
														br()),
														strings[3]/*text*/.lines()
																.flatMap(line -> Stream.of(text(line),
																		br())))
														.flatMap(Function.identity())
														.toArray(DomContent[]::new)
										)
								);
							})
							.filter(strings -> map.get(strings[1]/*queue url*/).getT3() == 0L)
							.forEach(strings ->
							{
								final Quadruple<String, String, Long, Queue<ContainerTag>> data = map.get(strings[1]/*queue url*/);
								s3Methods.uploadStringToS3Bucket(data.getT1()/*bucket name*/, data.getT2(),
										html(
												head(title("OCR")),
												body(data.getT4()/*p(...)[]*/
														.toArray(ContainerTag[]::new))
										).renderFormatted()
								);
								sqsMethods.sendSingleMessage(strings[1]/*queue url*/, "done task");
								map.remove(strings[1]/*queue url*/);
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
		}, "WorkerToManagerThread").start();

		while (!box.isTermination)
		{
			final List<Message> messages = sqsMethods.receiveMessage(localAppToManagerQueueUrl);
			messages.stream()
					.map(message -> message.body().split(SQSMethods.getSPLITERATOR())/*gives string array with length 5/6*/)
					.forEach(strings ->
					{
						box.isTermination = box.isTermination || (strings.length == 7 && strings[6].equals("terminate"));

//						ec2Methods.findOrCreateInstancesByJob(args[0]/*worker AMI*/, Integer.parseInt(strings[5]/*n*/), EC2Methods.Job.WORKER, """
//						                                                                                                                       #!/bin/sh
//						                                                                                                                       java -jar /home/ubuntu/workerApp.jar""");

						try (BufferedReader links = s3Methods.readObjectToString(strings[2]/*input/output bucket name*/, strings[3]/*URLs file name*/))
						{
							map.put(strings[1]/*queue url*/,
									new Quadruple<>(strings[2]/*input/output bucket name*/,
											strings[4]/*output file name*/,
											links.lines()
													.peek(imageUrl -> /*queue url*/ sqsMethods.sendSingleMessage(managerToWorkersQueueUrl,
															new StringBuilder("new image task").append(SQSMethods.getSPLITERATOR())
																	.append(strings[1]).append(SQSMethods.getSPLITERATOR())
																	.append(imageUrl)
																	.toString()))
													.count(),
											new LinkedList<>()));
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					});
			sqsMethods.deleteMessageBatch(localAppToManagerQueueUrl, messages);
		}
		sqsMethods.deleteQueue(localAppToManagerQueueUrl);
		System.out.println("Exiting \"" + Thread.currentThread().getName() + "\" thread...");
	}

	private static class Quadruple<T1, T2, T3, T4>
	{
		private T1 t1;
		private T2 t2;
		private T3 t3;
		private T4 t4;

		public Quadruple(T1 t1, T2 t2, T3 t3, T4 t4)
		{
			this.t1 = t1;
			this.t2 = t2;
			this.t3 = t3;
			this.t4 = t4;
		}

		public T1 getT1()
		{
			return t1;
		}

		public void setT1(T1 t1)
		{
			this.t1 = t1;
		}

		public T2 getT2()
		{
			return t2;
		}

		public void setT2(T2 t2)
		{
			this.t2 = t2;
		}

		public T3 getT3()
		{
			return t3;
		}

		public void setT3(T3 t3)
		{
			this.t3 = t3;
		}

		public T4 getT4()
		{
			return t4;
		}

		public void setT4(T4 t4)
		{
			this.t4 = t4;
		}

		@Override
		public boolean equals(Object o)
		{
			if (this == o)
				return true;
			if (!(o instanceof Quadruple))
				return false;
			Quadruple<?, ?, ?, ?> quadruple = (Quadruple<?, ?, ?, ?>) o;
			return t1.equals(quadruple.t1) &&
			       t2.equals(quadruple.t2) &&
			       t3.equals(quadruple.t3) &&
			       t4.equals(quadruple.t4);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(t1, t2, t3, t4);
		}

		@Override
		public String toString()
		{
			return "Quadruple{" +
			       "t1=" + t1 +
			       ", t2=" + t2 +
			       ", t3=" + t3 +
			       ", t4=" + t4 +
			       '}';
		}
	}
}
