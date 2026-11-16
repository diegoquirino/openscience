package io.github.diegoquirino.veredito.domain.withdrawal;

/**
 * Counts shown before confirmation and stored on the audit row after discard (spec 011 research
 * sections 3 and 4).
 */
public record WithdrawalImpact(long finalResponseCount, long draftResponseCount, long tasksReturnableOnDiscard) {

    public static WithdrawalImpact of(long finals, long drafts, long returnable) {
        return new WithdrawalImpact(Math.max(0, finals), Math.max(0, drafts), Math.max(0, returnable));
    }
}
