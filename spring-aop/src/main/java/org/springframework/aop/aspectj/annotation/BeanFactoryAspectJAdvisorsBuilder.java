/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import org.aspectj.lang.reflect.PerClauseKind;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper for retrieving @AspectJ beans from a BeanFactory and building
 * Spring Advisors based on them, for use with auto-proxying.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see AnnotationAwareAspectJAutoProxyCreator
 */
public class BeanFactoryAspectJAdvisorsBuilder {

	private final ListableBeanFactory beanFactory;

	private final AspectJAdvisorFactory advisorFactory;

	@Nullable
	private volatile List<String> aspectBeanNames;

	private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();

	private final Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>();


	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 * @param beanFactory the ListableBeanFactory to scan
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory) {
		this(beanFactory, new ReflectiveAspectJAdvisorFactory(beanFactory));
	}

	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 * @param beanFactory the ListableBeanFactory to scan
	 * @param advisorFactory the AspectJAdvisorFactory to build each Advisor with
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		Assert.notNull(advisorFactory, "AspectJAdvisorFactory must not be null");
		this.beanFactory = beanFactory;
		this.advisorFactory = advisorFactory;
	}


	/**
	 * Look for AspectJ-annotated aspect beans in the current bean factory,
	 * and return to a list of Spring AOP Advisors representing them.
	 * <p>Creates a Spring Advisor for each AspectJ advice method.
	 * @return the list of {@link org.springframework.aop.Advisor} beans
	 * @see #isEligibleBean
	 *
	 * 本方法会被多次调用，因为一个Bean在判断要不要进行AOP时，都会调用这个方法
	 */
	public List<Advisor> buildAspectJAdvisors() {
		// aspectBeanNames是用来缓存BeanFactory中所存在的切面类的beanName的，第一次为null，后面就不为null了，不为null表示之前就已经找到过BeanFactory中的切面了
		List<String> aspectNames = this.aspectBeanNames;

		if (aspectNames == null) {
			synchronized (this) {
				aspectNames = this.aspectBeanNames;
				if (aspectNames == null) {
					List<Advisor> advisors = new ArrayList<>();
					aspectNames = new ArrayList<>();

					// 把所有beanNames拿出来遍历，判断某个bean的类型是否是Aspect
					//这一段的核心逻辑，是将 IOC 容器，以及它的父 IOC 容器中，所有的 bean 的名称全部取出（直接声明父类型为 Object ，显然是取所有），之后，它会逐个解析这些 bean 对应的 Class
					String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
							this.beanFactory, Object.class, true, false);
					for (String beanName : beanNames) {
						if (!isEligibleBean(beanName)) {
							continue;
						}
						// We must be careful not to instantiate beans eagerly as in this case they
						// would be cached by the Spring container but would not have been weaved.
						//根据beanName获取对应的类型
						Class<?> beanType = this.beanFactory.getType(beanName, false);
						if (beanType == null) {
							continue;
						}
						if (this.advisorFactory.isAspect(beanType)) {//判断这个bean是不是被Aspect注解了，即，它是不是一个切面
							// 当前解析bean的所属类型是一个切面类
							aspectNames.add(beanName);
							// 切面的注解信息
							AspectMetadata amd = new AspectMetadata(beanType, beanName);

							// 下面是单实例切面bean会走的流程
							// 如果@Aspect不是perthis、pertarget，那么一个切面只会生成一个对象（单例）
							// 并且会将该切面中所对应的Advisor对象进行缓存
							if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {

								MetadataAwareAspectInstanceFactory factory =
										new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);
								// 利用BeanFactoryAspectInstanceFactory来解析Aspect类, 生成该切面中所有的Advisor
								List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
								if (this.beanFactory.isSingleton(beanName)) {
									// 缓存切面所对应的所有Advisor对象
									this.advisorsCache.put(beanName, classAdvisors);
								}
								else {
									this.aspectFactoryCache.put(beanName, factory);
								}
								advisors.addAll(classAdvisors);
							}
							else {
								// Per target or per this.
								//对于原型切面 bean 的解析，它的核心解析动作依然是 advisorFactory.getAdvisors 方法，
								// 只是这里面不会再用到 advisorsCache 这个缓存区了，这也说明原型切面 bean 的解析是多次执行的
								if (this.beanFactory.isSingleton(beanName)) {
									throw new IllegalArgumentException("Bean with name '" + beanName +
											"' is a singleton, but aspect instantiation model is not singleton");
								}
								MetadataAwareAspectInstanceFactory factory =
										new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
								this.aspectFactoryCache.put(beanName, factory);
								// 利用PrototypeAspectInstanceFactory来解析Aspect类
								// PrototypeAspectInstanceFactory的父类为BeanFactoryAspectInstanceFactory
								// 这两个Factory的区别在于PrototypeAspectInstanceFactory的构造方法中会判断切面Bean是不是原型，除此之外没有其他区别
								// 所以主要就是BeanFactoryAspectInstanceFactory来负责生成切面实例对象
								//// 解析Aspect切面类，构造增强器
								advisors.addAll(this.advisorFactory.getAdvisors(factory));
							}
						}
					}
					//把所有的切面类的bean名称缓存
					this.aspectBeanNames = aspectNames;
					return advisors;
				}
			}
		}//这个分支走完后，容器中的所有切面都已经被解析完毕了，并放入org.springframework.aop.aspectj.annotation.BeanFactoryAspectJAdvisorsBuilder.advisorsCache这个缓存中了

		if (aspectNames.isEmpty()) {
			return Collections.emptyList();
		}

		// 如果走到这，说明之前已经解析过了所有的切面，直接从缓存里面拿就行了
		List<Advisor> advisors = new ArrayList<>();
		for (String aspectName : aspectNames) {
			List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
			if (cachedAdvisors != null) {
				advisors.addAll(cachedAdvisors);
			}
			else {
				MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
				advisors.addAll(this.advisorFactory.getAdvisors(factory));
			}
		}
		return advisors;
	}

	/**
	 * Return whether the aspect bean with the given name is eligible.
	 * @param beanName the name of the aspect bean
	 * @return whether the bean is eligible
	 */
	protected boolean isEligibleBean(String beanName) {
		return true;
	}

}
