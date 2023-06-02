// Copyright Terry Hancock 2023
package host.thanco.NeptuneBase;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

            if (image.getColorModel().hasAlpha()) {
                BufferedImage target = new BufferedImage(image.getWidth(), image.getHeight(),BufferedImage.TYPE_INT_RGB);
                Graphics2D g = target.createGraphics();
                g.fillRect(0, 0, image.getWidth(), image.getHeight());
                g.drawImage(image, 0, 0, null);
                g.dispose();
                image = target;
            }

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

    public static String getImageBytesBase64(ChatItem imageItem) {
        try {
            byte[] imgBytes = Files.readAllBytes(Paths.get(new File((String) imageItem.getContent()).toURI()));
            return ImageHandler.compressImageBytes(imgBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String compressImageBytes(byte[] imageBytes) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
            gzipOutputStream.write(imageBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] compressedBytes = outputStream.toByteArray();
        byte[] encodedBytes = Base64.getEncoder().encode(compressedBytes);
        return new String(encodedBytes, StandardCharsets.UTF_8);
    }

    public static byte[] decompressImageBytes(String imageGZip) {
        byte[] decodedBytes = Base64.getDecoder().decode(imageGZip.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(decodedBytes);

        try (GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = gzipInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return outputStream.toByteArray();
    }
}
