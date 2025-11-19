package com.example.cache;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 实体持久化配置类
 * 管理实体的持久化属性、表名等配置信息
 * @param <T> 实体类型
 */
public class EntityPersistenceConfig<T extends Cacheable> {
    private final String tableName;
    private final Set<String> insertableProperties;
    private final Set<String> updatableProperties;
    private final Set<String> transientProperties;
    private boolean cascadePersist = false;

    /**
     * 构造函数
     * @param tableName 表名
     */
    public EntityPersistenceConfig(String tableName) {
        this.tableName = tableName;
        this.insertableProperties = new HashSet<>();
        this.updatableProperties = new HashSet<>();
        this.transientProperties = new HashSet<>();
    }

    /**
     * 获取表名
     * @return 表名
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * 添加可插入属性
     * @param propertyName 属性名
     * @return 当前配置对象
     */
    public EntityPersistenceConfig<T> addInsertableProperty(String propertyName) {
        insertableProperties.add(propertyName);
        return this;
    }

    /**
     * 添加可更新属性
     * @param propertyName 属性名
     * @return 当前配置对象
     */
    public EntityPersistenceConfig<T> addUpdatableProperty(String propertyName) {
        updatableProperties.add(propertyName);
        return this;
    }

    /**
     * 添加可插入和可更新属性
     * @param propertyName 属性名
     * @return 当前配置对象
     */
    public EntityPersistenceConfig<T> addInsertableAndUpdatableProperty(String propertyName) {
        insertableProperties.add(propertyName);
        updatableProperties.add(propertyName);
        return this;
    }

    /**
     * 添加瞬态属性（持久化后从内存中移除）
     * @param propertyName 属性名
     * @return 当前配置对象
     */
    public EntityPersistenceConfig<T> addTransientProperty(String propertyName) {
        transientProperties.add(propertyName);
        return this;
    }

    /**
     * 设置是否级联持久化子属性
     * @param cascadePersist 是否级联持久化
     * @return 当前配置对象
     */
    public EntityPersistenceConfig<T> setCascadePersist(boolean cascadePersist) {
        this.cascadePersist = cascadePersist;
        return this;
    }

    /**
     * 获取可插入属性集合
     * @return 可插入属性集合
     */
    public Set<String> getInsertableProperties() {
        return Collections.unmodifiableSet(insertableProperties);
    }

    /**
     * 获取可更新属性集合
     * @return 可更新属性集合
     */
    public Set<String> getUpdatableProperties() {
        return Collections.unmodifiableSet(updatableProperties);
    }

    /**
     * 获取瞬态属性集合
     * @return 瞬态属性集合
     */
    public Set<String> getTransientProperties() {
        return Collections.unmodifiableSet(transientProperties);
    }

    /**
     * 判断是否需要级联持久化
     * @return 是否级联持久化
     */
    public boolean isCascadePersist() {
        return cascadePersist;
    }
}