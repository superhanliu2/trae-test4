package com.example.cache;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * 缓存工具使用示例
 */
public class UsageExample {
    
    /**
     * 示例实体类：用户
     */
    public static class User implements Cacheable {
        private String id;
        private String name;
        private int age;
        
        public User(String id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }
        
        @Override
        public String getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public int getAge() {
            return age;
        }
        
        public void setAge(int age) {
            this.age = age;
        }
        
        @Override
        public String toString() {
            return "User{id='" + id + "', name='" + name + "', age=" + age + "}";
        }
    }
    
    /**
     * 示例实体类：订单
     */
    public static class Order implements Cacheable {
        private String id;
        private String userId;
        private double amount;
        
        public Order(String id, String userId, double amount) {
            this.id = id;
            this.userId = userId;
            this.amount = amount;
        }
        
        @Override
        public String getId() {
            return id;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public double getAmount() {
            return amount;
        }
        
        @Override
        public String toString() {
            return "Order{id='" + id + "', userId='" + userId + "', amount=" + amount + "}";
        }
    }
    
    public static void main(String[] args) throws SQLException, InterruptedException {
        // 创建数据库连接（示例使用H2内存数据库）
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", "");
        
        // 初始化数据库表
        initializeDatabase(connection);
        
        // 获取缓存管理器
        EntityCacheManager cacheManager = EntityCacheManager.getInstance();
        
        // 1. 用户缓存配置
        EntityCache<User> userCache = cacheManager.getCache(User.class);
        
        // 创建用户持久化配置
        EntityPersistenceConfig<User> userConfig = new EntityPersistenceConfig<User>("users")
                .addInsertableAndUpdatableProperty("name")
                .addInsertableAndUpdatableProperty("age")
                .addTransientProperty("lastUpdateTime"); // 最后更新时间作为瞬态属性
        
        cacheManager.setPersistenceConfig(User.class, userConfig);
        
        // 2. 订单缓存配置
        EntityCache<Order> orderCache = cacheManager.getCache(Order.class);
        
        // 创建订单持久化配置
        EntityPersistenceConfig<Order> orderConfig = new EntityPersistenceConfig<Order>("orders")
                .addInsertableAndUpdatableProperty("userId")
                .addInsertableAndUpdatableProperty("amount")
                .setCascadePersist(true); // 启用级联持久化
        
        cacheManager.setPersistenceConfig(Order.class, orderConfig);
        
        // 设置缓存参数示例
        userCache.setPersistenceIntervalMs(5000); // 每5秒持久化一次
        
        // 设置用户JDBC持久化策略
        userCache.setPersistenceStrategy(new JdbcPersistenceStrategy<User>(connection) {
            @Override
            protected String getSaveOrUpdateSql() {
                return "MERGE INTO users (id, name, age) VALUES (?, ?, ?)";
            }
            
            @Override
            protected String getDeleteSql() {
                return "DELETE FROM users WHERE id = ?";
            }
            
            @Override
            protected void setSaveOrUpdateParams(PreparedStatement stmt, User user) throws SQLException {
                stmt.setString(1, user.getId());
                stmt.setString(2, user.getName());
                stmt.setInt(3, user.getAge());
            }
            
            @Override
            protected void setDeleteParams(PreparedStatement stmt, String id) throws SQLException {
                stmt.setString(1, id);
            }
        });
        
        // 设置订单缓存参数
        orderCache.setMaxRecords(5000); // 最大5000条记录
        orderCache.setMaxSizeBytes(5 * 1024 * 1024); // 最大5MB内存
        orderCache.setPersistenceIntervalMs(10000); // 每10秒持久化一次
        
        // 设置订单JDBC持久化策略
        orderCache.setPersistenceStrategy(new JdbcPersistenceStrategy<Order>(connection) {
            @Override
            protected String getSaveOrUpdateSql() {
                return "MERGE INTO orders (id, user_id, amount) VALUES (?, ?, ?)";
            }
            
            @Override
            protected String getDeleteSql() {
                return "DELETE FROM orders WHERE id = ?";
            }
            
            @Override
            protected void setSaveOrUpdateParams(PreparedStatement stmt, Order order) throws SQLException {
                stmt.setString(1, order.getId());
                stmt.setString(2, order.getUserId());
                stmt.setDouble(3, order.getAmount());
            }
            
            @Override
            protected void setDeleteParams(PreparedStatement stmt, String id) throws SQLException {
                stmt.setString(1, id);
            }
        });
        
        // 3. 使用用户缓存
        User user1 = new User("user_001", "张三", 25);
        User user2 = new User("user_002", "李四", 30);
        
        userCache.put(user1);
        userCache.put(user2);
        
        System.out.println("获取用户信息: " + userCache.get("user_001"));
        System.out.println("获取用户信息: " + userCache.get("user_002"));
        
        // 更新用户信息
        user1.setName("张三更新");
        userCache.put(user1);
        
        System.out.println("更新后用户信息: " + userCache.get("user_001"));
        
        // 批量添加用户
        Set<User> batchUsers = new HashSet<>();
        batchUsers.add(new User("user_003", "王五", 28));
        batchUsers.add(new User("user_004", "赵六", 35));
        batchUsers.add(new User("user_005", "孙七", 40));
        
        Set<String> failedIds = userCache.putAll(batchUsers);
        System.out.println("批量添加失败的用户ID: " + failedIds);
        
        // 4. 使用订单缓存
        Order order1 = new Order("order_001", "user_001", 100.50);
        Order order2 = new Order("order_002", "user_002", 200.75);
        
        orderCache.put(order1);
        orderCache.put(order2);
        
        System.out.println("获取订单信息: " + orderCache.get("order_001"));
        System.out.println("获取订单信息: " + orderCache.get("order_002"));
        
        // 删除订单
        orderCache.remove("order_001");
        System.out.println("删除后订单信息: " + orderCache.get("order_001"));
        
        // 5. 等待持久化
        System.out.println("等待持久化完成...");
        Thread.sleep(15000);
        
        // 6. 验证持久化结果
        verifyPersistence(connection);
        
        // 7. 关闭缓存
        cacheManager.shutdown();
        connection.close();
        
        System.out.println("程序结束");
    }
    
