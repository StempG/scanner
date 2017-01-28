package com.tip.enzo;

import com.tip.enzo.service.XianlaiService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by enzo on 17/1/16.
 */

@Deprecated
public class TestMethods {


    public static void main(String[] args) throws Exception {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:/spring/*.xml");
        XianlaiService xianlaiService = context.getBean(XianlaiService.class);

//
////        DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
//        String cookie = xianlaiService.getForgetPasswordDocument();
////        String cookie0 = "aliyungf_tc=AQAAAILxmEAWPgQAuW/Ac42yzYS+ja/o; acw_tc=AQAAAPDpKR52dgQAuW/Ac3DVrURPrU9x; SHRIOSESSIONID=1096caeb-3ac7-40e5-894b-730b729910a2";
//        String imgVerifyCode = "";
//        while (StringUtils.isBlank(imgVerifyCode)) {
//            String imgFileName = xianlaiService.fetchVerifyCodeImages(cookie);
//            imgVerifyCode = xianlaiService.decodeVerifyCode(imgFileName);
//        }
//
//        System.out.println(imgVerifyCode);
//        ImageVerifyCodeResult sendMSG = xianlaiService.verifyImageCodeAndSendMsg("18767122251", imgVerifyCode, cookie);
//
//        System.out.println(sendMSG);
//
//
////7B0A
//
//
////        String cookie = "SHRIOSESSIONID=229f623b-27f2-4229-9712-a1b77390c3a6; acw_tc=AQAAAKjF2BnUnwwAuW/AcwIh7R/FamhS; aliyungf_tc=AQAAAGAFGCsvXwwAuW/AcxMaj0Si5tjx";
//
////        String cookie = cookie0;
//        boolean ifToNext = xianlaiService.toNext("18767122251", imgVerifyCode, "40432", cookie);
//
//        System.out.println(ifToNext);
//
//        /*
//         * step 1
//         */
////        String cookie = xianlaiService.getForgetPasswordDocument(client, httpContext);


        xianlaiService.getUserLoginPage();



    }
}
