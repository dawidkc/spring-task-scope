package io.github.dawidkc.spring.scopes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When used with {@code @EnableAspectJAutoProxy}, this annotation is used on a method parameter to create a new task
 * scope with the given parameter value as the context. The context is valid for the duration of the executed method.
 * <p>
 * For example:
 * <pre><code>
 *{@literal @}Component
 * public class Service {
 *
 *     void method(String param1,{@literal @}TaskContext String param2) {
 *         //...
 *     }
 *
 * }
 * </code></pre>
 * In the above example, {@code param2} will be the value of the task context active during the execution of the
 * method.
 *
 * @author dawidkc
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface TaskContext {
}
