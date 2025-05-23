package com.zervice.kbase.database.helpers;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface CoreDbTable {
    String after() default "";
}
