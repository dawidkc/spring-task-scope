package io.github.dawidkc.spring.scopes;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Slf4j
@SpringJUnitConfig
@ContextConfiguration(classes = {
        TestConfiguration.class
})
@TestPropertySource(properties = {
        "debug=true",
})
class ConcurrentTaskScopeTest {

    @Autowired
    TestConfiguration.Service service;

    ExecutorService executor = Executors.newFixedThreadPool(5);

    @Test
    void should_allow_for_concurrent_task_scope_creations() throws InterruptedException {
        // GIVEN a task returning true/false when matching thread id
        Callable<Boolean> task = () -> {
            final String contextObject = "whatever_" + Thread.currentThread().getId();
            String actualData;
            try (TaskScopeContext<?> unused = TaskScope.create(contextObject)) {
                actualData = service.getData();
            }
            return actualData.equals(contextObject);
        };
        // WHEN invoking all tasks concurrently
        final List<Boolean> results = IntStream.range(0, 10)
                .mapToObj(value -> executor.submit(task))
                .map(result -> {
                    try {
                        return result.get(1, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
        // THEN all tasks must match
        assertThat(results)
                .isNotEmpty()
                .allMatch(Boolean::booleanValue);
    }
}
