package io.github.dawidkc.spring.scopes;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration defining task scope and providing beans.
 *
 * @author dawidkc
 */
@SuppressWarnings("unused")
@Configuration
class TaskScopeConfiguration {

    /**
     * Registers the task scope.
     */
    @Bean
    static BeanFactoryPostProcessor beanFactoryPostProcessor() {
        return configurableListableBeanFactory ->
                configurableListableBeanFactory.registerScope(TaskScope.TASK_SCOPE_NAME, new TaskScope());
    }

    /**
     * Registers the task scope context. This object can only be resolved inside an active context.
     */
    @Bean
    @TaskScoped
    <T> TaskScope.Context<T> taskScopeContext() {
        return TaskScope.getCurrentContext();
    }

    /**
     * Registers aspect to process {@link TaskContext} usages.
     */
    @Bean
    TaskScopeAspect taskScopeAspect() {
        return new TaskScopeAspect();
    }

}
