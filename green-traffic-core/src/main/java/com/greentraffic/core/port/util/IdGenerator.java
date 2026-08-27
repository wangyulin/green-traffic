package com.greentraffic.core.port.util;

/**
 * 生成唯一 ID 的端口（核心不依赖具体实现）
 */
public interface IdGenerator {
    String generate();
}
