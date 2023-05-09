package org.apache.flume.sink.clickhouse;

import cc.blynk.clickhouse.ClickHouseConnectionImpl;
import cc.blynk.clickhouse.ClickHouseDataSource;
import cc.blynk.clickhouse.copy.CopyManager;
import cc.blynk.clickhouse.copy.CopyManagerFactory;
import com.google.common.base.Preconditions;
import org.apache.flume.*;
import org.apache.flume.conf.Configurable;
import org.apache.flume.instrumentation.SinkCounter;
import org.apache.flume.sink.AbstractSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.apache.flume.sink.clickhouse.ClickHouseSinkConstants.*;


public class ClickHouseSink extends AbstractSink implements Configurable {

    private static final Logger logger = LoggerFactory.getLogger(ClickHouseSink.class);
    private ClickHouseDataSource dataSource = null;
    private SinkCounter sinkCounter = null;
    private String host = null;
    private String port = null;
    private String user = null;
    private String password = null;
    private String database = null;
    private String table = null;
    private int batchSize;

    @Override
    public void configure(Context context) {
        logger.debug("------######begin configure...");
        if (sinkCounter == null) {
            sinkCounter = new SinkCounter(getName());
        }

        Preconditions.checkArgument(context.getString(HOST) != null && context.getString(HOST).length() > 0, "ClickHouse host must be specified!");
        this.host = context.getString(HOST);
        if (!this.host.startsWith("jdbc:clickhouse://")) {
            this.host = "jdbc:clickhouse://" + this.host;
        }

        Preconditions.checkArgument(context.getString(DATABASE) != null && context.getString(DATABASE).length() > 0, "ClickHouse database must be specified!");
        this.database = context.getString(DATABASE);
        Preconditions.checkArgument(context.getString(TABLE) != null && context.getString(TABLE).length() > 0, "ClickHouse table must be specified!");
        this.table = context.getString(TABLE);
        this.port = context.getString(PORT, DEFAULT_PORT);
        this.user = context.getString(USER, DEFAULT_USER);
        this.password = context.getString(PASSWORD, DEFAULT_PASSWORD);
        this.batchSize = context.getInteger(BATCH_SIZE, DEFAULT_BATCH_SIZE);
        logger.debug("------######end configure...");
    }

    @Override
    public void start() {
        logger.debug("------######begin start...");
        //String path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        //logger.debug("------######jar path:"+path);
        String jdbcUrl = String.format("%s:%s/%s", this.host, this.port, this.database);
        try{
            this.dataSource = new ClickHouseDataSource(jdbcUrl);
            logger.debug("------######getDataSource ok");
        } catch (Exception e) {
            e.printStackTrace();
        }
        sinkCounter.start();
        super.start();
        logger.debug("------######end start...");
    }


    @Override
    public void stop() {
        logger.debug("------######begin stop");
        sinkCounter.incrementConnectionClosedCount();
        sinkCounter.stop();
        super.stop();
        logger.debug("------######end stop");
    }


    @Override
    public Status process() throws EventDeliveryException {
        logger.debug("------######begin process");
        Status status = null;
        // Start transaction
        Channel ch = getChannel();
        Transaction txn = ch.getTransaction();
        txn.begin();
        try {
            int count;
            StringBuilder batch = new StringBuilder();
            for (count = 0; count < batchSize; ++count) {
                Event event = ch.take();
                if (event == null) {
                    break;
                }
                batch.append(new String(event.getBody(), StandardCharsets.UTF_8)).append("\n");
            }
            logger.debug("------######data from channel:\n"+batch.toString()+"EOF");
            if (count <= 0) {
                sinkCounter.incrementBatchEmptyCount();
                txn.commit();
                return Status.BACKOFF;
            } else if (count < batchSize) {
                sinkCounter.incrementBatchUnderflowCount();
            } else {
                sinkCounter.incrementBatchCompleteCount();
            }
            sinkCounter.addToEventDrainAttemptCount(count);
            CopyManager copyManager = CopyManagerFactory.create((ClickHouseConnectionImpl) this.dataSource.getConnection(this.user,this.password));
            logger.debug("------######copyManager ok");
            logger.debug("------######SQL:"+String.format(SQL_INSERT, this.database, this.table));
            copyManager.copyToDb(String.format(SQL_INSERT, this.database, this.table), new ByteArrayInputStream(batch.toString().getBytes(StandardCharsets.UTF_8)));
            logger.debug("------######copyTodb ok");
            sinkCounter.incrementEventDrainSuccessCount();
            status = Status.READY;
            txn.commit();
            logger.debug("------######commit ok");
        } catch (Throwable t) {
            txn.rollback();
            logger.error(t.getMessage(), t);
            status = Status.BACKOFF;
            // re-throw all Errors
            if (t instanceof Error) {
                throw (Error) t;
            }
        } finally {
            txn.close();
        }
        logger.debug("------######end process");
        return status;
    }
}
