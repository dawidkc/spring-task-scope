package io.github.dawidkc.spring.scopes;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.dawidkc.spring.scopes.aop.TaskContext;
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
        TaskScopeAOPTest.Service.class
})
@TestPropertySource(properties = {
        "debug=true",
})
class TaskScopeAOPTest {

    @Autowired
    Service service;

    @Autowired
    TaskScopeContext<TestContext> context;

    @Test
    void should_create_new_task_scope_for_first_annotated_param() {
        // WHEN invoking method with 1st arg being the context
        String result = service.method1("one", "two", "three");
        // THEN returned context should match 1st arg
        assertThat(result).isEqualTo("one");
    }

    @Test
    void should_create_new_task_scope_for_middle_annotated_param() {
        // WHEN invoking method with 2nd arg being the context
        String result = service.method1b("one", "two", "three");
        // THEN returned context should match 2nd arg
        assertThat(result).isEqualTo("two");
    }

    @Test
    void should_create_new_task_scope_for_last_annotated_param() {
        // WHEN invoking method with 3rd arg being the context
        String result = service.method1c("one", "two", "three");
        // THEN returned context should match 3rd arg
        assertThat(result).isEqualTo("three");
    }

    @Test
    void should_create_new_task_scope_for_all_annotated_params() {
        // WHEN invoking method with each arg being the context
        String result = service.method1d("one", "two", "three");
        // THEN returned context should match the last arg
        assertThat(result).isEqualTo("three");
    }

    @Value(staticConstructor = "of")
    static class TestContext {
        String data;
    }

    @SuppressWarnings("unused")
    @Component
    static class Service {

        String method1(@TaskContext String s1, String s2, String s3) {
            return TaskScope.getCurrentContextObject();
        }

        String method1b(String s1, @TaskContext String s2, String s3) {
            return TaskScope.getCurrentContextObject();
        }

        String method1c(String s1, String s2, @TaskContext String s3) {
            return TaskScope.getCurrentContextObject();
        }

        String method1d(@TaskContext String s1, @TaskContext String s2, @TaskContext String s3) {
            return TaskScope.getCurrentContextObject();
        }

        String method2(@TaskContext String s1) {
            return TaskScope.getCurrentContextObject();
        }

        String method3(@TaskContext TestContext context1) {
            TestContext ctx = TaskScope.getCurrentContextObject();
            return ctx.getData();
        }

    }

}