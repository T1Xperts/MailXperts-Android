package au.com.t1xperts.mailxperts;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilderFactory;

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Standards-based CardDAV connector used for direct Apple/iCloud contact sync. */
final class CardDavContactsClient {
    static final class Discovery {
        final String principalUrl;
        final String homeUrl;
        final String addressBookUrl;

        Discovery(String principalUrl, String homeUrl, String addressBookUrl) {
            this.principalUrl = principalUrl;
            this.homeUrl = homeUrl;
            this.addressBookUrl = addressBookUrl;
        }
    }

    private static final MediaType XML = MediaType.get("application/xml; charset=utf-8");
    private static final MediaType VCARD = MediaType.get("text/vcard; charset=utf-8");
    private final OkHttpClient http = new OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    Discovery discover(ContactSecretVault.CardDavCredential credential) throws Exception {
        requireCredential(credential);
        String root = requireHttps(credential.endpoint);
        String principalXml = propfind(root, credential, 0,
                "<d:propfind xmlns:d=\"DAV:\"><d:prop><d:current-user-principal/></d:prop></d:propfind>");
        String principalHref = firstText(parse(principalXml), "href");
        if (principalHref.isEmpty()) throw new IllegalStateException("CardDAV principal was not returned");
        String principal = resolve(root, principalHref);

        String homeXml = propfind(principal, credential, 0,
                "<d:propfind xmlns:d=\"DAV:\" xmlns:card=\"urn:ietf:params:xml:ns:carddav\">"
                        + "<d:prop><card:addressbook-home-set/></d:prop></d:propfind>");
        String homeHref = firstTextUnder(parse(homeXml), "addressbook-home-set", "href");
        if (homeHref.isEmpty()) throw new IllegalStateException("CardDAV address book home was not returned");
        String home = resolve(principal, homeHref);

        String collectionsXml = propfind(home, credential, 1,
                "<d:propfind xmlns:d=\"DAV:\"><d:prop><d:resourcetype/><d:displayname/></d:prop></d:propfind>");
        Document collections = parse(collectionsXml);
        String book = findAddressBookUrl(collections, home);
        if (book.isEmpty()) throw new IllegalStateException("No CardDAV address book collection was found");
        return new Discovery(principal, home, book);
    }

    List<CloudContactRecord> fetchAll(ContactSecretVault.CardDavCredential credential,
                                      String addressBookUrl) throws Exception {
        requireCredential(credential);
        addressBookUrl = requireHttps(addressBookUrl);
        String report = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<card:addressbook-query xmlns:d=\"DAV:\" xmlns:card=\"urn:ietf:params:xml:ns:carddav\">"
                + "<d:prop><d:getetag/><card:address-data/></d:prop>"
                + "<card:filter><card:prop-filter name=\"EMAIL\"/></card:filter>"
                + "</card:addressbook-query>";
        Request request = requestBuilder(addressBookUrl, credential)
                .header("Depth", "1")
                .method("REPORT", RequestBody.create(report, XML))
                .build();
        String xml = bodyOrThrow(http.newCall(request).execute());
        Document document = parse(xml);
        ArrayList<CloudContactRecord> out = new ArrayList<>();
        NodeList responses = elements(document, "response");
        for (int i = 0; i < responses.getLength(); i++) {
            Element response = (Element) responses.item(i);
            String href = firstText(response, "href");
            String etag = firstText(response, "getetag");
            String vcard = firstText(response, "address-data");
            if (href.isEmpty() || vcard.isEmpty()) continue;
            String remote = resolve(addressBookUrl, href);
            VCard parsed = parseVCard(vcard);
            for (String email : parsed.emails) {
                if (!email.isEmpty()) {
                    out.add(new CloudContactRecord(CloudContactStore.ICLOUD, remote, etag,
                            parsed.name, email, false));
                }
            }
        }
        return out;
    }

