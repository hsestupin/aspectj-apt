# Aspectj + annotation processing

This project is an example of how you can combine annotation processing  with aspectj.

# Problem

This code solves a specific problem which can be described as follows: you want to be able to add callbacks to arbitrary methods invocations (before method and after).

# Annotation processing

Firstly we mark some methods with `apt.Event` annotation from module *annotation-processors*. After that APT will generate 2 files:
1. `{$className}Callback.java` - this class contains only callbacks interfaces
2. `{$className}EventsAspect.aj` - this is aspect declaration for our class. It injects new fields to class for each annotated method for holding callbacks which will be called after/before method will be invoked respectively to annotation type:
   * `apt.Event#Order.Before`
   * `apt.Event#Order.After`

# Usage

How to run `my.Application`:
```bash
gradlew app:run -i
```

To clean all:
```bash
gradlew clean
```