# Task Scope for Spring

[![Build](https://github.com/dawidkc/spring-task-scope/actions/workflows/maven.yml/badge.svg)](https://github.com/dawidkc/spring-task-scope/actions/workflows/maven.yml)
[![GitHub License](https://img.shields.io/github/license/dawidkc/spring-task-scope)](LICENSE.md)
[![GitHub Release](https://img.shields.io/github/v/release/dawidkc/spring-task-scope)](https://github.com/dawidkc/spring-task-scope/releases/latest)
[![Project Page](https://img.shields.io/badge/Project%20Page-GitHub%20Pages-violet)](https://dawidkc.github.io/spring-task-scope/)
[![JavaDoc](https://img.shields.io/badge/JavaDoc-GitHub%20Pages-violet)](https://dawidkc.github.io/spring-task-scope/apidocs/index.html)

This is a (very simple) implementation of a simple task scope for Spring 5+. Requires Java 8+.

## Purpose

Often a bean should be available within the execution of a particular task, or only makes sense to exist within a
particular job or execution, or is dependent on arbitrary context. A task scope is a simple solution that allows to
implement `@TaskScoped` components which can depend on such arbitrary context.

## Usage

Please see details in [Usage](docs/usage.md) or directly in [JavaDoc](https://dawidkc.github.io/spring-task-scope/apidocs/index.html).