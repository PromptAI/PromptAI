package com.zervice.kbase.queues.atomic.operations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation to define an atomic task
 *
 * <pre>
 *     @AtomicOperation("mcOpName")
 *     public class FooOp {
 *         ... ...
 *     }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface AtomicOperation {
    String value();
}
