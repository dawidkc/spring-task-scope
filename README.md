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
- are local to the thread the task scope exists in

_Task context_ can be any object, from a simple `String` to any custom class instance you need.

## Purpose

Often a bean should only be available within the execution of a particular task, or only makes sense to exist within a
particular job or execution, or is dependent on arbitrary context. Let's call such service a _task-bound service_.

### Typical approaches

Especially outside IoC container like Spring, one can choose to design such services to either **(A)** consume the task
context in its methods or **(B)** keep the task context as state.

**For (A)**, _task-bound service_ can be completely stateless and can be a singleton:

```
class Service {
    /Result/ method(/TaskContext/ ctx, /actual params/)
}
```

The price to pay is that _every_ public task-bound method will take `/TaskContext/` as a parameter. This isn't a big
deal for 1-2 services. But if you have 4 or more calls on the stack, passing this additional param (including a
situation when only a dependency needs it) can be cumbersome, or can negatively impact the class design. Note that even
if a given service didn't require such context at all - but the dependency did - the method must pass the context
downstream.

**For (B)**, _task-bound service_ has a state:

```
class Service {
    /TaskContext/ ctx;
    Service(/TaskContext/ ctx) {
        this.ctx = ctx;
    }
    /Result/ method(/actual params/)
}
```

The price to pay is that in the IoC container, manual creation of such beans (through `new Service(ctx)`) requires the
user to take control of their lifecycle and dependency injection. Every time such bean is manually created, it needs to
be fed with all dependencies it requires, along with the `/TaskContext/`.

In case of **(A)**, Spring can easily manage those beans as e.g. singletons. You can `@Autowire` anything inside and it
would work. But the method params would be riddled with `/TaskContext/`, even if only to pass that param through to
dependencies. In case of **(B)**, manual creation of objects removes the benefits of using IoC container altogether.

A _task scope_ is a simple solution that allows to implement `@TaskScoped` components which can depend on such arbitrary
context, without loosing IoC features.

### Stateless vs stateful task-bound services

Often it is a choice between being stateless vs simplifying method signatures. The solution proposed here favours having
a state in such case (relying on Spring for instance management).

The reasoning is following: if a stateless bean is context-dependent, most of its public methods will contain a
parameter related to that context. This seems to effectively disconnect the state of an object and the action being
executed on that state, which seems not really in line with OOP design.

A better solution would be to bind that context to the bean instance, where the action on that context happens. It can
be as simple as providing the context parameter in the constructor, but then we stop profiting from Spring's IoC
container.

### How to use it

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

## Example use case

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