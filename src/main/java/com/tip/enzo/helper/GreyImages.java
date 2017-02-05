package com.tip.enzo.helper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created by enzo on 17/1/9.
 * 使图像变灰
 */
class GreyImages {


    private BufferedImage image;
    private int iw, ih;
    private int[] pixels;

    private GreyImages(BufferedImage image) {
        this.image = image;
        iw = image.getWidth();
        ih = image.getHeight();
        pixels = new int[iw * ih];
    }

    private BufferedImage changeGrey() {
        PixelGrabber pg = new PixelGrabber(image.getSource(), 0, 0, iw, ih, pixels, 0, iw);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 设定二值化的域值，默认值为100
        int grey = 100;
        // 对图像进行二值化处理，Alpha值保持不变
        ColorModel cm = ColorModel.getRGBdefault();
        for (int i = 0; i < iw * ih; i++) {
            int red, green, blue;
            int alpha = cm.getAlpha(pixels[i]);
            if (cm.getRed(pixels[i]) > grey) {
                red = 255;
            } else {
                red = 0;
            }
            if (cm.getGreen(pixels[i]) > grey) {
                green = 255;
            } else {
                green = 0;
            }
            if (cm.getBlue(pixels[i]) > grey) {
                blue = 255;
            } else {
                blue = 0;
            }
            pixels[i] = alpha << 24 | red << 16 | green << 8 | blue; //通过移位重新构成某一点像素的RGB值
        }
        // 将数组中的象素产生一个图像
        Image tempImg = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(iw, ih, pixels, 0, iw));
        image = new BufferedImage(tempImg.getWidth(null), tempImg.getHeight(null), BufferedImage.TYPE_INT_BGR);
        image.createGraphics().drawImage(tempImg, 0, 0, null);
        return image;
    }


    private BufferedImage getGrey() {
        ColorConvertOp ccp = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        return image = ccp.filter(image, null);
    }

    //Brighten using a linear formula that increases all color values
    private BufferedImage getBrighten() {
        RescaleOp rop = new RescaleOp(1.25f, 0, null);
        return image = rop.filter(image, null);
    }


    private BufferedImage getProcessedImg() {
        return image;
    }


    static BufferedImage greyImages(String fileLocation, String fileName) throws Exception {
        String absolutePath;
        String rightPath;

        if (fileLocation.endsWith("/")) {
            rightPath = fileLocation;
        } else {
            rightPath = fileLocation + "/";
        }
        absolutePath = rightPath + "origin/" + fileName;

        FileInputStream fin = new FileInputStream(absolutePath);
        BufferedImage bi = ImageIO.read(fin);
        GreyImages flt = new GreyImages(bi);
        flt.changeGrey();
        flt.getGrey();
        flt.getBrighten();
        bi = flt.getProcessedImg();
        File file = new File(rightPath + "shaped/" + fileName);
        ImageIO.write(bi, "JPG", file);
        return bi;

    }


    static BufferedImage greyImages(InputStream inputStream) throws Exception {

        BufferedImage bi = ImageIO.read(inputStream);
        GreyImages flt = new GreyImages(bi);
        flt.changeGrey();
        flt.getGrey();
        flt.getBrighten();
        bi = flt.getProcessedImg();
        return bi;

    }

//
//
//    public static void main(String[] args) throws IOException {
//
//        FileInputStream fin = new FileInputStream("/Users/enzo/Desktop/dustbin/verifyCodes/test.jpg");
//        BufferedImage bi = ImageIO.read(fin);
//        GreyImages flt = new GreyImages(bi);
//        flt.changeGrey();
//        flt.getGrey();
//        flt.getBrighten();
//        bi = flt.getProcessedImg();
//
//        File file = new File("/Users/enzo/Desktop/dustbin/verifyCodes/bbb" + ".jpg");
//        ImageIO.write(bi, "JPG", file);
//    }


}



