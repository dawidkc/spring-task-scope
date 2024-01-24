package io.github.dawidkc.spring.scopes;

import io.github.dawidkc.spring.scopes.aop.EnableAOPTaskScope;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SuppressWarnings("unused")
@Configuration
@EnableAOPTaskScope
@EnableAspectJAutoProxy
class TestConfiguration {

    @Bean
    @TaskScoped
    Service serviceBean(TaskScopeContext<String> context) {
        return new Service(context.getContextObject());
    }

    @AllArgsConstructor
    public static class Service {
        @Getter
        final String data;
    }

}
