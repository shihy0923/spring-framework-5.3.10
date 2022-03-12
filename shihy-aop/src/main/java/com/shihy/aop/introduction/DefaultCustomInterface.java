package com.shihy.aop.introduction;

/**
 * @author 周瑜
 */
public class DefaultCustomInterface  implements CustomInterface{

	@Override
	public void custom() {
		System.out.println("custom..");
	}
}