    /**
     * 初始化数据库表
     * @param connection 数据库连接
     * @throws SQLException SQL异常
     */
    private static void initializeDatabase(Connection connection) throws SQLException {
        // 创建用户表
        String createUserTable = "CREATE TABLE users (" +
            "id VARCHAR(50) PRIMARY KEY, " +
            "name VARCHAR(100), " +
            "age INT" +
        ")";
        connection.createStatement().executeUpdate(createUserTable);
        
        // 创建订单表
        String createOrderTable = "CREATE TABLE orders (" +
            "id VARCHAR(50) PRIMARY KEY, " +
            "user_id VARCHAR(50), " +
            "amount DOUBLE" +
        ")";
        connection.createStatement().executeUpdate(createOrderTable);
    }
    
    /**
     * 验证持久化结果
     * @param connection 数据库连接
     * @throws SQLException SQL异常
     */
    private static void verifyPersistence(Connection connection) throws SQLException {
        // 查询用户表
        System.out.println("\n数据库用户表内容:");
        String queryUsers = "SELECT * FROM users";
        try (PreparedStatement stmt = connection.prepareStatement(queryUsers);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                System.out.println("  User: id=" + rs.getString("id") + ", name=" + rs.getString("name") + ", age=" + rs.getInt("age"));
            }
        }
        
        // 查询订单表
        System.out.println("\n数据库订单表内容:");
        String queryOrders = "SELECT * FROM orders";
        try (PreparedStatement stmt = connection.prepareStatement(queryOrders);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                System.out.println("  Order: id=" + rs.getString("id") + ", user_id=" + rs.getString("user_id") + ", amount=" + rs.getDouble("amount"));
            }
        }
    }
}