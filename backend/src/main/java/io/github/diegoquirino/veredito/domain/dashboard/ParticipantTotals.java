package io.github.diegoquirino.veredito.domain.dashboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Participant totals by status and skill, and the completed-task shares per pseudonym. */
public record ParticipantTotals(
        long eligible, long ineligible, long declinedConsent, long incomplete, Map<String, Long> bySkill) {

    /** A non-anonymized participant's onboarding columns; no name or e-mail. */
    public record Fact(
            String pseudonym,
            String preferredLanguage,
            String consentDecision,
            String screeningAnswer,
            Boolean eligible,
            String skill) {}

    /** Completed tasks of one pseudonym and its share of all completed tasks. */
    public record Contributor(String pseudonym, long completedTasks, double shareOfCompleted) {}

    /** Buckets every fact once; {@code bySkill} lists only the codes that appear, most frequent first. */
    public static ParticipantTotals of(List<Fact> facts) {
        long[] tally = new long[ParticipantBucket.values().length];
        Map<String, Long> skills = new LinkedHashMap<>();
        Map<String, Long> counted = new HashMap<>();
        for (Fact fact : facts) {
            tally[ParticipantBucket.of(fact.consentDecision(), fact.screeningAnswer(), fact.eligible()).ordinal()]++;
            if (fact.skill() != null) {
                counted.merge(fact.skill(), 1L, Long::sum);
            }
        }
        counted.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .forEach(entry -> skills.put(entry.getKey(), entry.getValue()));
        return new ParticipantTotals(
                tally[ParticipantBucket.ELIGIBLE.ordinal()],
                tally[ParticipantBucket.INELIGIBLE.ordinal()],
                tally[ParticipantBucket.DECLINED_CONSENT.ordinal()],
                tally[ParticipantBucket.INCOMPLETE.ordinal()],
                skills);
    }

    /** Orders by completed tasks descending then pseudonym; the share is {@code 0} without completed tasks. */
    public static List<Contributor> contributors(Map<String, Long> completedByPseudonym) {
        long total = completedByPseudonym.values().stream().mapToLong(Long::longValue).sum();
        List<Contributor> list = new ArrayList<>();
        completedByPseudonym.forEach((pseudonym, count) ->
                list.add(new Contributor(pseudonym, count, total == 0 ? 0.0 : (double) count / total)));
        list.sort(Comparator.comparingLong(Contributor::completedTasks).reversed()
                .thenComparing(Contributor::pseudonym));
        return List.copyOf(list);
    }
}