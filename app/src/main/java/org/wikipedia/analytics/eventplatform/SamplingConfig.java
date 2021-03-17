package org.wikipedia.analytics.eventplatform;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the sampling config component of a stream configuration.
 */
class SamplingConfig {

    enum Identifier {
        @SerializedName("pageview") PAGEVIEW,
        @SerializedName("session") SESSION,
        @SerializedName("device") DEVICE
    }

    private double rate = 1.0;
    @NonNull private Identifier identifier = Identifier.SESSION;

    SamplingConfig() { }

    @VisibleForTesting SamplingConfig(double rate, @Nullable Identifier identifier) {
        this.rate = rate;
        this.identifier = identifier;
    }

    public double getRate() {
        return rate;
    }

    @NonNull public Identifier getIdentifier() {
        return identifier;
    }

}
