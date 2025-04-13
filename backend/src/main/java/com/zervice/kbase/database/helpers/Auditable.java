package com.zervice.kbase.database.helpers;

import java.lang.annotation.*;

/**
 * Use this annotation on Dao classes, so changes on corresponding table would generate
 * an audit log in AuditLog table
 *
 * Access =
 *   C - create
 *   R - read
 *   U - update
 *   D - delete
 */
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    boolean disabled() default false;

    /**
     * The name of the key column, default to id
     */
    String key() default "id";

    /**
     * If we need to audit read access, default to false
     */
    boolean read() default false;

    /**
     * If we need to audit create access, default to true
     */
    boolean create() default true;

    /**
     * If we need to audit update access, default to true
     */
    boolean update() default true;

    /**
     * If we need to audit delete access, default to true
     */
    boolean delete() default true;
}
