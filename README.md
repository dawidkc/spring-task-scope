# Task Scope for Spring

[![Build](https://github.com/dawidkc/spring-task-scope/actions/workflows/maven.yml/badge.svg)](https://github.com/dawidkc/spring-task-scope/actions/workflows/maven.yml)
[![GitHub License](https://img.shields.io/github/license/dawidkc/spring-task-scope)](LICENSE.md)
[![GitHub Release](https://img.shields.io/github/v/release/dawidkc/spring-task-scope)](https://github.com/dawidkc/spring-task-scope/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.dawidkc.spring/spring-task-scope)](https://central.sonatype.com/artifact/io.github.dawidkc.spring/spring-task-scope)
[![Project Page](https://img.shields.io/badge/Project%20Page-GitHub%20Pages-violet)](https://dawidkc.github.io/spring-task-scope/)
[![JavaDoc](https://img.shields.io/badge/JavaDoc-GitHub%20Pages-violet)](https://dawidkc.github.io/spring-task-scope/apidocs/index.html)

A (very simple) implementation of a simple task scope for Spring 5+. Requires Java 8+.

## What is a task scope?

It is a custom Spring _bean scope_ (like singleton, prototype, or request) which is started and stopped arbitrarily,
with any object serving as a _task context_.

_Task-scoped beans_:

- exist only for the duration of the scope
- have access (through injection) to _task context_

_Task context_ can any object, from a simple `String` to any custom class instance you need.

## Purpose

Often a bean should only be available within the execution of a particular task, or only makes sense to exist within a
particular job or execution, or is dependent on arbitrary context. A task scope is a simple solution that allows to
implement `@TaskScoped` components which can depend on such arbitrary context.

Often it is a choice between being stateless vs simplifying method signatures. The solution proposed here favours having
a state in such case (relying on Spring for instance management). The reasoning is following: if a stateless bean is
context-dependent, most of its public methods will contain a parameter related to that context. A better solution would
be to bind that context to the bean instance. It can be as simple as providing the context parameter in the constructor,
but then we stop profiting from Spring's IoC container.

Task Scope allows associating Spring bean instance with an arbitrary context which is created elsewhere. So one can
create (possibly nested) task scope like so:

```
void method() {
    // ...
    // task scope is created
    try (final TaskScopeContext<Task> ctx = TaskScope.create(/* any object */)) {
        // (any @TaskScoped bean is resolved here)
        // ...
    } // task scope is closed
    // ...
}
```

Alternatively, one can bind task scope to the execution of a particular method with AOP:

```
void method(@TaskContext Object ctx) {
    // ...
    // task scope is active for the duration of method
    // ...
}
```

Any `@TaskScoped` bean can inject `TaskScopeContext<T>` to extract the associated value:

```
@TaskScoped
@Component
public class Service {

    private final Task task;

    @Autowired
    public Service(TaskScopeContext<Task> taskContext) {
        this.task = taskContext.getContextObject();
    }

    public void work() {
        // ...
    }
}
```

(This is the preferred way to define such bean, although nothing prevents you from just keeping
the `TaskScopeContext<Task>` reference in bean's field.)

Please see details in [Usage](docs/usage.md).

### Example use case

Imagine a `Service` which starts processing of an arbitrary task (say, based on changing disk content, received JMS
message, etc.). This service transfers processing to `SubService1` which may invoke further services, like so:

```
Service     SubService1      SubService2    ...    SubServiceN
   |             .               .                       .
   |             .               .                       .
 (task  ---A---->x               .                       .
created)         |               .                       .
   |             |               .                       .
   |             +-------B------>x                       .
   |             |               |                       .
   |             |               +----C---         ----->x
   |             |               |          (...)        |
   |             |               |<-------         ------x
   |             |               |
   |             |<--------------+
   |             |
   |             |
   |             |
 (task  <--------x
finished)
   |
   |
   x
```

If we want to have everything stateless, it is very much possible that task-related context object will need to be
passed down with every invoked method, which isn't really ideal since it often just pollutes method signatures.

If you imagine additional situation where one of the above `SubServiceX` does not require knowledge about the task
context, but a subsequent one (`SubService(X+1)`) does, that means that we'd probably pass such context object
through `SubServiceX` for the sole purpose of transferring the parameter downstream.

To avoid passing some kind of task-related parameter downstream in all invoked methods `A`, `B`, `C`, one could decide
to instead introduce a state in those `SubServiceX` classes, which do require knowledge about the task context. Beans of
those classes would be bound to the existence of the task. That would allow the removal of passing the parameter
explicitly when invoking subsequent services, instead relying on injected state.

Originally beans would look like this:

```
@Component
public class SubService1 {

    @Autowired
    SubService2 subService2;

    void work(Task task) {
        // some processing
        subService2.work(task);
    }

}
```

(Note that it's not even clear that `SubService1` even requires `Task` object here but if `SubService2` (or anything
further downstream) does, this parameter gets passed along.)

We can modify bean definition to something like that:

```
@TaskScoped
@Component
public class SubService1 {

    @Autowired
    SubService2 subService2;

    @Autowired
    TaskScopeContext<Task> taskContext;

    void work() {
        final Task task = taskContext.getContextObject();
        // some processing
        subService2.work();
    }

}
```

(Note that now if a service down the line does not require task object, it could be a regular singleton bean - only
services requiring `Task` instance need to be `@TaskScoped` and inject `TaskScopeContext<Task>`.)

And the task is maintained through a `try-with-resources` block:

```
@Component
public class Service {

    @Autowired
    SubService1 subService1;

    void work() {
        // task not active
        try (final TaskScopeContext<Task> ctx = TaskScope.create(new Task(/* data */))) {
            // task active
            subService1.work();
        }
        // task not active
    }

}
```

Obviously, the solution is much more relevant in more complex environments.

## Usage

Please see details in [Usage](docs/usage.md) or directly
in [JavaDoc](https://dawidkc.github.io/spring-task-scope/apidocs/index.html).