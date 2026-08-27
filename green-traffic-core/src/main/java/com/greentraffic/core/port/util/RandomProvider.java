package com.greentraffic.core.port.util;

/**
 * 随机数提供器端口，抽象出随机策略以便测试可控
 */
public interface RandomProvider {
    int nextInt(int bound);
    double nextDouble(double origin, double bound);
}
