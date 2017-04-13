package net.optionfactory.propagationneverbug;

import java.io.IOException;
import java.util.Properties;
import junit.framework.Assert;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class PropagationNeverBugTest {

    private ServiceBean serviceBean;

    @Before
    public void setupAppCtx() {
        final ApplicationContext ctx = new AnnotationConfigApplicationContext(TestConfig.class);
        serviceBean = ctx.getBean(ServiceBean.class);
    }

    @Test
    public void shouldFailToObtainTransactionSynchronizedSessionWhenNoTransactionalContextIsPresent() {
        try {
            serviceBean.notAnnotated();
        } catch (HibernateException ex) {
            // expected
        }
    }

    @Test
    public void shouldFailToObtainTransactionSynchronizedSessionWhenTransactionalPropagationNeverContextIsPresent() {
        try {
            serviceBean.annotatedWithPropagationNever();
        } catch (HibernateException ex) {
            // expected
        }
    }

    @Component
    public static class ServiceBean {

        @Autowired
        public SessionFactory sessionFactory;

        public void notAnnotated() {
            sessionFactory.getCurrentSession().createNativeQuery("SELECT 1");
            Assert.fail("Should never get here: expected Hibernate Exception trying to obtain session");
        }

        @Transactional(propagation = Propagation.NEVER)
        public void annotatedWithPropagationNever() {
            sessionFactory.getCurrentSession().createNativeQuery("SELECT 1");
            Assert.fail("Should never get here: expected Hibernate Exception trying to obtain session");
        }

    }

    @EnableSpringConfigured
    @EnableTransactionManagement
    public static class TestConfig {

        @Bean
        public ServiceBean caller() {
            return new ServiceBean();
        }

        @Bean
        public PlatformTransactionManager txManager(SessionFactory sessionFactory) {
            final HibernateTransactionManager bean = new HibernateTransactionManager();
            bean.setSessionFactory(sessionFactory);
            return bean;
        }

        @Bean
        public LocalSessionFactoryBean localSessionFactoryBean() throws IOException, Exception {
            final Properties hibernateProperties = new Properties();
            hibernateProperties.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");

            final SimpleDriverDataSource datasource = new SimpleDriverDataSource();
            datasource.setUrl("jdbc:hsqldb:mem:mymemdb");
            datasource.setDriverClass(org.hsqldb.jdbcDriver.class);

            final LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
            factoryBean.setDataSource(datasource);
            factoryBean.setMappingLocations(new Resource[0]);
            factoryBean.setPackagesToScan(new String[]{});
            factoryBean.setHibernateProperties(hibernateProperties);
            return factoryBean;
        }
    }

}
