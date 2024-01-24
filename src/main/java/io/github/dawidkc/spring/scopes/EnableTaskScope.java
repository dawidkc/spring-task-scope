package io.github.dawidkc.spring.scopes;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.dawidkc.spring.scopes.aop.TaskContext;
import org.springframework.context.annotation.Import;

/**
 * Enables support for the task scope. To be used on the {@code @Configuration} classes as follows:
 *
 * <pre><code>
 *{@literal @}Configuration
 *{@literal @}EnableTaskScope
 * class TestConfiguration {
 *
 *    {@literal @}Bean
 *    {@literal @}TaskScoped
 *     Service serviceBean() {
 *         //...
 *     }
 *
 * }
 * </code></pre>
 * <p>
 * In the above example {@code Service} bean is bound to a scope of a particular task. The task can be initialized via
 * AOP (see {@link TaskContext}) or programmatically (see {@link TaskScope#create(Object)}).
 * <p>
 * A task-scoped service can then extract the contextual object associated with a given scope by invoking {@link
 * TaskScope#getCurrentContext()} or injecting {@link TaskScopeContext}. A preferred way to initialize service is to
 * provide task scope via the constructor, e.g. when task context is a plain {@code String}:
 * <p>
 * <pre><code>
 *    {@literal @}Bean
 *    {@literal @}TaskScoped
 *     Service serviceBean(TaskScope.Context<String> context) {
 *         return new Service(context.getContextObject());
 *     }
 * </code></pre>
 * <p>
 *
 * @author dawidkc
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TaskScopeConfiguration.class)
public @interface EnableTaskScope {
}
