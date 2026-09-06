package au.com.t1xperts.mailxperts;

import android.app.job.JobParameters;
import android.app.job.JobService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

/** Runs periodic IMAP work outside the UI process lifecycle. */
public class MailSyncJobService extends JobService {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "mailxperts-periodic-sync");
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    });

    private final ConcurrentHashMap<Integer, RunningJob> running = new ConcurrentHashMap<>();

    @Override public boolean onStartJob(JobParameters parameters) {
        String accountId = parameters.getExtras().getString("account_id", "");
        if (accountId.isEmpty()) return false;
        MailRepository.SyncToken token = new MailRepository.SyncToken();
        AtomicBoolean stopped = new AtomicBoolean(false);
        FutureTask<Void> future = new FutureTask<>(() -> {
            boolean retry = false;
            try {
                BackgroundMailSync.run(getApplicationContext(), accountId, token);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                retry = true;
            } catch (Exception ignored) {
                retry = true;
            } finally {
                running.remove(parameters.getJobId());
                if (!stopped.get()) jobFinished(parameters, retry);
            }
            return null;
        });
        running.put(parameters.getJobId(), new RunningJob(token, future, stopped));
        EXECUTOR.execute(future);
        return true;
    }

    @Override public boolean onStopJob(JobParameters parameters) {
        RunningJob job = running.remove(parameters.getJobId());
        if (job != null) {
            job.stopped.set(true);
            job.token.cancel();
            job.future.cancel(true);
        }
        return true;
    }

    private static final class RunningJob {
        final MailRepository.SyncToken token;
        final Future<?> future;
        final AtomicBoolean stopped;

        RunningJob(MailRepository.SyncToken token, Future<?> future, AtomicBoolean stopped) {
            this.token = token;
            this.future = future;
            this.stopped = stopped;
        }
    }
}
