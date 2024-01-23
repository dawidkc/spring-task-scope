package io.github.dawidkc.spring.scopes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * Scope annotation for task scope. Marks a given Spring Bean as task-bound. Such service is resolvable only when a task
 * scope is active.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Scope(value = TaskScope.TASK_SCOPE_NAME, proxyMode = ScopedProxyMode.TARGET_CLASS)
public @interface TaskScoped {
}
