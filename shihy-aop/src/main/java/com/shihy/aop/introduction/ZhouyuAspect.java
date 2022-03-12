package com.shihy.aop.introduction;

import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

/**
 * @author 周瑜
 */
@Aspect
@Component
public class ZhouyuAspect {

	@DeclareParents(value = "com.shihy.aop.introduction.CustomService", defaultImpl = DefaultCustomInterface.class)
	private CustomInterface customInterface;

}
