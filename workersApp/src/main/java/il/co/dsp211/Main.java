package il.co.dsp211;

import net.sourceforge.tess4j.TesseractException;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.IOException;

public class Main {
    private final static String SPLITERATOR = "🤠";

    public static void main(String[] args) {
        try (SQSMethods sqsMethods = new SQSMethods()) {
            String mangagerToWorkerQueueUrl = sqsMethods.getQueueUrl(args[0]);
            String workerToManagerQueueUrl = sqsMethods.getQueueUrl(args[1]);

            while (true) {
                System.out.println("Get SQS message...");
                Message message = sqsMethods.receiveMessage(mangagerToWorkerQueueUrl);
                //new image task🤠<manager to local app queue url>🤠<image url> (manager->worker)
                String imgURL = message.body().split(SPLITERATOR)[2];
                // Assumption: The message contains only the img url
                // Apply OCR on image
                System.out.println("Running Tesseract on the image URL...");
                String outputOCR;
                try {
                    outputOCR = ProcessOCR.process(imgURL);
                } catch (TesseractException | IOException e) {
                    // Send error message.
                    e.printStackTrace();
                    outputOCR = "ERROR! Could not download file or extract text from it";
                }
                // Create message and send in to the manager using the SQS queue
                //done OCR task🤠<manager to local app queue url>🤠<image url>🤠<text> (worker->manager)
                sqsMethods.sendSingleMessage(workerToManagerQueueUrl, imgURL + SPLITERATOR + outputOCR);
                // Delete message from the SQS queue because the task is finished
                sqsMethods.deleteMessage(mangagerToWorkerQueueUrl, message);
            }
        }
    }
}
