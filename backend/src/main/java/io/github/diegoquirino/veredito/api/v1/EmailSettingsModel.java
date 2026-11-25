package io.github.diegoquirino.veredito.api.v1;

import io.github.diegoquirino.veredito.persistence.InviteSettingsRepository.InviteSettings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.springframework.hateoas.RepresentationModel;

@Schema(name = "EmailSettings", description = "Invite send settings with date and time (UTC instants).")
public class EmailSettingsModel extends RepresentationModel<EmailSettingsModel> {

    private final String caae;
    private final String platformUrl;
    private final Instant collectionStartAt;
    private final Instant collectionEndAt;
    private final Instant reminderAt;

    public EmailSettingsModel(InviteSettings settings) {
        this.caae = settings.caae();
        this.platformUrl = settings.platformUrl();
        this.collectionStartAt = settings.collectionStartAt();
        this.collectionEndAt = settings.collectionEndAt();
        this.reminderAt = settings.reminderAt();
    }

    public String getCaae() {
        return caae;
    }

    public String getPlatformUrl() {
        return platformUrl;
    }

    public Instant getCollectionStartAt() {
        return collectionStartAt;
    }

    public Instant getCollectionEndAt() {
        return collectionEndAt;
    }

    public Instant getReminderAt() {
        return reminderAt;
    }
}
