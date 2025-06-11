package com.czdxwx.aiinterviewcoachbackend.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@Configuration
// 【关键】只扫描存放 MySQL 相关 Mapper 的包
@MapperScan(basePackages = "com.czdxwx.aiinterviewcoachbackend.mapper.mysql", sqlSessionFactoryRef = "mySqlSessionFactory")
public class MySqlDataSourceConfig {

    @Primary // 【关键】将 MySQL 数据源设为主数据源
    @Bean(name = "mySqlDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.mysql")
    public DataSource mySqlDataSource() {
        // 直接创建 DruidDataSource 实例，Spring Boot 会根据前缀自动注入配置
        return new DruidDataSource();
    }

    @Primary
    @Bean(name = "mySqlSessionFactory")
    public SqlSessionFactory mySqlSessionFactory(@Qualifier("mySqlDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        // 如果有针对 MySQL 的 XML 文件，可以在这里设置
        // bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/mysql/*.xml"));
        return bean.getObject();
    }

    @Primary
    @Bean(name = "mySqlTransactionManager")
    public DataSourceTransactionManager mySqlTransactionManager(@Qualifier("mySqlDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
