package io.github.dawidkc.spring.scopes;

import java.io.Closeable;
import java.util.Deque;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

/**
 * Task scope implementation.
 *
 * @author dawidkc
 * @see TaskScope#create(Object)
 * @see org.springframework.beans.factory.config.Scope
 */
@Slf4j
public final class TaskScope implements Scope {

    public static final String TASK_SCOPE_NAME = "task";

    private static final ThreadLocal<Deque<Context<?>>> CONTEXT_STACK =
            ThreadLocal.withInitial(ConcurrentLinkedDeque::new);

    /**
     * Create a new task scope with provided object as the context. The intent is to use this static method within a
     * {@code try-with-resources} block, example:
     * <p>
     * <pre><code>
     * // task scoped beans are unresolved here
     * try (var ctx = TaskScope.create("data")) {
     *     // task scoped beans get resolved here
     * }
     * // task scoped beans are unresolved here
     * </code></pre>
     *
     * @param contextObject any object which can be considered task context
     * @return auto-closeable {@link Context} object
     */
    public static <T> Context<T> create(final T contextObject) {
        log.debug("Creating new task scope with context {}", contextObject);
        Context<T> context = new Context<>(contextObject);
        CONTEXT_STACK.get().push(context);
        return context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final String name, final ObjectFactory<?> objectFactory) {
        return getCurrentContext().getBeans().computeIfAbsent(name, s1 -> objectFactory.getObject());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object remove(final String name) {
        Runnable callback = getCurrentContext().getDestructionCallbacks().remove(name);
        if (callback != null) {
            callback.run();
        }
        return getCurrentContext().getBeans().remove(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerDestructionCallback(final String name, final Runnable runnable) {
        getCurrentContext().getDestructionCallbacks().put(name, runnable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object resolveContextualObject(final String name) {
        if ("context".equals(name)) {
            return getCurrentContext().getContextObject();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConversationId() {
        return null;
    }

    private static void delete(final Context<?> context) {
        log.debug("Attempting to remove task scope with context {}", context.getContextObject());
        if (context != getCurrentContext()) {
            throw new IllegalStateException("Only currently active context may be removed");
        }
        CONTEXT_STACK.get().pop();
        if (CONTEXT_STACK.get().isEmpty()) {
            CONTEXT_STACK.remove();
        }
        log.debug("Task scope with context {} has been removed", context.getContextObject());
    }

    @SuppressWarnings("unchecked")
    static <T> Context<T> getCurrentContext() {
        if (CONTEXT_STACK.get().isEmpty()) {
            throw new NoSuchElementException("No task context available");
        }
        return (Context<T>) CONTEXT_STACK.get().peek();
    }

    /**
     * Returns current task-scoped context object.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getCurrentContextObject() {
        return (T) getCurrentContext().getContextObject();
    }

    /**
     * Context holder object for task scope.
     *
     * @see Context#getContextObject()
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Context<T> implements Closeable {

        private static final AtomicLong COUNTER = new AtomicLong();

        @Getter(AccessLevel.PRIVATE)
        private final Map<String, Object> beans = new ConcurrentHashMap<>();

        @Getter(AccessLevel.PRIVATE)
        private final Map<String, Runnable> destructionCallbacks = new ConcurrentHashMap<>();

        /**
         * Returns the context object provided when opening scope.
         */
        @Getter
        private final T contextObject;

        /**
         * Returns the unique context ID.
         */
        @Getter
        private final long id = COUNTER.getAndIncrement();

        @Override
        public void close() {
            TaskScope.delete(this);
        }
    }

}
