package com.example.cache;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 实体属性变化检测工具类
 * 用于检测实体对象属性是否发生变化
 */
public class PropertyChangeDetector {

    /**
     * 比较两个实体对象的属性变化
     * @param oldEntity 旧实体对象
     * @param newEntity 新实体对象
     * @param <T> 实体类型
     * @return 属性变化映射，key为属性名，value为变化后的属性值
     */
    public static <T extends Cacheable> Map<String, Object> detectChanges(T oldEntity, T newEntity) {
        Map<String, Object> changes = new HashMap<>();
        if (oldEntity == null || newEntity == null) {
            return changes;
        }

        Class<?> clazz = oldEntity.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object oldValue = field.get(oldEntity);
                Object newValue = field.get(newEntity);

                if (oldValue == null) {
                    if (newValue != null) {
                        changes.put(field.getName(), newValue);
                    }
                } else if (!oldValue.equals(newValue)) {
                    changes.put(field.getName(), newValue);
                }
            } catch (IllegalAccessException e) {
                // 忽略访问异常
            }
        }

        return changes;
    }

    /**
     * 比较两个实体对象的指定属性是否发生变化
     * @param oldEntity 旧实体对象
     * @param newEntity 新实体对象
     * @param properties 要比较的属性集合
     * @param <T> 实体类型
     * @return 属性变化映射，key为属性名，value为变化后的属性值
     */
    public static <T extends Cacheable> Map<String, Object> detectChanges(T oldEntity, T newEntity, Iterable<String> properties) {
        Map<String, Object> changes = new HashMap<>();
        if (oldEntity == null || newEntity == null) {
            return changes;
        }

        Class<?> clazz = oldEntity.getClass();

        for (String propertyName : properties) {
            try {
                Field field = clazz.getDeclaredField(propertyName);
                field.setAccessible(true);

                Object oldValue = field.get(oldEntity);
                Object newValue = field.get(newEntity);

                if (oldValue == null) {
                    if (newValue != null) {
                        changes.put(propertyName, newValue);
                    }
                } else if (!oldValue.equals(newValue)) {
                    changes.put(propertyName, newValue);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // 忽略不存在或不可访问的属性
            }
        }

        return changes;
    }

    /**
     * 将实体对象的指定属性设置为null
     * @param entity 实体对象
     * @param properties 要设置为null的属性集合
     * @param <T> 实体类型
     */
    public static <T extends Cacheable> void clearProperties(T entity, Iterable<String> properties) {
        if (entity == null) {
            return;
        }

        Class<?> clazz = entity.getClass();

        for (String propertyName : properties) {
            try {
                Field field = clazz.getDeclaredField(propertyName);
                field.setAccessible(true);

                // 检查属性类型是否为基本类型，基本类型不能设置为null
                if (!field.getType().isPrimitive()) {
                    field.set(entity, null);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // 忽略不存在或不可访问的属性
            }
        }
    }
}