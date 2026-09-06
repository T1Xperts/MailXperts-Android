package au.com.t1xperts.mailxperts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Produces bounded IMAP sequence-number ranges in newest-first order. */
final class SyncPlanner {
    static final int QUICK_BATCH_SIZE = 25;
    static final int BACKGROUND_BATCH_SIZE = 100;
    static final int FIRST_CACHE_RENDER_LIMIT = 100;
    static final int PERIODIC_BACKFILL_BUDGET = 200;
    static final int INITIAL_MESSAGE_LIMIT = 1_000;
    static final int OLDER_PAGE_SIZE = 1_000;
    static final int MAX_MESSAGE_LIMIT = 5_000;

    static final class Range {
        final int startInclusive;
        final int endInclusive;

        Range(int startInclusive, int endInclusive) {
            if (startInclusive < 1 || endInclusive < startInclusive) {
                throw new IllegalArgumentException("Invalid IMAP range");
            }
            this.startInclusive = startInclusive;
            this.endInclusive = endInclusive;
        }

        int size() {
            return endInclusive - startInclusive + 1;
        }
    }

    private SyncPlanner() {}

    static int clampRequestedLimit(int requestedLimit) {
        return Math.max(INITIAL_MESSAGE_LIMIT, Math.min(MAX_MESSAGE_LIMIT, requestedLimit));
    }

    static int nextRequestedLimit(int currentLimit) {
        int current = clampRequestedLimit(currentLimit);
        return Math.min(MAX_MESSAGE_LIMIT, current + OLDER_PAGE_SIZE);
    }

    static int limitBackfill(int requestedCount, int workBudget) {
        return Math.max(0, Math.min(Math.max(0, requestedCount), Math.max(0, workBudget)));
    }

    /** Plans at most {@code count} messages ending at the supplied IMAP sequence number. */
    static List<Range> newestFirst(int endSequence, int count, int firstBatchSize, int batchSize) {
        if (endSequence <= 0 || count <= 0) return Collections.emptyList();
        if (firstBatchSize <= 0 || batchSize <= 0) {
            throw new IllegalArgumentException("Batch sizes must be positive");
        }

        int remaining = Math.min(count, endSequence);
        int end = endSequence;
        int nextSize = Math.min(firstBatchSize, remaining);
        ArrayList<Range> ranges = new ArrayList<>();

        while (remaining > 0) {
            int size = Math.min(nextSize, remaining);
            int start = end - size + 1;
            ranges.add(new Range(start, end));
            remaining -= size;
            end = start - 1;
            nextSize = batchSize;
        }
        return ranges;
    }
}
