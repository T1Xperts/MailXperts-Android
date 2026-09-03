package au.com.t1xperts.mailxperts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class SyncPlannerTest {
    @Test public void initialSyncPaintsFiftyThenCoversExactlyOneThousand() {
        List<SyncPlanner.Range> ranges = SyncPlanner.newestFirst(
                1_250, 1_000, SyncPlanner.QUICK_BATCH_SIZE,
                SyncPlanner.BACKGROUND_BATCH_SIZE);

        assertEquals(50, ranges.get(0).size());
        assertEquals(1_201, ranges.get(0).startInclusive);
        assertEquals(1_250, ranges.get(0).endInclusive);
        assertEquals(1_000, totalSize(ranges));
        assertEquals(251, ranges.get(ranges.size() - 1).startInclusive);
        assertContiguousNewestFirst(ranges);
    }

    @Test public void planNeverRequestsBeforeSequenceOne() {
        List<SyncPlanner.Range> ranges = SyncPlanner.newestFirst(37, 1_000, 50, 100);

        assertEquals(1, ranges.size());
        assertEquals(1, ranges.get(0).startInclusive);
        assertEquals(37, ranges.get(0).endInclusive);
    }

    @Test public void olderPagingStopsAtFiveThousand() {
        assertEquals(2_000, SyncPlanner.nextRequestedLimit(1_000));
        assertEquals(5_000, SyncPlanner.nextRequestedLimit(4_000));
        assertEquals(5_000, SyncPlanner.nextRequestedLimit(5_000));
        assertEquals(1_000, SyncPlanner.clampRequestedLimit(10));
        assertEquals(5_000, SyncPlanner.clampRequestedLimit(10_000));
    }

    @Test public void emptyMailboxProducesNoRanges() {
        assertTrue(SyncPlanner.newestFirst(0, 1_000, 50, 100).isEmpty());
        assertTrue(SyncPlanner.newestFirst(1_000, 0, 50, 100).isEmpty());
    }

    @Test public void generatedRangesAreBoundedContiguousAndExact() {
        int[] folderSizes = {1, 49, 50, 51, 99, 100, 101, 999, 1_000, 5_001, 25_000};
        int[] requestedCounts = {1, 37, 50, 51, 100, 999, 1_000, 5_000};
        for (int folderSize : folderSizes) {
            for (int requestedCount : requestedCounts) {
                List<SyncPlanner.Range> ranges = SyncPlanner.newestFirst(
                        folderSize, requestedCount, SyncPlanner.QUICK_BATCH_SIZE,
                        SyncPlanner.BACKGROUND_BATCH_SIZE);
                assertEquals(Math.min(folderSize, requestedCount), totalSize(ranges));
                assertEquals(folderSize, ranges.get(0).endInclusive);
                assertTrue(ranges.get(0).size() <= SyncPlanner.QUICK_BATCH_SIZE);
                for (int i = 1; i < ranges.size(); i++) {
                    assertTrue(ranges.get(i).size() <= SyncPlanner.BACKGROUND_BATCH_SIZE);
                }
                assertContiguousNewestFirst(ranges);
            }
        }
    }

    private static int totalSize(List<SyncPlanner.Range> ranges) {
        int result = 0;
        for (SyncPlanner.Range range : ranges) result += range.size();
        return result;
    }

    private static void assertContiguousNewestFirst(List<SyncPlanner.Range> ranges) {
        for (int i = 1; i < ranges.size(); i++) {
            assertEquals(ranges.get(i - 1).startInclusive - 1,
                    ranges.get(i).endInclusive);
        }
    }
}
