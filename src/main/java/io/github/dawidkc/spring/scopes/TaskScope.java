package io.github.dawidkc.spring.scopes;

import java.util.Deque;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;

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

    private static final ThreadLocal<Deque<TaskScopeContext<?>>> CONTEXT_STACK =
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
     * @return auto-closeable {@link TaskScopeContext} object
     */
    public static <T> TaskScopeContext<T> create(final T contextObject) {
        log.debug("Creating new task scope with context {}", contextObject);
        TaskScopeContext<T> context = new TaskScopeContext<>(contextObject);
        CONTEXT_STACK.get().push(context);
        return context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final String name, final ObjectFactory<?> objectFactory) {
        final Map<String, Object> beans = getCurrentContext().getBeans();
        if (beans.get(name) == null) {
            beans.put(name, objectFactory.getObject());
        }
        return beans.get(name);
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

    static void delete(final TaskScopeContext<?> context) {
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
    static <T> TaskScopeContext<T> getCurrentContext() {
        if (CONTEXT_STACK.get().isEmpty()) {
            throw new NoSuchElementException("No task context available");
        }
        return (TaskScopeContext<T>) CONTEXT_STACK.get().peek();
    }

    /**
     * Returns current task-scoped context object.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getCurrentContextObject() {
        return (T) getCurrentContext().getContextObject();
    }

}
