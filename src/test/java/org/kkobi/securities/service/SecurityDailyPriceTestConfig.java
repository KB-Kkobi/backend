package org.kkobi.securities.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.kkobi.config.RedisConfig;
import org.kkobi.external.kis.service.StockQuoteService;
import org.kkobi.securities.mapper.SecurityDailyPriceMapper;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@PropertySource("classpath:/application.properties")
@ComponentScan(basePackages = {
        "org.kkobi.external.kis.config",
        "org.kkobi.external.kis.auth",
        "org.kkobi.external.kis.client",
        "org.kkobi.external.kis.service",
})
@MapperScan(basePackages = {
        "org.kkobi.securities.mapper",
})
@Import(RedisConfig.class)
public class SecurityDailyPriceTestConfig {

    @Value("${jdbc.driver}") String driver;
    @Value("${jdbc.url}") String url;
    @Value("${jdbc.username}") String username;
    @Value("${jdbc.password}") String password;

    @Autowired
    ApplicationContext applicationContext;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driver);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        return new HikariDataSource(config);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setConfigLocation(applicationContext.getResource("classpath:/mybatis-config.xml"));
        bean.setDataSource(dataSource());
        return (SqlSessionFactory) bean.getObject();
    }

    @Bean
    public DataSourceTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public SecurityDailyPriceService securityDailyPriceService(
            SecurityDailyPriceMapper mapper, StockQuoteService stockQuoteService) {
        return new SecurityDailyPriceService(mapper, stockQuoteService);
    }
}
