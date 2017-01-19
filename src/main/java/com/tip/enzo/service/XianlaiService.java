package com.tip.enzo.service;

import com.tip.enzo.common.ImageVerifyCodeResult;
import com.tip.enzo.helper.RecognizeImageHelper;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by enzo on 17/1/4.
 * <p>
 * 用于先来网站的service
 */
@Service
public class XianlaiService {


    @Value("${verify.image.path}")
    private String verifyImagePath;


    public String getForgetPasswordDocument() {
        HttpGet get = new HttpGet("http://vip.xianlaihy.com/login/toForgetPassword");
        get.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        get.setHeader("Accept-Encoding", "gzip, deflate, sdch");
        get.setHeader("Accept-Language", "zh-CN,zh;q=0.8");
        get.setHeader("Cache-Control", "max-age=0");
        get.setHeader("Connection", "keep-alive");
        get.setHeader("Host", "vip.xianlaihy.com");
        get.setHeader("Upgrade-Insecure-Requests", "1");
        get.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36");

        StringBuilder cookie = new StringBuilder();

        String SHRIOSESSIONID = "";
        String acw_tc = "";
        String aliyungf_tc = "";
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            client.execute(get);

            for (Cookie c : client.getCookieStore().getCookies()) {
                if (c.getName().equals("SHRIOSESSIONID")) {
                    SHRIOSESSIONID = c.getValue();
                }
                if (c.getName().equals("acw_tc")) {
                    acw_tc = c.getValue();
                }
                if (c.getName().equals("aliyungf_tc")) {
                    aliyungf_tc = c.getValue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        cookie.append("aliyungf_tc=").append(aliyungf_tc).append("; acw_tc=").append(acw_tc).append("; SHRIOSESSIONID=").append(SHRIOSESSIONID);
//        System.out.println("执行execute方法之后的cookie:");
//        System.out.println(cookie.toString());

        return cookie.toString();
    }


    /**
     * 拉取图片验证码
     *
     * @return fileName 文件名，不含文件路径
     */
    public String fetchVerifyCodeImages(String cookie) {
        try {
            String fileLocation = this.verifyImagePath + "origin/";
            String fileName = String.valueOf(new Date().getTime()) + ".jpg";
            File file = new File(fileLocation + fileName);
            OutputStream os = new FileOutputStream(file);

            String url = "http://vip.xianlaihy.com/login/getForCaptcher?t=" + getRandomNumber();

            HttpGet get = new HttpGet(url);
            get.setHeader("Host", "vip.xianlaihy.com");
            get.setHeader("Connection", "keep-alive");
            get.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36");
            get.setHeader("Accept", "image/webp,image/*,*/*;q=0.8");
            get.setHeader("Referer", "http://vip.xianlaihy.com/login/toForgetPassword");
            get.setHeader("Accept-Encoding", "gzip, deflate, sdch");
            get.setHeader("Accept-Language", "zh-CN,zh;q=0.8");
            get.setHeader("Cookie", cookie);
            HttpResponse response = HttpClients.createDefault().execute(get);


            InputStream is = response.getEntity().getContent();

            byte[] buff = new byte[1024];
            while (true) {
                int readLength = is.read(buff);
                if (readLength == -1) {
                    break;
                }
                byte[] temp = new byte[readLength];
                System.arraycopy(buff, 0, temp, 0, readLength);
                //写入文件
                os.write(temp);
            }
            is.close();
            os.close();

            return fileName;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public String decodeVerifyCode(String fileName) {
//        ImageFileLocationDefine.ORIGIN_IMAGES_LOCATION
        return RecognizeImageHelper.recognize(this.verifyImagePath, fileName);

    }


    /**
     * 验证图片验证码并发送短信
     *
     * @param phoneNumber   手机号码
     * @param imgVerifyCode 解析出来的验证码
     * @return 处理结果
     */
    public ImageVerifyCodeResult verifyImageCodeAndSendMsg(String phoneNumber, String imgVerifyCode, String cookie) {
        String url = "http://vip.xianlaihy.com/common/sendFindPswMsg?t=" + getRandomNumber() + "&tel=" + phoneNumber
                + "&verCode=" + imgVerifyCode;

        HttpGet httpGet = new HttpGet(url);

        httpGet.setHeader("Accept", "*/*");
        httpGet.setHeader("Accept-Encoding", "gzip, deflate, sdch");
        httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.8");
        httpGet.setHeader("Connection", "keep-alive");
        httpGet.setHeader("Host", "vip.xianlaihy.com");
        httpGet.setHeader("Referer", "http://vip.xianlaihy.com/login/toForgetPassword");
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36");
        httpGet.setHeader("X-Requested-With", "XMLHttpRequest");
        httpGet.setHeader("Cookie", cookie);


        try {
            HttpResponse response = HttpClients.createDefault().execute(httpGet);
            if (response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300) {
                String entity = EntityUtils.toString(response.getEntity());

                switch (entity) {
                    case "error":
                        return ImageVerifyCodeResult.PHONE_NOT_EXIST;
                    case "verError":
                        return ImageVerifyCodeResult.VERIFY_CODE_FAIL;
                    case "success":
                        return ImageVerifyCodeResult.SUCCESS;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ImageVerifyCodeResult.NETWORK_ERROR;
    }


    public boolean toNext(String phoneNumber, String imgVerifyCode, String smsCode, String cookie) {
        String url = "http://vip.xianlaihy.com/login/toNext";
        HttpPost post = new HttpPost(url);
        List<NameValuePair> list = new ArrayList<>();
        list.add(new BasicNameValuePair("tel", phoneNumber));
        list.add(new BasicNameValuePair("verCode", imgVerifyCode));
        list.add(new BasicNameValuePair("checkcode", smsCode));

        post.setHeader("Accept", "*/*");
        post.setHeader("Accept-Encoding", "gzip, deflate");
        post.setHeader("Accept-Language", "zh-CN,zh;q=0.8");
        post.setHeader("Connection", "keep-alive");
        post.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        post.setHeader("Host", "vip.xianlaihy.com");
        post.setHeader("Origin", "http://vip.xianlaihy.com");
        post.setHeader("Referer", "http://vip.xianlaihy.com/login/toForgetPassword");
        post.setHeader("X-Requested-With", "XMLHttpRequest");
        post.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36");
        post.setHeader("Cookie", cookie);
        try {
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list, "UTF-8");
            post.setEntity(entity);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            HttpResponse response = HttpClients.createDefault().execute(post);
            if (response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300) {

                String result = EntityUtils.toString(response.getEntity());
                if (!result.contains("1")) {
//                    System.out.println("to Next 失败");
                    return false;
                }
                //change password


                getDocument(cookie);
                Boolean x = changePwd(cookie);
                if (x != null) return x;


            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }


    private void getDocument(String cookie) {
        HttpGet get = new HttpGet("http://vip.xianlaihy.com/login/toModifyPassword");
        get.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\n");
        get.setHeader("Accept-Encoding", "gzip, deflate, sdch");
        get.setHeader("Accept-Language", "zh-CN,zh;q=0.8");
        get.setHeader("Connection", "keep-alive");
        get.setHeader("Host", "vip.xianlaihy.com");
        get.setHeader("Upgrade-Insecure-Requests", "1");
        get.setHeader("Referer", "http://vip.xianlaihy.com/login/toForgetPassword");
        get.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36");
        get.setHeader("Cookie", cookie);
        try {
            HttpClients.createDefault().execute(get);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static Boolean changePwd(String cookie) throws IOException {
        String changePasswordUrl = "http://vip.xianlaihy.com/login/updatePsw?t=" + getRandomNumber();
        HttpPost post = new HttpPost(changePasswordUrl);
        post.setHeader("Host", "vip.xianlaihy.com");
        post.setHeader("Connection", "keep-alive");
        post.setHeader("Accept", "*/*");
        post.setHeader("Origin", "http://vip.xianlaihy.com");
        post.setHeader("X-Requested-With", "XMLHttpRequest");
        post.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36");
        post.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        post.setHeader("Referer", "http://vip.xianlaihy.com/login/toModifyPassword");
        post.setHeader("Accept-Encoding", "gzip, deflate");
        post.setHeader("Accept-Language", "zh-CN,zh;q=0.8");
        post.setHeader("Cookie", cookie);


        List<NameValuePair> list = new ArrayList<>();
        list.add(new BasicNameValuePair("psw", "abc123456"));
        list.add(new BasicNameValuePair("confirm", "abc123456"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list, "UTF-8");
        post.setEntity(entity);

        HttpResponse changeResponse = HttpClients.createDefault().execute(post);

        if (changeResponse.getStatusLine().getStatusCode() >= 200 && changeResponse.getStatusLine().getStatusCode() < 300) {
            String changeResultEntity = EntityUtils.toString(changeResponse.getEntity());
            if (changeResultEntity.contains("ok")) {
//                System.out.println("修改密码成功");
                return true;
            }
            if (changeResultEntity.contains("0")) {
//                System.out.println("手机号不存在！");
                return false;
            }
        }
        return null;
    }


    private static double getRandomNumber() {
        return Math.random();
    }
}
