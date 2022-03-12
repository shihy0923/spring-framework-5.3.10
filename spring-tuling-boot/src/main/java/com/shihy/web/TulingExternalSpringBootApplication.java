package com.shihy.web;

import com.shihy.servlet.TulingServlet;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import javax.servlet.ServletRegistration;

/**
 * Created by xsls on 2019/8/18.
 */
public class TulingExternalSpringBootApplication {


    private static int port = 8099;
    private static String contextPath = "/tuling";

    public static void run() {
        Tomcat tomcat = new Tomcat();
        String baseDir = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        tomcat.setPort(port);

        try {
            Context context = tomcat.addContext(contextPath, baseDir);

            //ServletContextInitializer
            context.addServletContainerInitializer((c, servletContext) -> {

                ServletRegistration.Dynamic tulingServlet = servletContext.addServlet("TulingServlet", new TulingServlet());
                tulingServlet.addMapping("/tulingHello");
            }, null);

            // 启动tomcat
            tomcat.start();

        } catch (LifecycleException e) {
            e.printStackTrace();
        }
        // 挂起tomcat
        tomcat.getServer().await();

    }

}
