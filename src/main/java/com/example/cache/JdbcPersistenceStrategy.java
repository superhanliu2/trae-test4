package com.example.cache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JDBC持久化策略实现
 * 基于JDBC将缓存数据持久化到数据库
 * @param <T> 实体类型
 */
public abstract class JdbcPersistenceStrategy<T extends Cacheable> implements PersistenceStrategy<T> {
    private final Connection connection;
    private final AtomicLong batchSize = new AtomicLong(0);
    private int maxBatchSize = 100; // 默认批量大小
    
    /**
     * 构造函数
     * @param connection 数据库连接
     */
    protected JdbcPersistenceStrategy(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * 获取数据库连接
     * @return 数据库连接
     */
    protected Connection getConnection() {
        return connection;
    }
    
    /**
     * 设置最大批量大小
     * @param maxBatchSize 最大批量大小
     */
    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }
    
    /**
     * 获取保存或更新的SQL语句
     * @return SQL语句
     */
    protected abstract String getSaveOrUpdateSql();
    
    /**
     * 获取删除SQL语句
     * @return SQL语句
     */
    protected abstract String getDeleteSql();
    
    /**
     * 设置保存或更新的PreparedStatement参数
     * @param stmt PreparedStatement
     * @param entity 实体对象
     * @throws SQLException SQL异常
     */
    protected abstract void setSaveOrUpdateParams(PreparedStatement stmt, T entity) throws SQLException;
    
    /**
     * 设置删除的PreparedStatement参数
     * @param stmt PreparedStatement
     * @param id 实体ID
     * @throws SQLException SQL异常
     */
    protected abstract void setDeleteParams(PreparedStatement stmt, String id) throws SQLException;
    
    @Override
    public void saveOrUpdate(Set<T> entities) {
        if (entities.isEmpty()) {
            return;
        }
        
        String sql = getSaveOrUpdateSql();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (T entity : entities) {
                setSaveOrUpdateParams(stmt, entity);
                stmt.addBatch();
                
                // 达到批量大小则执行
                if (batchSize.incrementAndGet() >= maxBatchSize) {
                    executeBatch(stmt);
                    batchSize.set(0);
                }
            }
            
            // 执行剩余批量
            executeBatch(stmt);
            batchSize.set(0);
            
        } catch (SQLException e) {
            handleSQLException("saveOrUpdate", e);
        }
    }
    
    @Override
    public void deleteById(String id) {
        String sql = getDeleteSql();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setDeleteParams(stmt, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            handleSQLException("deleteById", e);
        }
    }
    
    /**
     * 执行批量操作
     * @param stmt PreparedStatement
     * @throws SQLException SQL异常
     */
    private void executeBatch(PreparedStatement stmt) throws SQLException {
        int[] results = stmt.executeBatch();
        // 可以在这里处理执行结果
    }
    
    /**
     * 处理SQL异常
     * @param operation 操作名称
     * @param e SQL异常
     */
    protected void handleSQLException(String operation, SQLException e) {
        System.err.println("[JDBC Persistence Error] " + operation + ": " + e.getMessage());
        e.printStackTrace();
    }
    
    /**
     * 检查实体是否存在于数据库
     * @param id 实体ID
     * @return 是否存在
     */
    protected boolean exists(String id) {
        // 默认实现，子类可以重写以提高性能
        String sql = getDeleteSql(); // 使用删除SQL的where条件
        sql = sql.replaceFirst("delete\\s+from\\s+", "select 1 from ")
                .replaceFirst("\\s+where\\s+", " where ");
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setDeleteParams(stmt, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            handleSQLException("exists", e);
            return false;
        }
    }
}