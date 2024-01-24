# Usage

## Dependency

Currently, the library is available via GitHub Packages. If you're working with Maven, you need to add this to your
project's dependencies:

```
<dependency>
  <groupId>io.github.dawidkc.spring</groupId>
  <artifactId>spring-task-scope</artifactId>
  <version>@VERSION@</version>
</dependency>
```

(Please
see [Working with the Apache Maven registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
on GitHub for details.)

(As an alternative, you can also use https://jitpack.io/.)

## Configuration

To enable task scope, make sure that a `@Configuration` scanned by Spring has `@EnableTaskScope` annotation:

```
@Configuration
@EnableTaskScope
class MyConfiguration {

    @Bean
    @TaskScoped
    Service serviceBean(TaskScopeContext<String> context) {
        return new Service(context.getContextObject());
    }
    
    //...

}
```

If you want to use AOP features like `@TaskContext`, use `@EnableAOPTaskScope` along with `@EnableAspectJAutoProxy`:

```
@Configuration
@EnableAOPTaskScope
@EnableAspectJAutoProxy
class MyConfiguration {

    @Bean
    @TaskScoped
    Service serviceBean(TaskScopeContext<String> context) {
        return new Service(context.getContextObject());
    }
    
    //...

}
```

## Creating task-scoped beans

You can create task-scoped beans by annotating them with `@TaskContext`. Such beans will only be resolved when there's
an active task scope.

To obtain the contextual object associated with the task scope, you need to inject `TaskScopeContext<Task>`.

```
@TaskScoped
@Component
public class Worker {

    @Autowired
    final TaskScopeContext<Task> taskContext;
    
    public void work() {
        Task task = taskContext.getContextObject();
        // ...
    }

}
```

It's probably a little cleaner to extract the contextual object in the constructor, like so:

```
@TaskScoped
@Component
public class Worker {

    final Task task;

    @Autowired
    public Worker(TaskScopeContext<Task> taskContext) {
        this.task = taskContext.getContextObject();
    }

    public void work() {
        // ...
    }

}
```

## Activating task scope

You can activate task scope wiith a `try-with-resources` block:

```
// task scope inactive
try (final TaskScopeContext<String> ctx1 = TaskScope.create("context")) {
    // task scope active
}
// task scope inactive
```

Beans with `@TaskScoped` are resolvable only when a task scope is active. Trying to resolve a bean outside of task scope
will result in `NoSuchElementException`. Any object can be treated as contextual object (in the above example it is a
String with the value `"context"`).

## Using @TaskContext to activate task scope within a method

You can activate task scope for the duration of a particular method execution. This only works when
both `@EnableAOPTaskScope` and `@EnableAspectJAutoProxy` are present on active Spring `@Configuration`.

```
@Component
public class Service2 {

    @Autowired
    Worker worker;  // @TaskScoped bean using TaskScopeContext<Task> as context holder 

    void work(@TaskContext Task context) {
        worker.work();
    }

}
```

The task scope will be created before the `work` method is invoked and closed after the method finishes.

Note that any `@TaskScoped` beans used within a task scope must use a matching type as a context, or
a `ClassCastException` will be thrown. 