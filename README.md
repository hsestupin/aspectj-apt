# Aspectj + annotation processing

This project is an example of how you can combine annotation processing  with aspectj.

## Problem

This code solves a specific problem which can be described as follows: we want to have an opportunity to add callbacks to arbitrary methods invocations (before annotated method or after). For example lets consider a simple class:

```java
    public class Application {

        public void someMethod1() {
            System.out.println("someMethod1 is invoked");
        }
    }
```

What we want is to write code like this: 

```java
    Application app = new Application();
    app.OnSomeMethod1BeforeEvent.add((emmiter) -> {
        System.out.println("some before-callback for method \"someMethod1\"");
    });
```

Without Aspectj we would have to add new field per method to accumulate callbacks and moreover we need to modify the method itself. 

## Annotation processing

Firstly we mark some methods with `apt.Event` annotation from module **annotation-processors**. After that APT will generate 2 files:

1. `{$className}Callback.java` - this class contains only callbacks interfaces
2. `{$className}EventsAspect.aj` - this is aspect declaration for our class. It injects new fields to class for each annotated method for holding callbacks which will be called after/before method will be invoked respectively to annotation type:
   * `apt.Event#Order.Before`
   * `apt.Event#Order.After`

## Usage

How to run `my.Application`:
```bash
gradlew app:run -i
```

To clean all:
```bash
gradlew clean
```
