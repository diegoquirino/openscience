package io.github.diegoquirino.veredito.persistence;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Answers whether the database responds to a validation query within two seconds. */
@Component
public class DatabaseStatus implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStatus.class);
    private static final int TIMEOUT_SECONDS = 2;
    private static final Duration STACK_TRACE_INTERVAL = Duration.ofMinutes(1);

    private final JdbcClient jdbc;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicReference<Instant> lastStackTrace = new AtomicReference<>(Instant.EPOCH);

    public DatabaseStatus(JdbcTemplate jdbcTemplate) {
        JdbcTemplate timed = new JdbcTemplate(jdbcTemplate.getDataSource());
        timed.setQueryTimeout(TIMEOUT_SECONDS);
        this.jdbc = JdbcClient.create(timed);
    }

    /** The wait is bounded here too, because borrowing a connection from the pool has its own, longer timeout. */
    public boolean isAvailable() {
        CompletableFuture<Integer> probe = CompletableFuture.supplyAsync(
                () -> jdbc.sql("SELECT 1").query(Integer.class).single(), executor);
        try {
            probe.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (TimeoutException ex) {
            probe.cancel(true);
            logFailure(ex);
            return false;
        } catch (ExecutionException ex) {
            logFailure(ex.getCause());
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void logFailure(Throwable ex) {
        Instant now = Instant.now();
        Instant last = lastStackTrace.get();
        if (Duration.between(last, now).compareTo(STACK_TRACE_INTERVAL) >= 0 && lastStackTrace.compareAndSet(last, now)) {
            log.warn("Database unavailable", ex);
        } else {
            log.warn("Database unavailable: {}", ex.toString());
        }
    }

    @Override
    public void destroy() {
        executor.shutdownNow();
    }
}
