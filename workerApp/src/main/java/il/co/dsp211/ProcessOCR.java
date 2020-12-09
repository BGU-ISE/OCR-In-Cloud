package il.co.dsp211;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;


public class ProcessOCR
{
	private static final Tesseract tesseract = new Tesseract();

	static
	{
//		tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");
		tesseract.setDatapath("C:\\Users\\amitb\\AppData\\Local\\Programs\\Tesseract-OCR\\tessdata");
	}

	public static String process(String url) throws TesseractException, IOException
	{
		// do OCR on image and save text
		String recognizedText = tesseract.doOCR(ImageIO.read(new URL(url).openStream()));

		if (recognizedText.isEmpty())
			return "ERROR! failed to recognize text from URL: " + url;

		System.out.println("URL: " + url + " | Result: " + recognizedText);
		return recognizedText;
	}

}