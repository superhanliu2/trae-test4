package com.example.cache;

/**
 * 可缓存实体接口
 * 所有需要缓存的实体类必须实现此接口
 */
public interface Cacheable {
    /**
     * 获取实体的唯一ID
     * @return 唯一ID字符串
     */
    String getId();
}
