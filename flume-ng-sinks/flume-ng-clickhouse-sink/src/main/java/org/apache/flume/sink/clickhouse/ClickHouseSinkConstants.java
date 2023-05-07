package org.apache.flume.sink.clickhouse;

/**
 * @author xuzl
 * @version 1.0.0
 * @ClassName ClickHouseSinkConstants.java
 * @Description TODO
 * @createTime 2023-05-06 16:45
 */
public class ClickHouseSinkConstants {
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String BATCH_SIZE = "batchSize";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String DATABASE = "database";
    public static final String TABLE = "table";
    public static final String DEFAULT_PORT = "8123";
    public static final int DEFAULT_BATCH_SIZE = 10000;
    public static final String DEFAULT_USER = "";
    public static final String DEFAULT_PASSWORD = "";
    public static final String SQL_INSERT="INSERT INTO %s.%s FORMAT JSONEachRow";
    public static final String JDBC_DRIVER_CLASS="cc.blynk.clickhouse.ClickHouseDriver";
}
