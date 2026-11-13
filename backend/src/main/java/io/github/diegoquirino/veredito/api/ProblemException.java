package io.github.diegoquirino.veredito.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;

/** An expected refusal; {@link ProblemDetailsAdvice} answers it with the localized Problem Details. */
public class ProblemException extends RuntimeException {

    private final ProblemType type;
    private final Object[] detailArgs;
    private final Map<String, Object> properties = new LinkedHashMap<>();
    private final HttpHeaders headers = new HttpHeaders();

    public ProblemException(ProblemType type, Object... detailArgs) {
        super(type.slug(), null, false, false);
        this.type = type;
        this.detailArgs = detailArgs.clone();
    }

    /** {@code invalid-request} with one {@code errors} entry per field, in the given order. */
    public static ProblemException invalidFields(Map<String, String> codeByField) {
        List<Map<String, String>> errors = codeByField.entrySet().stream()
                .map(error -> Map.of("field", error.getKey(), "code", error.getValue()))
                .toList();
        return new ProblemException(ProblemType.INVALID_REQUEST).with("errors", errors);
    }

    /** Adds an extension member to the body. */
    public ProblemException with(String name, Object value) {
        properties.put(name, value);
        return this;
    }

    public ProblemException header(String name, String value) {
        headers.set(name, value);
        return this;
    }

    public ProblemType type() {
        return type;
    }

    public Object[] detailArgs() {
        return detailArgs.clone();
    }

    public Map<String, Object> properties() {
        return Collections.unmodifiableMap(properties);
    }

    public HttpHeaders headers() {
        HttpHeaders copy = new HttpHeaders();
        copy.addAll(headers);
        return copy;
    }
}
