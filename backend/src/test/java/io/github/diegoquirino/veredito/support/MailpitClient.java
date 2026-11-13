package io.github.diegoquirino.veredito.support;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Reads the messages that Mailpit received, through its HTTP API. */
public class MailpitClient {

    private static final Pattern CODE = Pattern.compile("(?<!\\d)(\\d{6})(?!\\d)");
    private static final Duration ARRIVAL_TIMEOUT = Duration.ofSeconds(5);

    private final URI base;
    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    public MailpitClient(URI base) {
        this.base = base;
    }

    public record Message(String id, String subject, String text) {}

    /** Ids of the messages sent to the address, newest first. */
    public List<String> messageIdsTo(String address) {
        String query = URLEncoder.encode("to:\"" + address + "\"", StandardCharsets.UTF_8);
        JsonNode messages = get("/api/v1/search?query=" + query).path("messages");
        List<String> ids = new ArrayList<>();
        messages.forEach(message -> ids.add(message.path("ID").asString()));
        return ids;
    }

    public int countTo(String address) {
        return messageIdsTo(address).size();
    }

    /** The newest message to the address, waiting a few seconds for it to arrive. */
    public Message latestTo(String address) {
        Instant deadline = Instant.now().plus(ARRIVAL_TIMEOUT);
        while (true) {
            Optional<String> id = messageIdsTo(address).stream().findFirst();
            if (id.isPresent()) {
                JsonNode message = get("/api/v1/message/" + id.get());
                return new Message(id.get(), message.path("Subject").asString(), message.path("Text").asString());
            }
            if (Instant.now().isAfter(deadline)) {
                throw new AssertionError("No message arrived in Mailpit for the test address");
            }
            pause();
        }
    }

    /** The 6-digit code of the newest message to the address. */
    public String latestCodeTo(String address) {
        Matcher matcher = CODE.matcher(latestTo(address).text());
        if (!matcher.find()) {
            throw new AssertionError("The newest message has no 6-digit code");
        }
        return matcher.group(1);
    }

    public void clear() {
        send(HttpRequest.newBuilder(base.resolve("/api/v1/messages")).DELETE().build());
    }

    private JsonNode get(String path) {
        return json.readTree(send(HttpRequest.newBuilder(base.resolve(path)).GET().build()));
    }

    private String send(HttpRequest request) {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 300) {
                throw new AssertionError("Mailpit answered " + response.statusCode() + " to " + request.uri().getPath());
            }
            return response.body();
        } catch (IOException e) {
            throw new IllegalStateException("Mailpit is not reachable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void pause() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
