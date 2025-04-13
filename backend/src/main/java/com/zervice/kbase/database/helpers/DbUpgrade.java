package com.zervice.kbase.database.helpers;

import java.lang.annotation.*;

/*
 * We define version in database schemas. When schema changes, we need to upgrade or downgrade the database
 *
 * On each Dao, we define static methods like createDb, createTable etc. to represent latest schema
 *
 * When service starts, it would initialize database
 * 1. we will call DbInitializer.createCoreDb to initialize coreDb.
 * 2. we will call createTable of all Dao with CoreDbTable Annotation.
 * 3. Optionally, we can specify dependencies using "after" parameter.
 *
 *
 * When create account, we will call DbInitializer.createAccountDb to create new account.
 * It will call createTable of all classes in db package with AccountDbTable Annotation.
 * And you can also specify dependencies by after parameter
 *
 * For upgrading, the upgrading class shall have two methods
 *    upgradeCoreDb is used to update coreDb, it will be only executed once
 *    upgrateAccountDb is used to update per account (customer) Db
 *
 * When creating a upgrade class, you need to add @DbUpgrade notation with following params
 *    ver: MANDATORY, the introducing version, e.g. 3. We define DB version as our sprint #
 *         so we can share same DB version for multiple upgrading classes
 *    ticket: MANDATORY, the ticket information introducing this upgrade. We use ticket #
 *         plus short description to make this information more meaning ful
 *    after: OPTIONAL, to define the order of upgrading among the same DB version (e.g. certain
 *         upgrade must be executed after another upgrade, etc.)
 *
 * @Migration(ver=13, ticket="#121-foo upgrade", after="#110,#120")
 * public class FozUpgrade implement CoreDbUpgrade, AccountDbUpgrade {
 *     //will execute for control db
 *     @Override
 *     public void updateControlDb(Connection conn, String dbName) {
 *         ... ...
 *     }
 *
 *     //will execute for every account
 *     @Override
 *     public void updateAccountDb(Connection conn, String dbName) {
 *         ... ...
 *     }
 * }
 */
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface DbUpgrade {
    int ver();
    String ticket();
    String after() default "";
}
