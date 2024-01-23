package io.github.dawidkc.spring.scopes;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Slf4j
@SpringJUnitConfig
@ContextConfiguration(classes = {
        TestConfiguration.class,
        TaskScopeTest.Service.class
})
@TestPropertySource(properties = {
        "debug=true",
})
class TaskScopeTest {

    @Autowired
    Service service;

    @Autowired
    TaskScope.Context<TestContext> taskScopeContext;

    @Test
    void should_result_in_error_when_no_context_available_for_task_scoped_bean() {
        // GIVEN no active task scope
        // WHEN invoking task-scoped bean
        // THEN exception is thrown
        assertThatThrownBy(() -> service.getId())
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("No task context available");
    }

    @Test
    void should_result_in_error_when_no_context_available_for_task_scope_context_bean() {
        // GIVEN no active task scope
        // WHEN getting current context
        // THEN exception is thrown
        assertThatThrownBy(() -> taskScopeContext.getContextObject())
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("No task context available");
    }

    @Test
    void should_inject_task_scoped_bean_within_context() {
        // GIVEN a task scope
        String result1, result2;
        try (TaskScope.Context<TestContext> ctx = TaskScope.create(TestContext.of("ctx"))) {
            // WHEN invoking task-scoped bean twice
            result1 = service.getId();
            result2 = service.getId();
        }
        // THEN exactly the same service is used
        assertThat(result1).isNotEmpty();
        assertThat(result2).isNotEmpty();
        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void should_inject_task_scoped_bean_within_separate_contexts() {
        // GIVEN a task scope
        String result1;
        try (TaskScope.Context<TestContext> ctx = TaskScope.create(TestContext.of("ctx"))) {
            // WHEN invoking task-scoped bean
            result1 = service.getId();
        }
        // THEN the service is not available outside of scope
        assertThat(result1).isNotEmpty();
        assertThatThrownBy(() -> service.getId())
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("No task context available");
    }

    @Test
    void should_remove_task_scoped_bean_when_context_is_closed() {
        // GIVEN a task scope
        String result1, result2;
        final String ctxObject = "ctx";
        try (TaskScope.Context<TestContext> ctx = TaskScope.create(TestContext.of(ctxObject))) {
            result1 = service.getId();
        }
        // ...AND another task scope with same context Object
        try (TaskScope.Context<TestContext> ctx = TaskScope.create(TestContext.of(ctxObject))) {
            result2 = service.getId();
        }
        // THEN two separate instances of service are created
        assertThat(result1).isNotEmpty();
        assertThat(result2).isNotEmpty();
        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void should_inject_task_scoped_bean_within_composition_of_contexts() {
        // GIVEN a task scope
        String result1, result2;
        final String ctxObject = "ctx";
        try (TaskScope.Context<TestContext> ctx1 = TaskScope.create(TestContext.of(ctxObject))) {
            result1 = service.getId();
            // ...AND another task scope with same context Object
            try (TaskScope.Context<TestContext> ctx2 = TaskScope.create(TestContext.of(ctxObject))) {
                result2 = service.getId();
            }
        }
        // THEN two separate instances of service are created
        assertThat(result1).isNotEmpty();
        assertThat(result2).isNotEmpty();
        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void should_resolve_injected_context_correctly() {
        // GIVEN a task scope
        try (TaskScope.Context<TestContext> ctx = TaskScope.create(TestContext.of("ctx"))) {
            // THEN injected TaskScope context must contain current context object
            assertThat(taskScopeContext.getContextObject()).isSameAs(ctx.getContextObject());
        }
    }

    @Test
    void should_resolve_injected_context_correctly_in_nested_scopes() {
        // GIVEN 2 nested task scopes
        // THEN appropriate context object is resolved from the injected TaskScope.Context<>
        try (TaskScope.Context<TestContext> ctx1 = TaskScope.create(TestContext.of("ctx"))) {
            assertThat(taskScopeContext.getContextObject()).isSameAs(ctx1.getContextObject());
            try (TaskScope.Context<TestContext> ctx2 = TaskScope.create(TestContext.of("ctx"))) {
                assertThat(taskScopeContext.getContextObject()).isSameAs(ctx2.getContextObject());
            }
        }
    }

    @Test
    void should_treat_identical_contexts_as_not_equal_in_nested_scopes() {
        // GIVEN 2 "identical" nested task scopes
        try (TaskScope.Context<TestContext> ctx1 = TaskScope.create(TestContext.of("ctx"))) {
            try (TaskScope.Context<TestContext> ctx2 = TaskScope.create(TestContext.of("ctx"))) {
                // THEN underlying context can be equal
                assertThat(ctx1.getContextObject()).isEqualTo(ctx2.getContextObject());
                // ...BUT actual contexts differ
                assertThat(ctx1).isNotEqualTo(ctx2);
                assertThat(ctx1).isNotSameAs(ctx2);
            }
        }
    }

    @Test
    void should_only_allow_removal_of_the_current_context() {
        // GIVEN 2 nested contexts
        final TaskScope.Context<TestContext> ctx1 = TaskScope.create(TestContext.of("ctx"));
        final TaskScope.Context<TestContext> ctx2 = TaskScope.create(TestContext.of("ctx"));
        // WHEN closing contexts in incorrect order
        // THEN exception is thrown
        assertThatThrownBy(() -> {
            ctx1.close();
        })
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only currently active context may be removed");
        ctx2.close();
        ctx1.close();
    }

    @Value(staticConstructor = "of")
    static class TestContext {
        String data;

    }

    @Component
    @TaskScoped
    static class Service {
        final String id = UUID.randomUUID().toString();

        String getId() {
            return id;
        }
    }

}
