package org.wikipedia.analytics.eventplatform;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

public class StreamConfig {

    @SerializedName("stream") @Nullable private String streamName;

    @SerializedName("schema_title") @Nullable private String schemaTitle;

    @SerializedName("destination_event_service") @Nullable private DestinationEventService destinationEventService;

    @SerializedName("sampling") @Nullable private SamplingConfig samplingConfig;

    @SerializedName("producer") @Nullable private ProducerConfig producerConfig;

    /**
     * Constructor for testing.
     *
     * In practice, field values will be set by Gson during deserialization using reflection.
     *
     * @param streamName stream name
     * @param destinationEventService destination
     */
    @VisibleForTesting StreamConfig(
            @NonNull String streamName,
            @Nullable SamplingConfig samplingConfig,
            @Nullable DestinationEventService destinationEventService
    ) {
        this.streamName = streamName;
        this.samplingConfig = samplingConfig;
        this.destinationEventService = destinationEventService;
    }

    /**
     * Constructor for testing.
     *
     * In practice, filtering values will be set by Gson during deserialization using reflection.
     *
     * @param filteringValues filtering values
     */
    @VisibleForTesting StreamConfig(FilterByValueController.FilteringValues filteringValues) {
        this.producerConfig = new ProducerConfig();
        this.producerConfig.filteringValues = filteringValues;
    }

    @NonNull public String getStreamName() {
        return StringUtils.defaultString(streamName);
    }

    @NonNull public String getSchemaTitle() {
        return StringUtils.defaultString(schemaTitle);
    }

    @NonNull public List<String> getSubscribedInstruments() {
        List<String> dflt = Collections.emptyList();
        if (producerConfig == null) {
            return dflt;
        }
        if (producerConfig.subscribedInstruments == null) {
            return dflt;
        }
        return producerConfig.subscribedInstruments;
    }

    @NonNull public List<String> getRequestedValues() {
        List<String> dflt = Collections.emptyList();
        if (producerConfig == null) {
            return dflt;
        }
        if (producerConfig.requestedValues == null) {
            return dflt;
        }
        return producerConfig.requestedValues;
    }

    @Nullable public FilterByValueController.FilteringValues getFilteringValues() {
        if (producerConfig == null) {
            return null;
        }
        return producerConfig.filteringValues;
    }

    @NonNull public DestinationEventService getDestinationEventService() {
        return destinationEventService != null ? destinationEventService : DestinationEventService.ANALYTICS;
    }

    @Nullable public SamplingConfig getSamplingConfig() {
        return samplingConfig;
    }

    static final class ProducerConfig {
        @SerializedName("from_instrument") @Nullable private List<String> subscribedInstruments;
        @SerializedName("provide_values") @Nullable private List<String> requestedValues;
        @SerializedName("filter_values") @Nullable private FilterByValueController.FilteringValues filteringValues;
    }

}
