package io.github.dawidkc.spring.scopes;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Context holder object for task scope.
 *
 * @see TaskScopeContext#getContextObject()
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class TaskScopeContext<T> implements Closeable {

    private static final AtomicLong COUNTER = new AtomicLong();

    @Getter(AccessLevel.PACKAGE)
    private final Map<String, Object> beans = new ConcurrentHashMap<>();

    @Getter(AccessLevel.PACKAGE)
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
