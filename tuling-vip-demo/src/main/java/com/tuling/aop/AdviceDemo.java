package com.tuling.aop;

import com.tuling.UserService;
import com.tuling.aop.advice.ZhouyuAroundAdvice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.cglib.core.DebuggingClassWriter;
import org.springframework.cglib.proxy.Factory;

/**
 * @author 周瑜
 */
public class AdviceDemo {

	public static void main(String[] args) {

		System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "E:\\shihy\\CodeOfFrameSource\\spring-framework-5.3.10\\tuling-vip-demo\\build\\classes\\java\\main\\com\\tuling");
		UserService userService = new UserService();

		ProxyFactory proxyFactory = new ProxyFactory(userService);
		//JDK动态代理
//		proxyFactory.addAdvice(new ZhouyuAroundAdvice());
//		UserInterface proxy = (UserInterface) proxyFactory.getProxy();

		proxyFactory.addAdvice(new ZhouyuAroundAdvice());

		proxyFactory.setProxyTargetClass(true);
		UserService proxy = (UserService) proxyFactory.getProxy();

		proxy.test();

		System.out.println(proxy instanceof Factory);



//		//原生的Cglib动态代理
//		Enhancer enhancer = new Enhancer();
//		// 为代理类提供父类或接口。接口的话会比类有些特殊，生成的代理类会默认继承Object，那么所有方法的
//		// 调用都会调用到Object类，若调用的方法是在接口中定义的，很明显Object中不可能有我们自己定义的方法，就会报
//		// Exception in thread "main" java.lang.NoSuchMethodError: java.lang.Object.getBelief()Ljava/lang/String;
//		enhancer.setSuperclass(PersonInterface.class);
//		// 设置enhancer的回调对象,Callback的子类
//		enhancer.setCallback(new MyMethodInterceptor());
//		// 创建代理类和代理对象
//		PersonInterface proxy= (PersonInterface)enhancer.create();
//		// 通过代理对象调用目标方法
//		proxy.personTest();

	}
}
