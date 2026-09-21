package au.com.t1xperts.mailxperts;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InlineImageCidTest {
    @Test public void convertsEditorDataImageToCidPart() {
        String html = "<p>Logo<img src=\"data:image/png;base64,AQID\"></p>";
        InlineImageCid.Prepared prepared = InlineImageCid.prepare(html);

        assertEquals(1, prepared.images.size());
        assertTrue(prepared.html.contains("src=\"cid:mailxperts-inline-1@local\""));
        assertFalse(prepared.html.contains("data:image/png"));
        assertEquals("image/png", prepared.images.get(0).mimeType);
        assertArrayEquals(new byte[]{1, 2, 3}, prepared.images.get(0).bytes);
    }

    @Test public void preservesRemoteImages() {
        String html = "<img src=\"https://t1xperts.com.au/logo.png\">";
        InlineImageCid.Prepared prepared = InlineImageCid.prepare(html);

        assertTrue(prepared.images.isEmpty());
        assertEquals(html, prepared.html);
    }

    @Test public void malformedDataImageDoesNotCrashOrCreateMimePart() {
        String html = "<img src=\"data:image/png;base64,%%%%\">";
        InlineImageCid.Prepared prepared = InlineImageCid.prepare(html);

        assertTrue(prepared.images.isEmpty());
        assertEquals(html, prepared.html);
    }
}
