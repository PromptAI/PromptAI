package com.zervice.kbase.rbac;

public class RBACConstants {

    /**
     * // full permissions
     */
    public static final String SYS_ADMIN = "sys_admin";
    /**
     * // mostly readonly, can manage self information
     */
    public static final String SYS_USER = "sys_user";

    /**
     * Resources
     */
    public static final String RESOURCE_USERS = "users";
    public static final String RESOURCE_RBAC_ROLES = "rbacroles";
    public static final String RESOURCE_CONFIGURATIONS = "configurations";

    /**
     * Operations
     */
    public static final String OPERATION_ADD = "create";
    public static final String OPERATION_GET = "read";
    public static final String OPERATION_UPDATE = "update";
    public static final String OPERATION_DELETE = "delete";
    public static final String OPERATION_EXPORT = "export";
    public static final String OPERATION_IMPORT = "import";


    public static final String ROLE_SITE_ADMIN = "      {\n" +
            "                  remark: \"This role is granted access and permissions to all resources\",\n" +
            "                  permissions: [\n" +
            "                      {\n" +
            "                          resource: \"/**\",\n" +
            "                          operations: [\"read\", \"create\", \"update\", \"delete\", \"import\", \"export\"]\n" +
            "                      }\n" +
            "                  ]\n" +
            "              }";

    public static final String ROLE_SITE_USER = "      {\n" +
            "                  remark: \"This role is granted access to all resources except user confidential information\",\n" +
            "                  permissions: [\n" +
            "                      {\n" +
            "                          resource: \"/**\",\n" +
            "                          operations: [\"read\"]\n" +
            "                      }\n" +
            "                  ]\n" +
            "              }";

}
