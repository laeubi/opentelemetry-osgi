package org.eclipse.osgi.technology.incubator.opentelemetry.demo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.h2.jdbcx.JdbcDataSource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Demo component that performs periodic JDBC operations against an H2
 * in-memory database to exercise the JDBC weaver instrumentation.
 * Creates a simple table and runs INSERT, SELECT, UPDATE, and DELETE
 * queries on a schedule, producing a steady stream of JDBC spans and
 * metrics visible in Grafana.
 */
@Component(immediate = true)
public class JdbcDemoComponent {

    private static final Logger LOG = Logger.getLogger(JdbcDemoComponent.class.getName());
    private static final int INTERVAL_SECONDS = 10;
    private static final int INITIAL_DELAY_SECONDS = 20;

    private ScheduledExecutorService scheduler;
    private Connection connection;
    private final AtomicLong sequence = new AtomicLong();

    @Activate
    public void activate() {
        try {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setURL("jdbc:h2:mem:otel-demo;DB_CLOSE_DELAY=-1");
            connection = ds.getConnection();
            initializeSchema();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to initialize H2 database", e);
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "jdbc-demo");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::runDemoQueries,
                INITIAL_DELAY_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);

        LOG.info("JdbcDemoComponent activated — running JDBC demo queries every "
                + INTERVAL_SECONDS + "s");
    }

    @Deactivate
    public void deactivate() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOG.log(Level.FINE, "Error closing connection", e);
            }
        }
        LOG.info("JdbcDemoComponent deactivated");
    }

    private void initializeSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS demo_items (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        item_value DOUBLE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        }
        LOG.info("JDBC demo schema initialized");
    }

    private void runDemoQueries() {
        try {
            long id = sequence.incrementAndGet();
            insertItem(id);
            selectItems();
            updateItem(id);
            selectItemById(id);
            if (id % 3 == 0) {
                deleteItem(id);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "JDBC demo query failed: " + e.getMessage());
        }
    }

    private void insertItem(long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO demo_items (id, name, item_value) VALUES (?, ?, ?)")) {
            ps.setLong(1, id);
            ps.setString(2, "item-" + id);
            ps.setDouble(3, Math.random() * 100);
            ps.executeUpdate();
        }
    }

    private void selectItems() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) AS cnt FROM demo_items")) {
                if (rs.next()) {
                    LOG.fine("[JDBC Demo] Item count: " + rs.getInt("cnt"));
                }
            }
        }
    }

    private void selectItemById(long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, name, item_value FROM demo_items WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LOG.fine("[JDBC Demo] Found item: " + rs.getString("name"));
                }
            }
        }
    }

    private void updateItem(long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE demo_items SET item_value = ? WHERE id = ?")) {
            ps.setDouble(1, Math.random() * 100);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private void deleteItem(long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM demo_items WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
