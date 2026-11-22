package io.github.diegoquirino.veredito.api.v1;

import static io.github.diegoquirino.veredito.api.ApiDocs.HAL_FORMS;
import static io.github.diegoquirino.veredito.api.ApiDocs.PROBLEM_JSON;

import io.github.diegoquirino.veredito.api.ProblemDetailsSchema;
import io.github.diegoquirino.veredito.exporting.CsvDownload;
import io.github.diegoquirino.veredito.exporting.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.hateoas.Link;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** CSV exports of the final responses (spec 009); administrator only. */
@RestController
@RequestMapping("/api/v1/exports")
@Tag(name = "Administration")
public class ExportController {

    static final String COLLECTION = "/api/v1/exports";
    static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private static final String CSV = "text/csv";

    private final ExportService exports;

    public ExportController(ExportService exports) {
        this.exports = exports;
    }

    @GetMapping(version = "v1")
    @Operation(
            summary = "List the export downloads",
            description = """
                    Lists the systems that have items and links every download: the wide and long tables for \
                    all systems and, once per system, the completo file and the two pipeline rater files. \
                    Clients follow the links and never build addresses.""")
    @ApiResponse(
            responseCode = "200",
            description = "Export index",
            content = @Content(
                    mediaType = HAL_FORMS,
                    schema = @Schema(implementation = ExportIndexModel.class),
                    examples = @ExampleObject(value = """
                            {
                              "systems": ["minitest"],
                              "_links": {
                                "self": { "href": "/api/v1/exports" },
                                "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
                                "vd:export-wide": { "href": "/api/v1/exports/wide" },
                                "vd:export-long": { "href": "/api/v1/exports/long" },
                                "vd:export-completo": { "href": "/api/v1/exports/completo?system=minitest" },
                                "vd:export-rater1": { "href": "/api/v1/exports/rater1?system=minitest" },
                                "vd:export-rater2": { "href": "/api/v1/exports/rater2?system=minitest" }
                              }
                            }""")))
    @ApiResponse(
            responseCode = "401",
            description = "Visitor",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Participant",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ExportIndexModel index() {
        List<String> systems = exports.systems();
        ExportIndexModel model = new ExportIndexModel(systems);
        model.add(Link.of(COLLECTION));
        model.add(Link.of(COLLECTION + "/wide", LinkRelations.EXPORT_WIDE.curied()));
        model.add(Link.of(COLLECTION + "/long", LinkRelations.EXPORT_LONG.curied()));
        for (String system : systems) {
            String query = "?system=" + URLEncoder.encode(system, StandardCharsets.UTF_8);
            model.add(Link.of(COLLECTION + "/completo" + query, LinkRelations.EXPORT_COMPLETO.curied()));
            model.add(Link.of(COLLECTION + "/rater1" + query, LinkRelations.EXPORT_RATER1.curied()));
            model.add(Link.of(COLLECTION + "/rater2" + query, LinkRelations.EXPORT_RATER2.curied()));
        }
        return model;
    }

    @GetMapping(path = "/wide", version = "v1")
    @Operation(
            summary = "Download the wide table",
            description = """
                    One row per item of every system, in queue order, with both raters side by side (Appendix C \
                    of spec 001): name, e-mail, consent acceptance, professional skill code, chosen codes, \
                    comment and response language. Only final responses; a role not completed leaves its \
                    cells blank. Texts starting with =, +, - or @ get a leading apostrophe.""")
    @ApiResponse(
            responseCode = "200",
            description = "veredito_wide.csv",
            content = @Content(
                    mediaType = CSV,
                    schema = @Schema(type = "string"),
                    examples = @ExampleObject(value = """
                            system,edit_key,uc,kind,field,origin,target,rater_1_name,rater_1_email,\
                            rater_1_accepted_research_terms_lgpd,rater_1_professional_skill,\
                            rater_1_organization,rater_1_organization_other,\
                            rater_1_change_impact_choice,rater_1_maintenance_operation_choice,rater_1_comment,\
                            rater_1_response_language,rater_2_name,rater_2_email,\
                            rater_2_accepted_research_terms_lgpd,rater_2_professional_skill,\
                            rater_2_organization,rater_2_organization_other,\
                            rater_2_change_impact_choice,rater_2_maintenance_operation_choice,rater_2_comment,\
                            rater_2_response_language
                            minitest,LoginTest|1|1,LoginTest,MOD,step,launches the login screen,displays the login screen,\
                            Ana Souza,ana@example.com,v1 pt-BR 2026-11-20T17:03:00+00:00,test_analyst,ufcg,,HIGH,UPDATE,,\
                            pt-BR,,,,,,,,,,
                            """)))
    @ApiResponse(
            responseCode = "401",
            description = "Visitor",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Participant",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ResponseEntity<byte[]> wide() {
        return csv(exports.wide());
    }

