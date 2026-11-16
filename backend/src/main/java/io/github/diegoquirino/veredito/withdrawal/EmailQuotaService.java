package io.github.diegoquirino.veredito.withdrawal;

import io.github.diegoquirino.veredito.domain.access.SendLimits;
import io.github.diegoquirino.veredito.domain.dashboard.EmailQuota;
import io.github.diegoquirino.veredito.persistence.AccessCodeRepository;
import io.github.diegoquirino.veredito.persistence.ConfigRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Rolling 24 h e-mail quota: access codes plus withdrawal confirmation sends that actually went out
 * (spec 011 research section 6).
 */
@Service
public class EmailQuotaService {

    private final AccessCodeRepository codes;
    private final WithdrawalQuery withdrawals;
    private final ConfigRepository config;
    private final Clock clock;

    public EmailQuotaService(
            AccessCodeRepository codes, WithdrawalQuery withdrawals, ConfigRepository config, Clock clock) {
        this.codes = codes;
        this.withdrawals = withdrawals;
        this.config = config;
        this.clock = clock;
    }

    public EmailQuota current() {
        Instant since = clock.instant().minus(SendLimits.DAY);
        long sent = codes.countAllSince(since) + withdrawals.countEmailSentSince(since);
        return EmailQuota.of(sent, config.dailyEmailLimit());
    }

    public boolean canSend() {
        return current().emailsRemaining() > 0;
    }
}
