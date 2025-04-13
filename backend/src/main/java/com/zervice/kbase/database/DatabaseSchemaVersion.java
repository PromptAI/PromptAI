package com.zervice.kbase.database;

/**
 * A constant indicating the latest schema version
 *
 * We define schema versions as an always increasing number (might not be continuous), it means so long
 * we can have a mechanism to consistently creating increasing numbers, it could be used as the way to
 * manage our database schema version.
 *
 * A possible way is to use our DEV schedule as a mechanism, for example, we can assign a number to the
 * schedule (like sprint # or other increasing number), whenever we need to upgrade DB in a slot of that
 * schedule, we set the database schema to the latest corresponding number (so we have some way to trace back
 * when DB be upgraded)
 */
public class DatabaseSchemaVersion {
    public static final int VER = 0;
}
