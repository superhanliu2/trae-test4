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
    
    // 存储不同类型的持久化配置，key为实体类名
    private final Map<String, EntityPersistenceConfig<?>> persistenceConfigMap = new ConcurrentHashMap<>();
    
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
     * 设置实体类的持久化配置
     * @param <T> 实体类型
     * @param entityClass 实体类
     * @param config 持久化配置
     */
    public <T extends Cacheable> void setPersistenceConfig(Class<T> entityClass, EntityPersistenceConfig<T> config) {
        String className = entityClass.getName();
        persistenceConfigMap.put(className, config);
        
        // 同时将配置应用到对应的缓存
        EntityCache<T> cache = getCache(entityClass);
        cache.setPersistenceConfig(config);
    }
    
    /**
     * 获取实体类的持久化配置
     * @param <T> 实体类型
     * @param entityClass 实体类
     * @return 持久化配置
     */
    @SuppressWarnings("unchecked")
    public <T extends Cacheable> EntityPersistenceConfig<T> getPersistenceConfig(Class<T> entityClass) {
        String className = entityClass.getName();
        return (EntityPersistenceConfig<T>) persistenceConfigMap.get(className);
    }
    
    /**
     * 移除指定类型的缓存
     * @param entityClass 实体类
     */
    public void removeCache(Class<?> entityClass) {
        String className = entityClass.getName();
        cacheMap.remove(className);
        persistenceConfigMap.remove(className);
    }
    
    /**
     * 关闭所有缓存
     */
    public void shutdown() {
        cacheMap.values().forEach(EntityCache::shutdown);
        cacheMap.clear();
        persistenceConfigMap.clear();
    }
}