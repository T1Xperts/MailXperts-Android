package au.com.t1xperts.mailxperts;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AttachmentRefTest {
    @Test public void roundTripsAttachmentMetadata() {
        List<AttachmentRef> source = Arrays.asList(
                new AttachmentRef("/tmp/a file.pdf", "Invoice | September.pdf", "application/pdf", 12345L),
                new AttachmentRef("/tmp/photo.jpg", "photo.jpg", "image/jpeg", 9876L));
        List<AttachmentRef> decoded = AttachmentRef.decode(AttachmentRef.encode(source));
        assertEquals(2, decoded.size());
        assertEquals(source.get(0).path, decoded.get(0).path);
        assertEquals(source.get(0).name, decoded.get(0).name);
        assertEquals(source.get(0).mimeType, decoded.get(0).mimeType);
        assertEquals(source.get(0).size, decoded.get(0).size);
    }
}
