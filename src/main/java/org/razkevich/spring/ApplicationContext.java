package org.razkevich.spring;

import org.razkevich.PentahoExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Properties;

@Configuration
public class ApplicationContext {

    @Bean
    @Qualifier("standardLogger")
    public Logger standardLogger() {
        return LoggerFactory.getLogger("");
    }

    @Bean
    @Qualifier("fileOnlyLogger")
    public Logger fileOnlyLogger() {
        return LoggerFactory.getLogger("file_only");
    }

    @Bean
    public PentahoExecutor executor() throws IOException {
        return new PentahoExecutor(applicationProperties(), mappingProperties());
    }

    @Bean
    @Qualifier("mappingProperties")
    public Properties mappingProperties() throws IOException {
        return new Properties() {{
            load(this.getClass().getClassLoader().getResourceAsStream("dbmapping.properties"));
        }};
    }

    @Bean
    @Qualifier("applicationProperties")
    public Properties applicationProperties() throws IOException {
        return new Properties() {{
            load(this.getClass().getClassLoader().getResourceAsStream("application.properties"));
        }};
    }
}
