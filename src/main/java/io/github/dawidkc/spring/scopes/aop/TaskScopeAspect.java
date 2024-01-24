package io.github.dawidkc.spring.scopes.aop;

import java.lang.annotation.Annotation;

import io.github.dawidkc.spring.scopes.TaskScope;
import io.github.dawidkc.spring.scopes.TaskScopeContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.SoftException;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * Aspect declaration, configuring the use of {@code @TaskContext}.
 *
 * @author dawidkc
 */
@Slf4j
@Aspect
class TaskScopeAspect {

    @Around("execution(* *(.., @io.github.dawidkc.spring.scopes.aop.TaskContext (*), ..))")
    public Object wrapInTask(final ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        log.debug("Applying task scope to {}", proceedingJoinPoint.getSignature());
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        String methodName = signature.getMethod().getName();
        Class<?>[] parameterTypes = signature.getMethod().getParameterTypes();
        Annotation[][] annotations;
        try {
            annotations = proceedingJoinPoint.getTarget().getClass()
                    .getDeclaredMethod(methodName, parameterTypes).getParameterAnnotations();
        } catch (Exception e) {
            throw new SoftException(e);
        }
        SupplierThrowingAnything<Object> delegate = buildMethodInvocationWithContext(proceedingJoinPoint, annotations);
        return delegate.get();
    }

    private SupplierThrowingAnything<Object> buildMethodInvocationWithContext(
            final ProceedingJoinPoint proceedingJoinPoint,
            final Annotation[][] annotations
    ) {
        Object[] args = proceedingJoinPoint.getArgs();
        SupplierThrowingAnything<Object> delegate = proceedingJoinPoint::proceed;
        for (int i = args.length - 1; i >= 0; i--) {
            Object arg = args[i];
            for (Annotation annotation : annotations[i]) {
                if (annotation.annotationType() == TaskContext.class) {
                    log.debug("Applying task scope to arg #{}", i);
                    delegate = wrap(delegate, arg);
                }
            }
        }
        return delegate;
    }

    private static <T> SupplierThrowingAnything<T> wrap(
            final SupplierThrowingAnything<T> supplier,
            final Object ctx
    ) {
        return () -> {
            try (TaskScopeContext<Object> context = TaskScope.create(ctx)) {
                return supplier.get();
            }
        };
    }

    @FunctionalInterface
    private interface SupplierThrowingAnything<T> {
        T get() throws Throwable;
    }
}
