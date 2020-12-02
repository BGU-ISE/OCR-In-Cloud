package il.co.dsp211;

import net.sourceforge.tess4j.TesseractException;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.IOException;

public class Main
{
	private final static String SPLITERATOR = "🤠";

	/**
	 * <ul>
	 *     <li>args[0]->manger to worker queue url</li>
	 *     <li>args[1]->worker to manger queue url</li>
	 * </ul>
	 * @param args
	 */
	public static void main(String[] args)
	{
		try (SQSMethods sqsMethods = new SQSMethods())
		{
			String mangerToWorkerQueueUrl = sqsMethods.getQueueUrl(args[0]);
			String workerToManagerQueueUrl = sqsMethods.getQueueUrl(args[1]);

			while (true)
			{
				System.out.println("Get SQS message...");
				Message message = sqsMethods.receiveMessage(mangerToWorkerQueueUrl);
				//new image task🤠<manager to local app queue url>🤠<image url> (manager->worker)
				String[] split = message.body().split(SPLITERATOR);
				// Assumption: The message contains only the img url
				// Apply OCR on image
				System.out.println("Running Tesseract on the image URL...");
				String outputOCR;
				try
				{
					outputOCR = ProcessOCR.process(split[2]);
				}
				catch (TesseractException | IOException e)
				{
					// Send error message.
					e.printStackTrace();
					outputOCR = "ERROR! Could not download file or extract text from it";
				}
				// Create message and send in to the manager using the SQS queue
				//done OCR task🤠<manager to local app queue url>🤠<image url>🤠<text> (worker->manager)
				sqsMethods.sendSingleMessage(workerToManagerQueueUrl,
						"done OCR task" + SPLITERATOR +
						split[1] + SPLITERATOR +
						split[2] + SPLITERATOR +
						outputOCR);
				// Delete message from the SQS queue because the task is finished
				sqsMethods.deleteMessage(mangerToWorkerQueueUrl, message);
			}
		}
	}
}