    CloudContactRecord upsert(ContactSecretVault.CardDavCredential credential,
                              String addressBookUrl, CloudContactRecord existing,
                              String name, String email) throws Exception {
        requireCredential(credential);
        addressBookUrl = requireHttps(addressBookUrl);
        String remote = existing != null && !existing.remoteId.isEmpty()
                ? requireHttps(existing.remoteId)
                : join(addressBookUrl, "mailxperts-" + digest(email) + ".vcf");
        Request.Builder builder = requestBuilder(remote, credential)
                .header("Content-Type", "text/vcard; charset=utf-8");
        if (existing != null && !existing.etag.isEmpty()) builder.header("If-Match", existing.etag);
        else builder.header("If-None-Match", "*");
        Request request = builder.put(RequestBody.create(vcard(name, email), VCARD)).build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("CardDAV write failed (HTTP " + response.code() + ")");
            }
            String etag = response.header("ETag", existing == null ? "" : existing.etag);
            return new CloudContactRecord(CloudContactStore.ICLOUD, remote, etag,
                    name, email, false);
        }
    }

    void delete(ContactSecretVault.CardDavCredential credential, CloudContactRecord record)
            throws Exception {
        if (record == null || record.remoteId.isEmpty()) return;
        Request.Builder builder = requestBuilder(requireHttps(record.remoteId), credential).delete();
        if (!record.etag.isEmpty()) builder.header("If-Match", record.etag);
        try (Response response = http.newCall(builder.build()).execute()) {
            if (!response.isSuccessful() && response.code() != 404) {
                throw new IllegalStateException("CardDAV delete failed (HTTP " + response.code() + ")");
            }
        }
    }

    private String propfind(String url, ContactSecretVault.CardDavCredential credential,
                            int depth, String body) throws Exception {
        Request request = requestBuilder(requireHttps(url), credential)
                .header("Depth", String.valueOf(depth))
                .method("PROPFIND", RequestBody.create(body, XML))
                .build();
        return bodyOrThrow(http.newCall(request).execute());
    }

    private Request.Builder requestBuilder(String url, ContactSecretVault.CardDavCredential credential) {
        String secure = requireHttps(url);
        return new Request.Builder().url(secure)
                .header("Authorization", Credentials.basic(credential.username, credential.appPassword))
                .header("Accept", "application/xml,text/vcard,*/*");
    }

    private static String bodyOrThrow(Response response) throws Exception {
        try (Response safe = response) {
            String body = safe.body() == null ? "" : safe.body().string();
            if (!safe.isSuccessful() && safe.code() != 207) {
                throw new IllegalStateException("CardDAV request failed (HTTP " + safe.code() + ")");
            }
            return body;
        }
    }

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(
                (xml == null ? "" : xml).getBytes(StandardCharsets.UTF_8)));
    }

    private static String findAddressBookUrl(Document document, String home) throws Exception {
        NodeList responses = elements(document, "response");
        for (int i = 0; i < responses.getLength(); i++) {
            Element response = (Element) responses.item(i);
            if (elements(response, "addressbook").getLength() == 0) continue;
            String href = firstText(response, "href");
            if (!href.isEmpty()) return resolve(home, href);
        }
        return "";
    }

    private static NodeList elements(Node node, String localName) {
        if (node instanceof Document) return ((Document) node).getElementsByTagNameNS("*", localName);
        return ((Element) node).getElementsByTagNameNS("*", localName);
    }

    private static String firstText(Document document, String localName) {
        NodeList list = elements(document, localName);
        return list.getLength() == 0 ? "" : safeText(list.item(0));
    }

    private static String firstText(Element element, String localName) {
        NodeList list = elements(element, localName);
        return list.getLength() == 0 ? "" : safeText(list.item(0));
    }

    private static String firstTextUnder(Document document, String parent, String child) {
        NodeList parents = elements(document, parent);
        if (parents.getLength() == 0 || !(parents.item(0) instanceof Element)) return "";
        return firstText((Element) parents.item(0), child);
    }

    private static String safeText(Node node) {
        return node == null || node.getTextContent() == null ? "" : node.getTextContent().trim();
    }

    private static final class VCard {
        final String name;
        final List<String> emails;
        VCard(String name, List<String> emails) {
            this.name = name;
            this.emails = emails;
        }
    }

    private static VCard parseVCard(String raw) {
        String unfolded = (raw == null ? "" : raw).replace("\r\n ", "").replace("\n ", "");
        String name = "";
        ArrayList<String> emails = new ArrayList<>();
        for (String line : unfolded.split("\\r?\\n")) {
            int colon = line.indexOf(':');
            if (colon < 0) continue;
            String key = line.substring(0, colon).toUpperCase(Locale.ROOT);
            String value = unescape(line.substring(colon + 1).trim());
            if (key.equals("FN") || key.startsWith("FN;")) name = value;
            if (key.equals("EMAIL") || key.startsWith("EMAIL;")) {
                if (!value.isEmpty()) emails.add(value);
            }
        }
        return new VCard(name, emails);
    }

    private static String vcard(String name, String email) {
        String display = escape(name == null || name.trim().isEmpty() ? email : name.trim());
        return "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:" + display
                + "\r\nEMAIL;TYPE=INTERNET:" + escape(email)
                + "\r\nUID:mailxperts-" + digest(email)
                + "\r\nREV:" + utcNow() + "\r\nEND:VCARD\r\n";
    }

    private static String utcNow() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }

    private static String escape(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\")
                .replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }

    private static String unescape(String value) {
        return value.replace("\\n", "\n").replace("\\,", ",")
                .replace("\\;", ";").replace("\\\\", "\\");
    }

    private static String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(
                    CloudContactStore.normalizeEmail(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < 12 && i < hash.length; i++) out.append(String.format(Locale.ROOT, "%02x", hash[i]));
            return out.toString();
        } catch (Exception ignored) {
            return Integer.toHexString(CloudContactStore.normalizeEmail(value).hashCode());
        }
    }

    private static String resolve(String base, String href) throws Exception {
        URL resolved = new URL(new URL(requireHttps(base)), href);
        return requireHttps(resolved.toString());
    }

    private static String join(String base, String child) {
        String secure = requireHttps(base);
        return secure.endsWith("/") ? secure + child : secure + "/" + child;
    }

    static String requireHttps(String url) {
        String value = url == null ? "" : url.trim();
        if (!value.regionMatches(true, 0, "https://", 0, 8)) {
            throw new IllegalArgumentException("CardDAV URL must use HTTPS");
        }
        return value;
    }

    private static void requireCredential(ContactSecretVault.CardDavCredential credential) {
        if (credential == null || !credential.configured()) {
            throw new IllegalStateException("CardDAV is not configured");
        }
    }
}
