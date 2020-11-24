package il.co.dsp211;

import java.io.File;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;


public class ProcessOCR {

    public static Tesseract tesseract;

    public static void init() {
        // Setup Tesseract engine
        Tesseract tesseract = new Tesseract();
        // Path to tessdata folder
        tesseract.setDatapath("");
    }

    public static String process(String url) throws TesseractException {
        String imgFile = null;
        try {
            imgFile = SaveImageFromURL.saveImage(url);
        } catch (Exception e) {
            return "ERROR! failed to download img from inserted URL: " + url;
        }

        // do OCR on image and save text
        String recognized_text = tesseract.doOCR(new File(imgFile));

        if (recognized_text.isEmpty()) {
            return "ERROR! failed to recognize text from URL: " + url;
        }

        System.out.println("URL: " + url + " | Result: " + recognized_text);
        return recognized_text;
    }

}
