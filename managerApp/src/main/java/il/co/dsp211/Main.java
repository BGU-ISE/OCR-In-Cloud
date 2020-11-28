package il.co.dsp211;

import j2html.tags.ContainerTag;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static j2html.TagCreator.*;

/**
 * <ul>
 *     <li>The manager responsible to determine the manager->workers and workers<-manager queues names</li>
 *     <li>The local app responsible to determine the localApp->manager and manager<-localApp queues names</li>
 * </ul>
 */
public class Main
{
	private final static Map<String /*local app <- manager URL*/, Pair<Long /*remaining tasks*/, Deque<ContainerTag>>> map = new ConcurrentHashMap<>();
	private static String bucketName, localAppToManagerQueueName, managerAMI, workerAMI;

	public static void main(String[] args)
	{
		try (/*EC2Methods ec2Methods = new EC2Methods();*/
				S3Methods s3Methods = new S3Methods();
				SQSMethods sqsMethods = new SQSMethods())
		{
			bucketName = args[0];
			localAppToManagerQueueName = args[1];
			managerAMI = args[2];
			workerAMI = args[3];
			String workerToManagerQueueURL = sqsMethods.createQueue("workerToManager" + System.currentTimeMillis());


			final Thread workerResultReceiverThread = new Thread(() ->
			{
				while (true)
					sqsMethods.receiveMessage(workerToManagerQueueURL).stream()
							.map(Message::body)
							.map(body -> body.split(SQSMethods.getSPLITERATOR())/*gives string array with length 4*/)
							.peek(strings ->
							{
								final Pair<Long, Deque<ContainerTag>> value = map.get(strings[1]/*queue url*/);
								value.setKey(value.getKey() - 1);
								value.getValue().addLast(
										p(
												img().withSrc(strings[2]/*image url*/),
												br(),
												text(strings[3]/*text*/)
										)
								);
							})
							.filter(strings -> map.get(strings[1]/*queue url*/).getKey() == 0)
							.forEach(strings ->
							{
								final String
										responseOutputBucketName = "outputBucket" + System.currentTimeMillis(),
										outputHTMLFileName = "outputHTML.html";
								s3Methods.uploadStringToS3Bucket(responseOutputBucketName, outputHTMLFileName,
										html(
												title("OCR"),
												body(map.get(strings[1]/*queue url*/).getValue()
														.toArray(ContainerTag[]::new))
										).renderFormatted()
								);
								sqsMethods.sendSingleMessage(strings[1]/*queue url*/, "done task" + SQSMethods.getSPLITERATOR() + responseOutputBucketName + SQSMethods.getSPLITERATOR() + outputHTMLFileName);
								map.remove(strings[1]/*queue url*/);
							});
			});

// new task🤠<manager to local app queue url>🤠<input bucket name>🤠<location of input file> (local->manager)
// new image task🤠<manager to local app queue url>🤠<image url> (manager->worker)
// done OCR task🤠<manager to local app queue url>🤠<image url>🤠<text> (worker->manager)
// done task🤠<output bucket name>🤠<S3 location of HTML file> (manager->local)

			workerResultReceiverThread.start();
			String localAppToManagerQueueUrl = sqsMethods.getQueueUrl(localAppToManagerQueueName);
		}
	}

	private static class Pair<T1, T2>
	{
		private T1 key;
		private T2 value;

		public Pair(T1 key, T2 value)
		{
			this.key = key;
			this.value = value;
		}

		public T1 getKey()
		{
			return key;
		}

		public void setKey(T1 key)
		{
			this.key = key;
		}

		public T2 getValue()
		{
			return value;
		}

		public void setValue(T2 value)
		{
			this.value = value;
		}

		@Override
		public boolean equals(Object o)
		{
			if (this == o)
				return true;
			if (!(o instanceof Pair))
				return false;
			Pair<?, ?> pair = (Pair<?, ?>) o;
			return key.equals(pair.key) &&
			       value.equals(pair.value);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(key, value);
		}

		@Override
		public String toString()
		{
			return "Pair{" +
			       "key=" + key +
			       ", value=" + value +
			       '}';
		}
	}
}
