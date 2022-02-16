package com.tuling.aop;

import com.tuling.UserInterface;
import com.tuling.UserService;
import com.tuling.aop.advice.ZhouyuAroundAdvice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.cglib.proxy.Factory;

/**
 * @author 周瑜
 */
public class AdviceDemo {

	public static void main(String[] args) {
		UserService userService = new UserService();

		ProxyFactory proxyFactory = new ProxyFactory(userService);
		proxyFactory.addAdvice(new ZhouyuAroundAdvice());
//		proxyFactory.setProxyTargetClass(true);
		UserInterface proxy = (UserInterface) proxyFactory.getProxy();
		proxy.test();

		System.out.println(proxy instanceof Factory);
	}
}
