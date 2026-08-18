package org.kkobi.assessment.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.kkobi.assessment.calculator.AssetRatioCalculator;
import org.kkobi.assessment.calculator.BehaviorRuleEngine;
import org.kkobi.assessment.calculator.MarketStateCalculator;
import org.kkobi.assessment.calculator.PersonaClassifier;
import org.kkobi.assessment.calculator.SecurityPositionCalculator;
import org.kkobi.assessment.calculator.VirtualInvestmentFollowUpCalculator;
import org.kkobi.assessment.calculator.VirtualInvestmentPeriodCalculator;
import org.kkobi.assessment.calculator.VirtualInvestmentScoreCalculator;
import org.kkobi.assessment.service.AssessmentResultService;
import org.kkobi.assessment.service.AssessmentSettlementService;
import org.kkobi.assessment.service.VirtualInvestmentPeriodAssessmentService;
import org.kkobi.assessment.service.VirtualInvestmentPeriodResultService;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@PropertySource("classpath:/application.properties")
@MapperScan("org.kkobi.assessment.mapper")
@Import({
        AssetRatioCalculator.class,
        BehaviorRuleEngine.class,
        MarketStateCalculator.class,
        PersonaClassifier.class,
        SecurityPositionCalculator.class,
        VirtualInvestmentFollowUpCalculator.class,
        VirtualInvestmentPeriodCalculator.class,
        VirtualInvestmentScoreCalculator.class,
        AssessmentResultService.class,
        AssessmentSettlementService.class,
        VirtualInvestmentPeriodAssessmentService.class,
        VirtualInvestmentPeriodResultService.class
})
public class AssessmentDatabaseTestConfig {

    @Value("${jdbc.driver}")
    private String driver;

    @Value("${jdbc.url}")
    private String url;

    @Value("${jdbc.username}")
    private String username;

    @Value("${jdbc.password}")
    private String password;

    @Autowired
    private ApplicationContext applicationContext;

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
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setConfigLocation(applicationContext.getResource("classpath:/mybatis-config.xml"));
        factory.setDataSource(dataSource);
        return factory.getObject();
    }

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
