package org.wikipedia.analytics.eventplatform;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import org.wikipedia.analytics.eventplatform.FilterByValueController.FilteringRules;

import java.util.Collections;
import java.util.List;

public class StreamConfig {

    @SerializedName("stream") @Nullable private String streamName;

    @SerializedName("schema_title") @Nullable private String schemaTitle;

    @SerializedName("destination_event_service") @Nullable private DestinationEventService destinationEventService;

    @SerializedName("producers") @Nullable private ProducerConfig producerConfig;

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
            @NonNull ProducerConfig producerConfig,
            @Nullable DestinationEventService destinationEventService
    ) {
        this.streamName = streamName;
        this.producerConfig = producerConfig;
        this.destinationEventService = destinationEventService;
    }

    /**
     * Constructor for testing.
     *
     * In practice, filtering values will be set by Gson during deserialization using reflection.
     *
     * @param filteringRules filtering values
     */
    @VisibleForTesting StreamConfig(FilteringRules filteringRules) {
        this.producerConfig = new ProducerConfig();
        this.producerConfig.clientConfig = new ProducerConfig.MetricsPlatformClientConfig();
        this.producerConfig.clientConfig.filteringRules = filteringRules;
    }

    @NonNull public String getStreamName() {
        return StringUtils.defaultString(streamName);
    }

    @NonNull public String getSchemaTitle() {
        return StringUtils.defaultString(schemaTitle);
    }

    @NonNull public DestinationEventService getDestinationEventService() {
        return destinationEventService != null ? destinationEventService : DestinationEventService.ANALYTICS;
    }

    @NonNull public List<String> getSubscribedEvents() {
        List<String> dflt = Collections.emptyList();
        if (producerConfig == null ||
            producerConfig.clientConfig == null ||
            producerConfig.clientConfig.subscribedEvents == null) {
            return dflt;
        }
        return producerConfig.clientConfig.subscribedEvents;
    }

    @NonNull public List<String> getRequestedProducerContext() {
        List<String> dflt = Collections.emptyList();
        if (producerConfig == null ||
            producerConfig.clientConfig == null ||
            producerConfig.clientConfig.producerContext == null) {
            return dflt;
        }
        return producerConfig.clientConfig.producerContext;
    }

    @NonNull public FilteringRules getFilteringRules() {
        FilteringRules dflt = new FilteringRules();
        if (producerConfig == null ||
            producerConfig.clientConfig == null ||
            producerConfig.clientConfig.filteringRules == null) {
            return dflt;
        }
        return producerConfig.clientConfig.filteringRules;
    }

    @NonNull public SamplingConfig getSamplingConfig() {
        SamplingConfig dflt = new SamplingConfig();
        if (producerConfig == null ||
            producerConfig.clientConfig == null ||
            producerConfig.clientConfig.samplingConfig == null) {
            return null;
        }
        return producerConfig.clientConfig.samplingConfig;
    }

    static final class ProducerConfig {
        @SerializedName("metrics_platform_client") @Nullable MetricsPlatformClientConfig clientConfig;

        static final class MetricsPlatformClientConfig {
            @SerializedName("events") @Nullable private List<String> subscribedEvents;
            @SerializedName("context") @Nullable private List<String> producerContext;
            @SerializedName("sample") @Nullable private SamplingConfig samplingConfig;
            @SerializedName("filter") @Nullable private FilteringRules filteringRules;
        }
    }

}
