package io.github.diegoquirino.veredito.domain.exporting;

/** The Appendix D columns of one item, exactly as stored ({@code null} origin or target export as empty). */
public record ExportItem(
        String system,
        String editKey,
        String uc,
        String kind,
        String field,
        String origin,
        String target,
        String originVersion,
        String targetVersion,
        String originUsecase,
        String targetUsecase) {}