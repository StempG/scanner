package com.tip.enzo;

import com.tip.enzo.service.ScannerService;
import com.tip.enzo.service.TianMaService;
import com.tip.enzo.service.XianlaiService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by enzo on 17/1/4.
 *
 */
public class Main {


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


        int threadNumber = 3;
        ExecutorService pool = Executors.newFixedThreadPool(threadNumber);//创建一个固定大小为15的线程池

        try {
            for (int i = 0; i < threadNumber; i++) {
                pool.submit(new ScannerService(xianlaiService, tianMaService));
            }


        } catch (Exception e) {
            pool.shutdown();
            e.printStackTrace();
            System.exit(0);
        }
    }


}
