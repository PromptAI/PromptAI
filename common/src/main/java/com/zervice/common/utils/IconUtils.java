package com.zervice.common.utils;

import lombok.extern.log4j.Log4j2;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Log4j2
public class IconUtils {

    private static final int[] _BACKGROUND = new int[]{255, 255, 255, 0};
    private static final MessageDigest _USER_NAME_DIGESTER;

    static {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            digest = null;
        }

        _USER_NAME_DIGESTER = digest;
    }

    /**
     * Generate random pixel images ...
     */
    private static final int _PIXELS = 8;

    public static File generateUserImage(String name, String outputPath, String outputName) {
        int width = 128;
        int height = 128;

       return generateImageBaseName(name, outputPath, outputName, width, height, 99);
    }

    public static File generateProjectImage(String name, String outputPath, String outputName) {
        int width = 320;
        int height = 200;
        // 这里圆角最小为1，如果设置0，图片则会变得很小...
      return   generateImageBaseName(name, outputPath, outputName, width, height, 1);
    }


    public static File generateImageBaseName(String name, String outputPath, String outputName,
                                             int width, int height, int cornerRadius) {
        // Generate a new image buffer of the specified size.
        if (outputPath.startsWith("file:")) {
            outputPath = outputPath.substring(5);
        }

        try {
            File file = Paths.get(outputPath, outputName).toFile();

            final BufferedImage identicon = new BufferedImage(_PIXELS, _PIXELS, BufferedImage.TYPE_INT_ARGB);
            final WritableRaster raster = identicon.getRaster();

            // Generate a hash of the input data using the provided hash generator.
            // Hash is 16 bytes ...
            final byte[] hash = _USER_NAME_DIGESTER.digest(name.getBytes(StandardCharsets.UTF_8));

            // RGB is determined by first three bytes.
            final int[] color = new int[]{hash[0] & 255, hash[1] & 255, hash[2] & 255, 255};

            for (int x = 0; x < _PIXELS; x++) {

                // Offset the position in the hash based on x coordinate.
                final int hashIndex = x < ((_PIXELS + 1) / 2) ? x : (_PIXELS - 1) - x;

                for (int y = 0; y < _PIXELS; y++) {

                    // If the bit at the hashIndex is on/1 the color is used.
                    raster.setPixel(x, y, (hash[hashIndex] >> y & 1) == 1 ? color : _BACKGROUND);
                }
            }

            BufferedImage rounded = makeRoundedCorner(identicon, width, height, cornerRadius);
            ImageIO.write(rounded, "png", file);

            return file;
        } catch (Throwable t) {
            //ignore any exception for it's just used for avatar
            LOG.error("Cannot generate user icon", t);
            return null;
        }
    }


    public static final String FORMAT_PNG = "png";

    public static void resizeAndSave(byte[] data, File output, int scaledWidth, int scaledHeight) throws IOException {
        resizeAndSave(data, FORMAT_PNG, output, scaledWidth, scaledHeight);
    }

    public static void resizeAndSave(byte[] data, String formatName, File output, int scaledWidth, int scaledHeight)
            throws IOException {
        // reads input image
        BufferedImage inputImage = ImageIO.read(new ByteArrayInputStream(data));

        if (scaledWidth <= 0) {
            throw new IllegalArgumentException("Invalid scaled width.");
        }

        if (scaledHeight == 0) {
            int width = inputImage.getWidth();
            int height = inputImage.getHeight();
            scaledHeight = (int) (height * (scaledWidth / (double) width));
        }

        // creates output image
        BufferedImage outputImage = new BufferedImage(scaledWidth,
                scaledHeight, inputImage.getType());

        // scales the input image to the output image
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        // writes to output file
        ImageIO.write(outputImage, formatName, output);
    }

    private static BufferedImage makeRoundedCorner(BufferedImage image, int width, int height, int cornerRadius) {
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius));
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(image, 0, 0, width, height, null);
        g2.dispose();
        return output;
    }
}