    @GetMapping(path = "/long", version = "v1")
    @Operation(
            summary = "Download the pseudonymized long table",
            description = """
                    One row per final response, in queue order then role, with the pseudonym, professional \
                    skill code, participant preferred language, consent language, chosen codes, comment, \
                    response language and the block reservation and submission timestamps (ISO-8601 with \
                    offset). Never carries name or e-mail. Texts starting with =, +, - or @ get a leading \
                    apostrophe.""")
    @ApiResponse(
            responseCode = "200",
            description = "veredito_long.csv",
            content = @Content(
                    mediaType = CSV,
                    schema = @Schema(type = "string"),
                    examples = @ExampleObject(value = """
                            system,edit_key,block,role,pseudonym,professional_skill,organization,organization_other,\
                            participant_preferred_language,consent_language,change_impact,maintenance_operation,\
                            comment,response_language,reserved_at,submitted_at
                            minitest,LoginTest|1|1,1,1,R-0001,test_analyst,ufcg,,pt-BR,pt-BR,HIGH,UPDATE,,pt-BR,\
                            2026-11-20T16:40:00+00:00,2026-11-20T17:03:00+00:00
                            """)))
    @ApiResponse(
            responseCode = "401",
            description = "Visitor",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Participant",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ResponseEntity<byte[]> longTable() {
        return csv(exports.longTable());
    }

    @GetMapping(path = "/completo", version = "v1")
    @Operation(
            summary = "Download the filled oracle of one system",
            description = """
                    veredito_<system>_completo.csv: one row per item of the chosen system in queue order, the \
                    eleven Appendix D columns exactly as stored and, for each role, pseudonym, chosen codes, \
                    comment and response language (blank while the role is not completed). No name or e-mail; \
                    nothing is neutralized, so the file can be fed to the pipeline.""")
    @Parameter(name = "system", description = "The item system, as listed by the export index.", required = true,
            example = "minitest")
    @ApiResponse(
            responseCode = "200",
            description = "veredito_<system>_completo.csv",
            content = @Content(
                    mediaType = CSV,
                    schema = @Schema(type = "string"),
                    examples = @ExampleObject(value = """
                            system,edit_key,uc,kind,field,origin,target,origin_version,target_version,\
                            origin_usecase,target_usecase,rater_1_pseudonym,rater_1_change_impact,\
                            rater_1_maintenance_operation,rater_1_comment,rater_1_response_language,\
                            rater_2_pseudonym,rater_2_change_impact,rater_2_maintenance_operation,\
                            rater_2_comment,rater_2_response_language
                            minitest,LoginTest|1|1,LoginTest,MOD,step,launches the login screen,\
                            displays the login screen,v1.0,v1.1,usecase A,usecase B,R-0001,HIGH,UPDATE,,pt-BR,,,,,
                            """)))
    @ApiResponse(
            responseCode = "400",
            description = "Missing system",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Visitor",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Participant",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "404",
            description = "No item has that system",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ResponseEntity<byte[]> completo(@RequestParam(required = false) String system) {
        return csv(exports.completo(system));
    }

    @GetMapping(path = "/rater1", version = "v1")
    @Operation(
            summary = "Download the pipeline file of rater 1",
            description = """
                    t1_<system>_rater1.csv for the issta_tcm package: one row per item of the chosen system in \
                    queue order with edit_key, uc, kind, field, origin, target, change_impact, \
                    maintenance_operation and comment. Label cells are blank while role 1 is not completed. \
                    No language column; no name or e-mail; nothing is neutralized.""")
    @Parameter(name = "system", description = "The item system, as listed by the export index.", required = true,
            example = "minitest")
    @ApiResponse(
            responseCode = "200",
            description = "t1_<system>_rater1.csv",
            content = @Content(
                    mediaType = CSV,
                    schema = @Schema(type = "string"),
                    examples = @ExampleObject(value = """
                            edit_key,uc,kind,field,origin,target,change_impact,maintenance_operation,comment
                            LoginTest|1|1,LoginTest,MOD,step,launches the login screen,displays the login screen,HIGH,UPDATE,
                            """)))
    @ApiResponse(
            responseCode = "400",
            description = "Missing system",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Visitor",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Participant",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "404",
            description = "No item has that system",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ResponseEntity<byte[]> rater1(@RequestParam(required = false) String system) {
        return csv(exports.rater(system, 1));
    }

    @GetMapping(path = "/rater2", version = "v1")
    @Operation(
            summary = "Download the pipeline file of rater 2",
            description = """
                    t1_<system>_rater2.csv for the issta_tcm package: same nine columns as rater 1, filled from \
                    role 2. Label cells are blank while role 2 is not completed.""")
    @Parameter(name = "system", description = "The item system, as listed by the export index.", required = true,
            example = "minitest")
    @ApiResponse(
            responseCode = "200",
            description = "t1_<system>_rater2.csv",
            content = @Content(
                    mediaType = CSV,
                    schema = @Schema(type = "string"),
                    examples = @ExampleObject(value = """
                            edit_key,uc,kind,field,origin,target,change_impact,maintenance_operation,comment
                            LoginTest|1|1,LoginTest,MOD,step,launches the login screen,displays the login screen,LOW,KEEP,
                            """)))
    @ApiResponse(
            responseCode = "400",
            description = "Missing system",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Visitor",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Participant",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    @ApiResponse(
            responseCode = "404",
            description = "No item has that system",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetailsSchema.class)))
    public ResponseEntity<byte[]> rater2(@RequestParam(required = false) String system) {
        return csv(exports.rater(system, 2));
    }

    /** The content type is preset, so any {@code Accept} receives the file. */
    private static ResponseEntity<byte[]> csv(CsvDownload file) {
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.filename()).build().toString())
                .body(file.content());
    }
}