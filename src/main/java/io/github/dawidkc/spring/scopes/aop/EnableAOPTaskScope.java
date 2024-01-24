package io.github.dawidkc.spring.scopes.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.dawidkc.spring.scopes.EnableTaskScope;
import org.springframework.context.annotation.Import;

/**
 * Enables support for the task scope initiated via AOP with {@link TaskContext}.
 *
 * @author dawidkc
 * @see TaskContext
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableTaskScope
@Import(TaskScopeAspectConfiguration.class)
public @interface EnableAOPTaskScope {
}
