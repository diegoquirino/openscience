package io.github.diegoquirino.veredito.domain.exporting;

import java.util.List;

/** The exact headers of the four export formats (spec 009 contracts and Appendix C of spec 001; org in 025). */
public final class ExportColumns {

    public static final List<String> WIDE = List.of(
            "system", "edit_key", "uc", "kind", "field", "origin", "target",
            "rater_1_name", "rater_1_email", "rater_1_accepted_research_terms_lgpd",
            "rater_1_professional_skill", "rater_1_organization", "rater_1_organization_other",
            "rater_1_change_impact_choice",
            "rater_1_maintenance_operation_choice", "rater_1_comment", "rater_1_response_language",
            "rater_2_name", "rater_2_email", "rater_2_accepted_research_terms_lgpd",
            "rater_2_professional_skill", "rater_2_organization", "rater_2_organization_other",
            "rater_2_change_impact_choice",
            "rater_2_maintenance_operation_choice", "rater_2_comment", "rater_2_response_language");

    public static final List<String> LONG = List.of(
            "system", "edit_key", "block", "role", "pseudonym", "professional_skill",
            "organization", "organization_other",
            "participant_preferred_language", "consent_language", "change_impact", "maintenance_operation",
            "comment", "response_language", "reserved_at", "submitted_at");

    public static final List<String> COMPLETO = List.of(
            "system", "edit_key", "uc", "kind", "field", "origin", "target", "origin_version", "target_version",
            "origin_usecase", "target_usecase",
            "rater_1_pseudonym", "rater_1_change_impact", "rater_1_maintenance_operation", "rater_1_comment",
            "rater_1_response_language",
            "rater_2_pseudonym", "rater_2_change_impact", "rater_2_maintenance_operation", "rater_2_comment",
            "rater_2_response_language");

    public static final List<String> RATER = List.of(
            "edit_key", "uc", "kind", "field", "origin", "target", "change_impact", "maintenance_operation",
            "comment");

    private ExportColumns() {}
}
