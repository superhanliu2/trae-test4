package com.example.cache;

import java.util.Set;

/**
 * 持久化策略接口
 * 定义缓存数据持久化的方法
 * @param <T> 实体类型
 */
public interface PersistenceStrategy<T extends Cacheable> {
    /**
     * 保存或更新实体集合
     * @param entities 实体集合
     */
    void saveOrUpdate(Set<T> entities);
    
    /**
     * 插入实体集合
     * @param entities 实体集合
     */
    default void insert(Set<T> entities) {
        saveOrUpdate(entities);
    }
    
    /**
     * 更新实体集合
     * @param entities 实体集合
     */
    default void update(Set<T> entities) {
        saveOrUpdate(entities);
    }
    
    /**
     * 根据ID删除实体
     * @param id 实体ID
     */
    void deleteById(String id);
    
    /**
     * 根据ID集合删除实体
     * @param ids 实体ID集合
     */
    default void deleteByIds(Set<String> ids) {
        ids.forEach(this::deleteById);
    }
}