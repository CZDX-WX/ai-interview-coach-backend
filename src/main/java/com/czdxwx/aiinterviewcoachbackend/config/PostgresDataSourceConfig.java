package com.czdxwx.aiinterviewcoachbackend.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.czdxwx.aiinterviewcoachbackend.config.mybatis.PGvectorTypeHandler;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@Configuration
// 【关键】扫描专门存放 PostgreSQL 相关 Mapper 的包
@MapperScan(basePackages = "com.czdxwx.aiinterviewcoachbackend.mapper.postgres", sqlSessionFactoryRef = "postgresSessionFactory")
public class PostgresDataSourceConfig {

    @Bean(name = "postgresDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.postgres")
    public DataSource postgresDataSource() {
        return new DruidDataSource();
    }

    @Bean(name = "postgresSessionFactory")
    public SqlSessionFactory postgresSessionFactory(@Qualifier("postgresDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);

        // 【关键】为 PostgreSQL 的会话工厂注册我们自定义的 PGvector 类型处理器
        bean.setTypeHandlers(new TypeHandler[]{new PGvectorTypeHandler()});

        // 如果有针对 PostgreSQL 的 XML 文件，可以在这里设置
        // bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/postgres/*.xml"));
        return bean.getObject();
    }

    @Bean(name = "postgresTransactionManager")
    public DataSourceTransactionManager postgresTransactionManager(@Qualifier("postgresDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}