/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.framework.adapter;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of the {@link AdvisorAdapterRegistry} interface.
 * Supports {@link org.aopalliance.intercept.MethodInterceptor},
 * {@link org.springframework.aop.MethodBeforeAdvice},
 * {@link org.springframework.aop.AfterReturningAdvice},
 * {@link org.springframework.aop.ThrowsAdvice}.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class DefaultAdvisorAdapterRegistry implements AdvisorAdapterRegistry, Serializable {
    //里面放了Advisor适配器
	private final List<AdvisorAdapter> adapters = new ArrayList<>(3);


	/**
	 * Create a new DefaultAdvisorAdapterRegistry, registering well-known adapters.
	 */
	public DefaultAdvisorAdapterRegistry() {
		registerAdvisorAdapter(new MethodBeforeAdviceAdapter());
		registerAdvisorAdapter(new AfterReturningAdviceAdapter());
		registerAdvisorAdapter(new ThrowsAdviceAdapter());
	}


	@Override
	public Advisor wrap(Object adviceObject) throws UnknownAdviceTypeException {
		if (adviceObject instanceof Advisor) {
			return (Advisor) adviceObject;
		}
		if (!(adviceObject instanceof Advice)) {
			throw new UnknownAdviceTypeException(adviceObject);
		}
		Advice advice = (Advice) adviceObject;
		if (advice instanceof MethodInterceptor) {
			// So well-known it doesn't even need an adapter.
			return new DefaultPointcutAdvisor(advice);
		}
		for (AdvisorAdapter adapter : this.adapters) {
			// Check that it is supported.
			if (adapter.supportsAdvice(advice)) {
				return new DefaultPointcutAdvisor(advice);
			}
		}
		throw new UnknownAdviceTypeException(advice);
	}

	@Override
	public MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException {
		List<MethodInterceptor> interceptors = new ArrayList<>(3);
		Advice advice = advisor.getAdvice();//获取Advisor中的Advice对象
		//下面分两种情况：
		//情况1：
		//我们自定义的Advice都是会实现这个org.aopalliance.intercept.MethodInterceptor接口，所以在这里自定义的Advice就会被放到interceptors这个集合中
		//事务的Advice,org.springframework.transaction.interceptor.TransactionInterceptor就类似于我们自己实现的Advice,在这里就会被放入interceptors这个集合中
		//@After这个注解对应的AspectJAfterAdvice也是这个，具体的看MethodInterceptor实现类
		if (advice instanceof MethodInterceptor) {
			interceptors.add((MethodInterceptor) advice);
		}
		//情况2:
		//下面是处理AspectJ相关的三个特殊的注解的AOP的逻辑，即我们用到的@Before，@AfterReturning这两个注解对应的Advice，还有一个ThrowsAdvice类型的(没见过，不管了，看其它两个)，即AspectJMethodBeforeAdvice、AspectJAfterReturningAdvice这些个，
		//它们是在org.springframework.aop.aspectj.annotation.ReflectiveAspectJAdvisorFactory.getAdvice里面生成的，但是它们本身不是MethodInterceptor类型的，这里再包装成MethodInterceptor类型的。
		//将AspectJMethodBeforeAdvice、AspectJAfterReturningAdvice这些个Advice适配成对应的MethodInterceptor，即MethodBeforeAdviceInterceptor、AfterReturningAdviceInterceptor这些。如MethodBeforeAdviceInterceptor里面就关联了AspectJMethodBeforeAdvice
		//这里统一封装成MethodInterceptor的意义是为了后面统一调用它们的org.aopalliance.intercept.MethodInterceptor.invoke方法，在这个方法里面，实现了调用它所对应的Advice的接口的逻辑，比如MethodBeforeAdviceInterceptor的invoke方法实现的
		//逻辑就是先调用AspectJMethodBeforeAdvice的Before方法，然后再调用后面的Advice的逻辑，或者实际的方法。
		for (AdvisorAdapter adapter : this.adapters) {//遍历Advisor适配器，对Advice进行包装
			if (adapter.supportsAdvice(advice)) {//判断当前的适配器是否符合这个Advice
				//将Advice包装成对应的MethodInterceptor，一般是 MethodBeforeAdviceInterceptor、AfterReturningAdviceInterceptor、ThrowsAdviceInterceptor，这三个，这三个同时又实现了Advice接口
				interceptors.add(adapter.getInterceptor(advisor));
			}
		}
		if (interceptors.isEmpty()) {
			throw new UnknownAdviceTypeException(advisor.getAdvice());
		}
		return interceptors.toArray(new MethodInterceptor[0]);
	}

	@Override
	public void registerAdvisorAdapter(AdvisorAdapter adapter) {
		this.adapters.add(adapter);
	}

}
