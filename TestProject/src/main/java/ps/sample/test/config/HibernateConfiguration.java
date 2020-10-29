package ps.sample.test.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.cfg.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/**
 * HibernateConfiguration class to configure the hibernate ORM for application
 * @author anand nandeshwar
 * @since 06-06-2020
 * 
 */
@Configuration
public class HibernateConfiguration {

	@Autowired
	private JpaProperties jpaProperties;
	
	@Value("${spring.jpa.database-platform}")
	private String hibernateDilect;
	
	@Value("${spring.jpa.hibernate.ddl-auto}")
	private String hbm2ddl;

    @Bean
	JpaVendorAdapter jpaVendorAdapter() {
		HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
		hibernateJpaVendorAdapter.setGenerateDdl(true);
		hibernateJpaVendorAdapter.setShowSql(true);
		hibernateJpaVendorAdapter.setDatabase(Database.MYSQL);

		return hibernateJpaVendorAdapter;

	}
	
	@Bean
	LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {

		Map<String, Object> jpaPropertiesMap = new HashMap<>(this.jpaProperties.getProperties());
		jpaPropertiesMap.put(Environment.FORMAT_SQL, true);
		jpaPropertiesMap.put(Environment.SHOW_SQL, true);
		jpaPropertiesMap.put(Environment.DIALECT,  this.hibernateDilect);
		jpaPropertiesMap.put(Environment.HBM2DDL_AUTO, this.hbm2ddl);

		LocalContainerEntityManagerFactoryBean entityManager = new LocalContainerEntityManagerFactoryBean();
		entityManager.setDataSource(dataSource);
		entityManager.setPackagesToScan("ps.sample.test.entity*");
		entityManager.setJpaVendorAdapter(this.jpaVendorAdapter());
		entityManager.setJpaPropertyMap(jpaPropertiesMap);
		return entityManager;
	}
}
