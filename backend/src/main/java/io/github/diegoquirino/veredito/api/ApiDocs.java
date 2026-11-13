package io.github.diegoquirino.veredito.api;

/** Media types and examples shared by the OpenAPI annotations of every controller. */
public final class ApiDocs {

    public static final String HAL_FORMS = "application/prs.hal-forms+json";
    public static final String PROBLEM_JSON = "application/problem+json";

    public static final String UNSUPPORTED_VERSION_EXAMPLE = """
            {
              "type": "urn:veredito:problem:unsupported-api-version",
              "title": "API version not supported",
              "status": 400,
              "detail": "API version \\"v9\\" does not exist. Available versions: v1.",
              "instance": "/api/v9",
              "supportedVersions": ["v1"]
            }""";

    public static final String NOT_FOUND_EXAMPLE = """
            {
              "type": "urn:veredito:problem:not-found",
              "title": "Resource not found",
              "status": 404,
              "detail": "Nothing exists at this address.",
              "instance": "/api/v1/rels/unknown"
            }""";

    public static final String NOT_ACCEPTABLE_EXAMPLE = """
            {
              "type": "urn:veredito:problem:not-acceptable",
              "title": "Format not available",
              "status": 406,
              "detail": "This resource cannot answer in any of the requested formats.",
              "instance": "/api/v1"
            }""";

    private ApiDocs() {}
}
