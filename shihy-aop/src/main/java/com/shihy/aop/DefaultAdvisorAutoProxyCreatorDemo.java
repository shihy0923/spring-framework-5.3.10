package com.shihy.aop;

import com.shihy.UserService;
import com.shihy.aop.advice.ZhouyuAfterReturningAdvice;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 周瑜
 */
@Configuration
public class DefaultAdvisorAutoProxyCreatorDemo {

	@Bean
	public UserService userService() {
		return new UserService();
	}

	@Bean
	public DefaultPointcutAdvisor defaultPointcutAdvisor() {
		NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
		pointcut.addMethodName("test");

		DefaultPointcutAdvisor defaultPointcutAdvisor = new DefaultPointcutAdvisor();
		defaultPointcutAdvisor.setPointcut(pointcut);
		defaultPointcutAdvisor.setAdvice(new ZhouyuAfterReturningAdvice());

		return defaultPointcutAdvisor;
	}

	@Bean
	public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
		DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator = new DefaultAdvisorAutoProxyCreator();
		defaultAdvisorAutoProxyCreator.setProxyTargetClass(true);

		return defaultAdvisorAutoProxyCreator;
	}

	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext =
				new AnnotationConfigApplicationContext(DefaultAdvisorAutoProxyCreatorDemo.class);
		UserService userService = applicationContext.getBean("userService", UserService.class);
		userService.test();
	}
}
