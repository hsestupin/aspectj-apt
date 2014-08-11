package apt;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация создающая ивент для вызова метода.
 *
 * @author pavel.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Event {

    enum Order {
        Before,
        After
    }

    @NotNull Order value() default Order.Before;

    boolean treadSafe() default false;
}