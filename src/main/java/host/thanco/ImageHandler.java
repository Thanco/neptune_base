// Copyright Terry Hancock 2023
package host.thanco;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import javax.imageio.*;
import javax.imageio.stream.*;

public abstract class ImageHandler {
    private static final float COMPRESSION = 0.8f;

    public static void saveImage(String location, byte[] imageBytes) {
        try {
            String tempFile = "img/temp.png";
            FileOutputStream out = new FileOutputStream(tempFile);
            out.write(imageBytes);
            out.close();
            File file = new File(tempFile);
            BufferedImage image = ImageIO.read(file);
            File newFile = new File(location);

            OutputStream os = new FileOutputStream(newFile);

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            ImageWriter writer = (ImageWriter) writers.next();

            ImageOutputStream ios = ImageIO.createImageOutputStream(os);
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();

            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(COMPRESSION);
            writer.write(null, new IIOImage(image, null, null), param);

            os.close();
            ios.close();
            writer.dispose();
            file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] getImageBytes(ChatItem imageItem) {
        try {
            return Files.readAllBytes(Paths.get(new File("img/" + imageItem.getChannel() + imageItem.getItemIndex() + ".jpg").toURI()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }
}
