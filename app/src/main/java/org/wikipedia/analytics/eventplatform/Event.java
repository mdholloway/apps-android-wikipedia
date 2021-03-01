package org.wikipedia.analytics.eventplatform;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

import static org.wikipedia.util.DateUtil.iso8601DateFormat;

/**
 * Base class for an Event Platform event.
 *
 * TODO: Add fields whose presence or absence is to be managed by stream configuration.
 */
public class Event {
    @SerializedName("$schema") @Nullable private String schema;
    @Nullable private Meta meta;
    @NonNull private String dt;
    @SerializedName("app_session_id") @Nullable private String sessionId;
    @SerializedName("app_install_id") @Nullable private String appInstallId;

    public Event() {
        this.dt = iso8601DateFormat(new Date());
    }

    public void setSchema(@NonNull String schema) {
        this.schema = schema;
    }

    @Nullable public String getStream() {
        if (this.meta == null) {
            return null;
        }
        return this.meta.stream;
    }

    public void setStream(@NonNull String stream) {
        if (this.meta == null) {
            this.meta = new Meta();
        }
        this.meta.stream = stream;
    }

    @Nullable public String getDomain() {
        if (this.meta == null) {
            return null;
        }
        return this.meta.domain;
    }

    public void setDomain(@NonNull String domain) {
        if (this.meta == null) {
            this.meta = new Meta();
        }
        this.meta.domain = domain;
    }

    public void setSessionId(@NonNull String sessionId) {
        this.sessionId = sessionId;
    }

    public void setAppInstallId(@Nullable String appInstallId) {
        this.appInstallId = appInstallId;
    }

    private static final class Meta {
        @Nullable private String stream;
        @Nullable private String domain;
    }
}
