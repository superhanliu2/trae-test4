package com.example.cache;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 实体缓存实现类
 * 管理单一类型实体的缓存
 * @param <T> 实体类型
 */
public class EntityCache<T extends Cacheable> {
    private final Class<T> entityClass;
    private final Map<String, CacheEntry<T>> cache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> dirtyMap = new ConcurrentHashMap<>(); // 标记需要持久化的数据
    
    // 缓存配置
    private long maxSizeBytes = Long.MAX_VALUE;
    private int maxRecords = Integer.MAX_VALUE;
    private long persistenceIntervalMs = 60000; // 默认1分钟持久化一次
    
    // 持久化策略
    private PersistenceStrategy<T> persistenceStrategy;
    private ScheduledExecutorService scheduler;
    
    /**
     * 构造函数
     * @param entityClass 实体类
     */
    public EntityCache(Class<T> entityClass) {
        this.entityClass = entityClass;
    }
    
    /**
     * 设置缓存最大字节数
     * @param maxSizeBytes 最大字节数
     */
    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }
    
    /**
     * 设置缓存最大记录数
     * @param maxRecords 最大记录数
     */
    public void setMaxRecords(int maxRecords) {
        this.maxRecords = maxRecords;
    }
    
    /**
     * 设置持久化间隔
     * @param persistenceIntervalMs 持久化间隔（毫秒）
     */
    public void setPersistenceIntervalMs(long persistenceIntervalMs) {
        this.persistenceIntervalMs = persistenceIntervalMs;
    }
    
    /**
     * 设置持久化策略
     * @param persistenceStrategy 持久化策略
     */
    public void setPersistenceStrategy(PersistenceStrategy<T> persistenceStrategy) {
        this.persistenceStrategy = persistenceStrategy;
        if (persistenceStrategy != null) {
            startPersistenceScheduler();
        }
    }
    
    /**
     * 启动持久化调度器
     */
    private void startPersistenceScheduler() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(this::persistDirtyData, 
                persistenceIntervalMs, persistenceIntervalMs, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * 缓存条目类
     */
    private static class CacheEntry<T> {
        private final T data;
        private final long sizeBytes;
        private final long timestamp;
        
        CacheEntry(T data) {
            this.data = data;
            this.sizeBytes = estimateSize(data);
            this.timestamp = System.currentTimeMillis();
        }
        
        /**
         * 估算对象大小
         * @param obj 对象
         * @return 估计大小（字节）
         */
        private long estimateSize(Object obj) {
            // 简单估算，实际应用中可以使用更精确的方式
            return obj.toString().length() * 2L;
        }
    }
    
    /**
     * 添加或更新缓存
     * @param entity 实体对象
     * @return 旧实体对象（如果存在）
     */
    public T put(T entity) {
        String id = entity.getId();
        
        // 检查是否达到缓存上限
        if (cache.size() >= maxRecords && !cache.containsKey(id)) {
            logWarning("缓存记录数已达到上限: " + maxRecords);
            return null;
        }
        
        CacheEntry<T> entry = new CacheEntry<>(entity);
        CacheEntry<T> oldEntry = cache.put(id, entry);
        
        // 检查内存大小
        if (getCurrentSizeBytes() > maxSizeBytes) {
            cache.put(id, oldEntry); // 恢复旧值
            logWarning("缓存内存已达到上限: " + maxSizeBytes + " bytes");
            return null;
        }
        
        // 标记为需要持久化
        if (persistenceStrategy != null) {
            dirtyMap.put(id, true);
        }
        
        return oldEntry != null ? oldEntry.data : null;
    }
    
    /**
     * 批量添加或更新缓存
     * @param entities 实体对象集合
     * @return 添加失败的实体ID集合
     */
    public Set<String> putAll(Iterable<T> entities) {
        Set<String> failedIds = new HashSet<>();
        for (T entity : entities) {
            if (put(entity) == null) {
                failedIds.add(entity.getId());
            }
        }
        return failedIds;
    }
    
    /**
     * 根据ID获取实体
     * @param id 实体ID
     * @return 实体对象（如果存在）
     */
    public T get(String id) {
        CacheEntry<T> entry = cache.get(id);
        return entry != null ? entry.data : null;
    }
    
    /**
     * 根据ID删除实体
     * @param id 实体ID
     * @return 删除的实体对象（如果存在）
     */
    public T remove(String id) {
        CacheEntry<T> entry = cache.remove(id);
        dirtyMap.remove(id);
        
        if (persistenceStrategy != null && entry != null) {
            // 标记为需要持久化删除
            persistenceStrategy.deleteById(id);
        }
        
        return entry != null ? entry.data : null;
    }
    
    /**
     * 批量删除实体
     * @param ids 实体ID集合
     * @return 删除的实体对象集合
     */
    public Set<T> removeAll(Iterable<String> ids) {
        Set<T> removedEntities = new HashSet<>();
        for (String id : ids) {
            T entity = remove(id);
            if (entity != null) {
                removedEntities.add(entity);
            }
        }
        return removedEntities;
    }
    
    /**
     * 获取当前缓存大小（字节）
     * @return 缓存大小
     */
    private long getCurrentSizeBytes() {
        return cache.values().stream()
            .mapToLong(entry -> entry.sizeBytes)
            .sum();
    }
    
    /**
     * 获取当前缓存记录数
     * @return 记录数
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * 检查缓存是否为空
     * @return 是否为空
     */
    public boolean isEmpty() {
        return cache.isEmpty();
    }
    
    /**
     * 持久化脏数据
     */
    private void persistDirtyData() {
        if (persistenceStrategy == null || dirtyMap.isEmpty()) {
            return;
        }
        
        // 复制脏数据ID并清空脏映射
        Set<String> dirtyIds = Set.copyOf(dirtyMap.keySet());
        dirtyMap.keySet().removeAll(dirtyIds);
        
        // 获取需要持久化的实体
        Set<T> dirtyEntities = dirtyIds.stream()
            .map(this::get)
            .filter(entity -> entity != null)
            .collect(Collectors.toSet());
        
        if (!dirtyEntities.isEmpty()) {
            persistenceStrategy.saveOrUpdate(dirtyEntities);
        }
    }
    
    /**
     * 记录警告日志
     * @param message 日志信息
     */
    private void logWarning(String message) {
        System.err.println("[Cache Warning] " + entityClass.getName() + ": " + message);
    }
    
    /**
     * 关闭缓存
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
        
        // 持久化剩余脏数据
        persistDirtyData();
    }
}
