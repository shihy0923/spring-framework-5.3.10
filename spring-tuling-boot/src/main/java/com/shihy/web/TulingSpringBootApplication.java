package com.shihy.web;

import com.shihy.servlet.TulingServlet;
import org.apache.catalina.startup.Tomcat;

/**
 * Created by xsls on 2019/8/18.
 */
public class TulingSpringBootApplication {

    public static void run() throws Exception {

        // 创建Tomcat应用对象
        Tomcat tomcat = new Tomcat();
        // 设置Tomcat的端口号
        tomcat.setPort(8080);

        tomcat.addWebapp("/tuling","D:\\server");

        // 创建Servlet
        tomcat.addServlet("/tuling", "tulingServlet", new TulingServlet());
        // Servlet映射
        //standardContext.addServletMappingDecoded("/hello", "tulingServlet");
        //启动tomcat容器
        tomcat.start();
        //等待
        tomcat.getServer().await();
    }

}
