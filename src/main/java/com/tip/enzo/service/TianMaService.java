package com.tip.enzo.service;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by enzo on 17/1/4.
 * <p>
 * 用于天码网站的service
 */
@Service
public class TianMaService {


    @Value("${tianma.account.username}")
    private String userName;
    @Value("${tianma.account.password}")
    private String password;

    @Value("${tianma.item.id}")
    private Long itemId;

    private final static int interval = 5;//min
    private final static int vCodeLength = 5;
    private final static int phoneArraySize = 15;

    private Date getTokenTimeStamp;
    private String token;


    /**
     * 获取登录token
     *
     * @return token
     * @throws Exception e
     */
    private String getToken() throws Exception {
        //没有token时间或者token时间过期
        if (this.getTokenTimeStamp == null ||
                (new Date().getTime() - this.getTokenTimeStamp.getTime()) > interval * 60 * 1000) {

            HttpClient client = HttpClients.createDefault();
            String url = "http://api.tianma168.com/tm/Login?uName=" +
                    this.userName + "&pWord=" + this.password + "&Code=UTF8";
            HttpGet httpGet = new HttpGet(url);
            HttpResponse response = client.execute(httpGet);
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                String entity = EntityUtils.toString(response.getEntity());
                if (entity.contains("False")) {
                    throw new Exception("请求出错：" + entity);
                }
                String[] results = entity.split("&");
                if (results.length > 0) {
                    //记录token获取的时间
                    this.getTokenTimeStamp = new Date();
                    this.token = results[0];
                    return this.token;
                }
            }
        }

        return this.token;
    }


    private void releaseToken(String token) throws Exception {
        HttpClient client = HttpClients.createDefault();
        String url = "http://api.tianma168.com/tm/Exit?token" + token + "&Code=UTF8";
        HttpGet httpGet = new HttpGet(url);
        client.execute(httpGet);
    }

    /**
     * 获取号码列表
     *
     * @return list
     * @throws Exception e
     */
    public List<String> getPhoneNumber() throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        String url = "http://api.tianma168.com/tm/getPhone?ItemId=" +
                this.itemId + "&token=" + this.getToken() + "&Count=" + phoneArraySize + "&Code=UTF8";
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = client.execute(httpGet);
        int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            String entity = EntityUtils.toString(response.getEntity());
            if (entity.contains("False")) {
                if (entity.contains("请先释放号码")) {
                    this.releasePhoneNumbers();
                    return new ArrayList<>();
                } else {
                    throw new Exception("请求出错：" + entity);
                }
            }

            String[] results = entity.split(";");
            if (results.length > 0) {
                List<String> phones = new ArrayList<>();
                Collections.addAll(phones, results);
                client.close();
                return phones;
            }
        }

        client.close();
        return null;
    }


    /**
     * 监听手机号码
     *
     * @param phoneNumber 手机号码
     * @return vCode
     */
    public String getSmsContent(String phoneNumber) throws Exception {
        int maxLoopTimes = 15;

        for (int i = 0; i < maxLoopTimes; i++) {
            HttpClient client = HttpClients.createDefault();
            String url = "http://api.tianma168.com/tm/getMessage?token=" +
                    this.getToken() + "&itemId=" + this.itemId + "&phone=" + phoneNumber + "&Code=UTF8";
            HttpGet httpGet = new HttpGet(url);
            HttpResponse response = client.execute(httpGet);
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                String entity = EntityUtils.toString(response.getEntity());
                if (entity.equals("False:没有短信，请5秒后再试")) {
                    Thread.sleep(5000);
                    continue;
                }

                if (entity.equals("False:此号码已经被释放")) {
                    break;
                }


                if (entity.contains("MSG")) {
                    String[] strings = entity.split("&");
                    return getVerifyCodeFromContent(strings[1]);
                }
            }
        }

        return null;
    }


    /**
     * 释放号码
     *
     * @return 是否成功
     * @throws Exception e
     */
    public boolean releasePhoneNumbers() throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        String url = "http://api.tianma168.com/tm/releaseAllPhone?token=" + this.getToken();
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = client.execute(httpGet);
        int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            String entity = EntityUtils.toString(response.getEntity());
            if (entity.contains("Ok")) {
                client.close();
                return true;
            }

        }

        client.close();
        return false;
    }


    public void addOnBlackList(String phoneNumber) throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        String url = "http://api.tianma168.com/tm/addBlack?token=" + this.getToken() + "&phoneList=" +
                this.itemId + "-" + phoneNumber + ";";
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = client.execute(httpGet);
        int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            String entity = EntityUtils.toString(response.getEntity());
            if (entity.contains("Ok")) {
                client.close();
            }
        }

        client.close();
    }


    private String getVerifyCodeFromContent(String content) {
        String vCode = "";
        int num = 0;
        for (int i = 0; i < content.length(); i++) {
            String s = content.substring(i, i + 1);
            if (NumberUtils.isNumber(s)) {
                vCode += s;
                num++;
                if (num == vCodeLength) {
                    return vCode;
                }
            }
        }

        return vCode;
    }


}
