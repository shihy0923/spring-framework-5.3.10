package com;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @ClassName: MyMethodInterceptor.java
 * @Description:
 * @Version: v1.0.0
 * @author: shihy
 * @date 2022年02月17日
 */
public class MyMethodInterceptor implements MethodInterceptor {
	// 所有生成的代理方法都调用此方法而不是原始方法
	public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
		System.out.println("======插入前置通知======");
		methodProxy.invokeSuper(o, objects);
		System.out.println("======插入后者通知======");
		return null;
	}
}

