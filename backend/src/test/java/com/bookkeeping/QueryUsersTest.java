package com.bookkeeping;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * 查询数据库中的用户信息（调试工具）
 * 
 * 注意：这是一个调试工具脚本，不是单元测试。
 * 使用 @Disabled 标记，确保在运行所有测试时不会执行。
 * 如需手动运行，请使用：mvn test -Dtest=QueryUsersTest#queryUsers
 */
@SpringBootTest
@Disabled("调试工具，不作为常规测试执行")
public class QueryUsersTest {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Test
    public void queryUsers() {
        String sql = "SELECT id, username, email, password_hash, created_at FROM users ORDER BY id";
        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql);
        
        System.out.println("\n=== User Information in Database ===\n");
        
        if (users.isEmpty()) {
            System.out.println("No users found in database");
        } else {
            for (Map<String, Object> user : users) {
                System.out.println("User ID: " + user.get("id"));
                System.out.println("Username: " + user.get("username"));
                Object email = user.get("email");
                System.out.println("Email: " + (email != null && !email.toString().isEmpty() ? email : "Not set"));
                System.out.println("Password Hash: " + user.get("password_hash"));
                System.out.println("Created At: " + user.get("created_at"));
                System.out.println("----------------------------------------");
            }
        }
    }
}
