package il.co.dsp211;


import net.sourceforge.tess4j.TesseractException;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.IOException;
import java.util.List;

public class Main
{
	private final static String SPLITERATOR = "🤠";

	public static void main(String[] args)
	{
		try (SQSMethods sqsMethods = new SQSMethods())
		{
			String mangagerToWorkerQueueName = args[0];
			String mangagerToWorkerQueueUrl = sqsMethods.getQueueUrl(mangagerToWorkerQueueName);
			String workerToManagerQueueName = args[1];
			String workerToManagerQueueUrl = sqsMethods.getQueueUrl(workerToManagerQueueName);

			String outputOCR = null;

			while (true)
			{
				System.out.println("Get SQS message...");
				List<Message> message = sqsMethods.receiveMessage(mangagerToWorkerQueueUrl);
				if (!message.isEmpty())
				{
					String msgContent = message.get(0).body();
					String imgURL = msgContent;
					// Assumption: The message contains only the img url
					// Apply OCR on image
					System.out.println("Running Tesseract on the image URL...");
					try
					{
						outputOCR = ProcessOCR.process(imgURL);
					}
					catch (TesseractException | IOException e)
					{
						// Send error message.
					}
					// Create message and send in to the manager using the SQS queue
					sqsMethods.sendSingleMessage(workerToManagerQueueUrl, imgURL + SPLITERATOR + outputOCR);
					// Delete message from the SQS queue because the task is finished
					sqsMethods.deleteMessage(mangagerToWorkerQueueUrl, message.get(0));

				} else
				{
					// No messages in the SQS queue. Go to sleep.
					// Implemented using builtin SQS Queue attribute "long polling"
					//Thread.sleep(1);
				}
			}
		}
	}
}
