package io.github.dawidkc.spring.scopes.aop;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration defining task scope and providing beans.
 *
 * @author dawidkc
 */
@SuppressWarnings("unused")
@Configuration
class TaskScopeAspectConfiguration {

    /**
     * Registers aspect to process {@link TaskContext} usages.
     */
    @Bean
    TaskScopeAspect taskScopeAspect() {
        return new TaskScopeAspect();
    }

}
