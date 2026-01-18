package com.bookkeeping.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class DatabaseConfig {
    
    @Value("${spring.datasource.url}")
    private String datasourceUrl;
    
    /**
     * 应用启动时自动创建数据库目录
     */
    @PostConstruct
    public void initDatabaseDirectory() {
        // 从 jdbc:sqlite:./data/bookkeeping.db 中提取路径
        if (datasourceUrl.startsWith("jdbc:sqlite:")) {
            String dbPath = datasourceUrl.substring("jdbc:sqlite:".length());
            File dbFile = new File(dbPath);
            File dbDir = dbFile.getParentFile();
            
            if (dbDir != null && !dbDir.exists()) {
                boolean created = dbDir.mkdirs();
                if (created) {
                    System.out.println("数据库目录已创建: " + dbDir.getAbsolutePath());
                } else {
                    System.err.println("无法创建数据库目录: " + dbDir.getAbsolutePath());
                }
            }
        }
    }
}
