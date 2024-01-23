package io.github.dawidkc.spring.scopes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SuppressWarnings("unused")
@Configuration
@EnableTaskScope
@EnableAspectJAutoProxy
class TestConfiguration {

    @Bean
    @TaskScoped
    Service serviceBean(TaskScope.Context<String> context) {
        return new Service(context.getContextObject());
    }

    @AllArgsConstructor
    public static class Service {
        @Getter
        final String data;
    }

}
