package com.tip.enzo.helper;


import org.springframework.util.ResourceUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by enzo on 17/1/10.
 * 图片处理
 */
public class RecognizeImageHelper {


    private static Map<BufferedImage, String> trainMap = null;


    static {
        try {
            loadTrainData();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static boolean isWhite(int colorInt) {
        Color color = new Color(colorInt);
        if (color.getRed() + color.getGreen() + color.getBlue() > 100) {
            return true;
        }
        return false;
    }


    private static BufferedImage removeBackground(InputStream inputStream) throws Exception {
        return GreyImages.greyImages(inputStream);
    }


    private static List<BufferedImage> splitImage(BufferedImage img) throws Exception {
        List<BufferedImage> subImages = new ArrayList<>();
        int width = img.getWidth();
        int height = img.getHeight();
        List<Integer> split_X_List = new ArrayList<>();

        /*
         * 切割X轴
         */
        boolean isFindBlackPoint = true;
        for (int x = 0; x < width; ++x) {

            //找Y轴上存在黑点的 x坐标
            if (isFindBlackPoint) {
                for (int y = 0; y < height; ++y) {
                    if (!isWhite(img.getRGB(x, y))) {
                        split_X_List.add(x);
                        isFindBlackPoint = false;
                        break;
                    }
                }
            }
            //找Y轴上 全部都是 白点的 x坐标
            else {
                boolean is_Y_haveBlack = false;
                for (int y = 0; y < height; ++y) {
                    //是否  在找出Y轴上第一个黑色的点
                    if (!isWhite(img.getRGB(x, y))) {
                        is_Y_haveBlack = true;
                        break;
                    }
                }
                if (!is_Y_haveBlack) {
                    split_X_List.add(x);
                    isFindBlackPoint = true;
                }
            }
        }

        //如果是奇数
        if (split_X_List.size() % 2 == 1 && split_X_List.get(split_X_List.size() - 1) < width) {
            split_X_List.add(width);
        }
        if (split_X_List.size() != 8) {
            throw new Exception("切割X轴错误，list长度不是8");
        }


        /*
         * 切割Y轴
         */
        int letterOrder = 1;//开始切割第几个字母

        for (; letterOrder <= 4; letterOrder++) {

            Integer y_upper = null;
            Integer y_higher = null;
            Label1:
            for (int y = 0; y < height; ++y) {
                int x_start_index = (letterOrder - 1) * 2;
                int x_end_index = letterOrder * 2 - 1;
                //找到第一个点
                for (int x = split_X_List.get(x_start_index); x < split_X_List.get(x_end_index); ++x) {
                    if (!isWhite(img.getRGB(x, y))) {
                        y_upper = y;
                        break Label1;
                    }
                }
            }

            Label2:
            for (int y = height - 1; y > 0; y--) {
                int x_start_index = (letterOrder - 1) * 2;
                int x_end_index = letterOrder * 2 - 1;
                //找到第一个点
                for (int x = split_X_List.get(x_start_index); x < split_X_List.get(x_end_index); ++x) {
                    if (!isWhite(img.getRGB(x, y))) {
                        y_higher = y;
                        break Label2;
                    }
                }
            }
            if (y_higher == null || y_upper == null) {
                throw new Exception("切割Y轴出错");
            }

            //split img
            int x_start = split_X_List.get((letterOrder - 1) * 2);
            int x_end = split_X_List.get(letterOrder * 2 - 1);
            subImages.add(img.getSubimage(x_start, y_upper, (x_end - x_start), (y_higher - y_upper + 1)));
//            ImageIO.write(img.getSubimage(x_start, y_upper, (x_end-x_start), (y_higher-y_upper+1)),
//                    "JPG", new File("/Users/enzo/Desktop/dustbin/verifyCodes/temp/第"+ letterOrder +"张图.jpg"));

        }

        return subImages;
    }


    private static Map<BufferedImage, String> loadTrainData() throws Exception {
        if (trainMap == null) {
            Map<BufferedImage, String> map = new HashMap<>();
            File dir = ResourceUtils.getFile("classpath:base/imgs/letters");
            File[] files = dir.listFiles();
            if (files == null) {
                throw new Exception("letter文件路径不存在");
            }
            for (File file : files) {
                if (!file.getName().endsWith("jpg")) {
                    continue;
                }
                String letter;
                String[] nameArray = file.getName().split("\\.");
                if (nameArray[0].length() > 1) {
                    letter = String.valueOf(file.getName().charAt(1));
                } else {
                    letter = String.valueOf(file.getName().charAt(0));
                }

                map.put(ImageIO.read(file), letter);
            }
            trainMap = map;
        }
        return trainMap;
    }

    private static String getSingleCharOcr(BufferedImage img, Map<BufferedImage, String> map) {
        String result = "";
        int width = img.getWidth();
        int height = img.getHeight();
        int min = width * height;
        for (BufferedImage bi : map.keySet()) {
            if (bi.getWidth() != width || bi.getHeight() != height) {
                continue;
            }

            int count = 0;
            Label1:
            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    if (isWhite(img.getRGB(x, y)) != isWhite(bi.getRGB(x, y))) {
                        count++;
                        if (count >= min)
                            break Label1;
                    }
                }
            }
            if (count < min) {
                min = count;
                result = map.get(bi);
            }
        }
        return result;
    }


    public static String recognize(InputStream inputStream) {
        String result = "";
        try {
            BufferedImage img = removeBackground(inputStream);
            List<BufferedImage> listImg = splitImage(img);
            for (BufferedImage bi : listImg) {
                String letter = getSingleCharOcr(bi, trainMap);
                if (letter.length() > 1) {
                    letter = letter.substring(1);
                }
                result += letter;
            }
        } catch (Exception ignored) {
//            ignored.printStackTrace();
        }

        return result;
    }


    public static void main(String[] args) throws Exception {

        System.out.println();
////        File dir = new File(ImageFileLocationDefine.ORIGIN_IMAGES_LOCATION);
////        File[] files = dir.listFiles();
////        for (File file : files){
////            if (file.getName().contains(".DS_Store")){
////                continue;
////            }
////
//////            removeBackground(ImageFileLocationDefine.ORIGIN_IMAGES_LOCATION, file.getName());
////            String text = recognize(ImageFileLocationDefine.ORIGIN_IMAGES_LOCATION, file.getName());
////            if (StringUtils.isBlank(text) || text.length()!=4){
////                continue;
////            }
////
//////            String text = recognize(ImageFileLocationDefine.ORIGIN_IMAGES_LOCATION, file.getName());
////            System.out.println(file.getName()+ "的解析结果是：" + text);
////        }
//
////
//        String text = recognize(ImageFileLocationDefine.ORIGIN_IMAGES_LOCATION, "1484144038880.jpg");
//
//        System.out.println("1484144038880.jpg"+ "的解析结果是：" + text);

    }


}
