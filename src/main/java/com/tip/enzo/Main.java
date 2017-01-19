package com.tip.enzo;

import com.tip.enzo.common.ImageVerifyCodeResult;
import com.tip.enzo.service.TianMaService;
import com.tip.enzo.service.XianlaiService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.CollectionUtils;

import java.util.List;


/**
 * Created by enzo on 17/1/4.
 */
public class Main {


    private static final Logger logger_deal = LoggerFactory.getLogger("deal");

    private static final Logger logger_success = LoggerFactory.getLogger("success");

    /**
     * 1.拉取号码
     * <p>
     * 循环使用号码去 请求【闲lai】——更改密码
     * 拉取图片验证码  --
     * * 解析图片验证码，若解析不出来，重新拉取图片验证码 --
     * * 请求【闲来】发送验证码接口，需要在同一个session下，记录在cookie里 -- 处理接口返回（成功，验证码错误，号码不是平台用户）
     * <p>
     * 2.循环号码去找回密码
     * 3.成功的话将结果记录
     *
     * @param args args
     */
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:/spring/*.xml");
        TianMaService tianMaService = context.getBean(TianMaService.class);
        XianlaiService xianlaiService = context.getBean(XianlaiService.class);


        try {

            while (true) {
                //1.获取可用号码
                List<String> phoneNumbers;

                try {
                    phoneNumbers = tianMaService.getPhoneNumber();
                } catch (Exception e) {
                    e.printStackTrace();
                    Thread.sleep(10000);
                    continue;
                }

                //{ test code
//                phoneNumbers = new ArrayList<>();
//                phoneNumbers.add("15755260209");
                //}

                if (CollectionUtils.isEmpty(phoneNumbers)) {
                    continue;
                }


                for (String phoneNumber : phoneNumbers) {
                    logger_deal.info("开始处理号码：" + phoneNumber);

                    String cookie = xianlaiService.getForgetPasswordDocument();

                    /*
                     * 1.获取验证码图片
                     * 2.解析图片验证码
                     */
                    String imgVerifyCode = "";
                    int retryTime = 0;
                    ImageVerifyCodeResult imageVerifyCodeResult = ImageVerifyCodeResult.NETWORK_ERROR;
                    while (StringUtils.isBlank(imgVerifyCode)) {
                        retryTime++;
                        String imgFileName = xianlaiService.fetchVerifyCodeImages(cookie);
                        imgVerifyCode = xianlaiService.decodeVerifyCode(imgFileName);
                        if (StringUtils.isBlank(imgVerifyCode) || imgVerifyCode.length() != 4) {
                            continue;
                        }
                        imageVerifyCodeResult =
                                xianlaiService.verifyImageCodeAndSendMsg(phoneNumber, imgVerifyCode, cookie);

                        if (imageVerifyCodeResult.equals(ImageVerifyCodeResult.SUCCESS)) {
                            break;
                        }
                        if (imageVerifyCodeResult.equals(ImageVerifyCodeResult.PHONE_NOT_EXIST)) {
                            break;
                        }
                        if (retryTime > 15) {
                            break;
                        }
                    }


                    switch (imageVerifyCodeResult) {
                        case SUCCESS: {
                            String smsCode = tianMaService.getSmsContent(phoneNumber);

                            if (StringUtils.isNotBlank(smsCode)) {
                                if (xianlaiService.toNext(phoneNumber, imgVerifyCode, smsCode, cookie)) {
                                    // 存储修改成功的号码
                                    System.out.println("修改密码成功，号码：" + phoneNumber);
                                    tianMaService.addOnBlackList(phoneNumber);
                                    logger_success.info("修改密码成功：" + phoneNumber);
                                }
                            }

                        }
                    }
                }

                //释放号码
                tianMaService.releasePhoneNumbers();


            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }


    }
}
