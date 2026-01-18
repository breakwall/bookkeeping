package com.bookkeeping;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试基础类
 * 提供通用的测试配置和工具方法
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlite:./data/test-bookkeeping.db",
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.jpa.show-sql=false",  // 测试时关闭SQL日志，减少输出
    "jwt.secret=test-secret-key-for-testing-only",
    "jwt.expiration=86400000"
})
@Transactional  // 每个测试方法执行后自动回滚，确保测试数据隔离
public abstract class AbstractBaseTest {
    
    /**
     * 生成唯一的测试用户名
     * 使用时间戳确保每次测试使用不同的用户名
     */
    protected String generateUniqueUsername() {
        return "testuser_" + System.currentTimeMillis();
    }
    
    /**
     * 生成唯一的测试邮箱
     */
    protected String generateUniqueEmail() {
        return "test_" + System.currentTimeMillis() + "@test.com";
    }
}
