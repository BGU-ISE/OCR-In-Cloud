package il.co.dsp211;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;


public class ProcessOCR
{
	private static final Tesseract tesseract = new Tesseract();

	static
	{
		tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");
//		tesseract.setDatapath("C:\\Users\\amitb\\AppData\\Local\\Programs\\Tesseract-OCR\\tessdata");
	}

	public static String process(String url) throws TesseractException, IOException
	{

		// do OCR on image and save text
		String recognizedText = tesseract.doOCR(ImageIO.read(getRealImageURL(url).openStream()));

		if (recognizedText.isEmpty())
			return "ERROR! failed to recognize text from URL: " + url;

		System.out.println("URL: " + url + " | Result: " + recognizedText);
		return recognizedText;
	}

	private static URL getRealImageURL(String url) throws IOException
	{
		URL urlObj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
		con.setInstanceFollowRedirects(false);
		con.connect();
		final String location = con.getHeaderField("Location");
		con.disconnect();
		return location == null ? urlObj : new URL(location);
	}
}
