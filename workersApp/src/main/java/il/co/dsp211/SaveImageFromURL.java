package il.co.dsp211;

import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;


public class SaveImageFromURL {

    static int uniqueFileID = 0;

    public static String saveImage(String imgURL) throws Exception {
        URL url = new URL(imgURL);
        String file = Paths.get(url.getPath()).getFileName().getFileName().toString();
        String fileName = FilenameUtils.getBaseName(file);
        String imgType = FilenameUtils.getExtension(file);
        String filePath = "workersApp/src/main/java/il/co/dsp211/"; // TODO: EDIT Var value or remove

        String uniqueFileDestination = filePath + fileName + "_" + uniqueFileID + "." + imgType;

        // Download and save image from URL
        InputStream in = new BufferedInputStream(url.openStream());
        OutputStream out = new BufferedOutputStream(new FileOutputStream(uniqueFileDestination));

        int i;
        while ( (i = in.read()) != -1) {
            out.write(i);
        }
        in.close();
        out.close();

        // Advance uniq ID
        uniqueFileID++;

        return uniqueFileDestination;
    }

}
