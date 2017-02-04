package com.tip.enzo;

import com.tip.enzo.service.ScannerService;
import com.tip.enzo.service.TianMaService;
import com.tip.enzo.service.XianlaiService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestMutiThread {


    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:/spring/*.xml");
        TianMaService tianMaService = context.getBean(TianMaService.class);
        XianlaiService xianlaiService = context.getBean(XianlaiService.class);


        ThreadPoolExecutor executor = new ThreadPoolExecutor(15, 20, 200, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(15));

        for (int i = 0; i < 15; i++) {
            ScannerService scanner = new ScannerService(xianlaiService, tianMaService);
            executor.execute(scanner);
        }
        executor.shutdown();


    }
}
