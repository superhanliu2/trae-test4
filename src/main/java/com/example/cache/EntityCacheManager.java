package com.example.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体缓存管理器
 * 负责管理多种类型实体的独立缓存
 */
public class EntityCacheManager {
    private static final EntityCacheManager INSTANCE = new EntityCacheManager();
    
    // 存储不同类型的缓存实例，key为实体类名
    private final Map<String, EntityCache<?>> cacheMap = new ConcurrentHashMap<>();
    
    private EntityCacheManager() {
    }
    
    /**
     * 获取缓存管理器实例
     * @return 单例实例
     */
    public static EntityCacheManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 创建或获取指定类型的缓存
     * @param <T> 实体类型
     * @param entityClass 实体类
     * @return 实体缓存实例
     */
    @SuppressWarnings("unchecked")
    public <T extends Cacheable> EntityCache<T> getCache(Class<T> entityClass) {
        String className = entityClass.getName();
        return (EntityCache<T>) cacheMap.computeIfAbsent(className, k -> new EntityCache<>(entityClass));
    }
    
    /**
     * 移除指定类型的缓存
     * @param entityClass 实体类
     */
    public void removeCache(Class<?> entityClass) {
        String className = entityClass.getName();
        cacheMap.remove(className);
    }
    
    /**
     * 关闭所有缓存
     */
    public void shutdown() {
        cacheMap.values().forEach(EntityCache::shutdown);
        cacheMap.clear();
    }
}