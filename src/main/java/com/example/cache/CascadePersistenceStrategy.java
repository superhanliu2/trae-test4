package com.example.cache;

import java.util.Set;

/**
 * 级联持久化策略接口
 * 支持子属性的持久化
 * @param <T> 实体类型
 */
public interface CascadePersistenceStrategy<T extends Cacheable> extends PersistenceStrategy<T> {
    /**
     * 获取子属性的缓存
     * @param <C> 子属性类型
     * @param childEntityClass 子属性实体类
     * @return 子属性缓存
     */
    <C extends Cacheable> EntityCache<C> getChildCache(Class<C> childEntityClass);
    
    /**
     * 持久化实体及其子属性
     * @param entity 实体对象
     */
    void cascadeSaveOrUpdate(T entity);
    
    /**
     * 插入实体及其子属性
     * @param entity 实体对象
     */
    default void cascadeInsert(T entity) {
        cascadeSaveOrUpdate(entity);
    }
    
    /**
     * 更新实体及其子属性
     * @param entity 实体对象
     */
    default void cascadeUpdate(T entity) {
        cascadeSaveOrUpdate(entity);
    }
    
    /**
     * 级联持久化实体集合
     * @param entities 实体集合
     */
    default void cascadeSaveOrUpdate(Set<T> entities) {
        entities.forEach(this::cascadeSaveOrUpdate);
    }
}