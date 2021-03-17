package org.wikipedia.analytics.eventplatform;

import androidx.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import io.reactivex.rxjava3.schedulers.Schedulers;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_GATEWAY_TIMEOUT;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static org.wikipedia.BuildConfig.META_WIKI_BASE_URI;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.DEVICE;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.PAGEVIEW;
import static org.wikipedia.analytics.eventplatform.SamplingConfig.Identifier.SESSION;
import static org.wikipedia.settings.Prefs.isEventLoggingEnabled;

public final class EventPlatformClient {

    /**
     * Stream configs to be fetched on startup and stored for the duration of the app lifecycle.
     */
    static Map<String, StreamConfig> STREAM_CONFIGS = new HashMap<>();

    /**
     * Map of instrument names to the streams to which they should produce events.
     * TODO: Transform stream configs rather than holding separate structures?
     */
    static Map<String, Set<String>> EVENTS_TO_STREAMS = new HashMap<>();

    /**
     * Map of stream names to target schema versions.
     */
    private static final HashMap<String, String> SCHEMAS = new HashMap<String, String>() {{
        put("test.instrumentation", "/analytics/test/1.0.0");
        put("android.user_contribution_screen", "/analytics/mobile_apps/android_user_contribution_screen/2.0.0");
    }};

    /**
     * A regular expression to match JavaScript regular expression literals. (How meta!)
     * This is not as strict as it could be in that it allows individual flags to be specified more
     * than once, but it doesn't really matter because we don't expect flags and ignore them if
     * present.
     */
    static String JS_REGEXP_PATTERN = "^/.*/[gimsuy]{0,6}$";

    /**
     * Fetch stream configs from the MW API and set them up for use for the duration of the
     * app lifecycle.
     */
    public static void setUpStreamConfigs() {
        ServiceFactory.get(new WikiSite(META_WIKI_BASE_URI))
                .getStreamConfigs()
                .subscribeOn(Schedulers.io())
                .subscribe(response -> updateStreamConfigs(response.getStreamConfigs()), L::e);
    }

    /**
     * Get the stream config for a given stream name.
     *
     * Stream configuration keys can take the form of either a string literal or a (JavaScript)
     * regular expression pattern. To take advantage of the performance strengths of HashMaps, we'll
     * first attempt to retrieve the stream config as a literal key name. If no match is found,
     * we'll then iterate over the keys to search for a regular expression matching the provided
     * stream name.
     *
     * Regex-formatted keys use JavaScript regular expression literal syntax, e.g.: '/foo/'.
     * We don't expect any flags, and ignore them if present.
     *
     * N.B. Since stream config keys can be given as regular expressions, it is technically
     * possible that more than one key could match the provided stream name. In this event that more
     * than one match is present, we'll return the config corresponding to the first match found.
     *
     * @param streamName stream name
     * @return the first matching stream config, or null if no match is found
     */
    static StreamConfig getStreamConfig(String streamName) {
        if (STREAM_CONFIGS.containsKey(streamName)) {
            return STREAM_CONFIGS.get(streamName);
        }

        for (String key : STREAM_CONFIGS.keySet()) {
            if (key.matches(JS_REGEXP_PATTERN)) {
                // Note: After splitting on the slash character ("/"), element [0] of the resulting
                // array will contain the empty string (""), and element [1] will contain the
                // regular expression pattern.
                // If any flags are specified, they will be present in element [2], but will be
                // ignored here.
                if (streamName.matches(key.split("/")[1])) {
                    return STREAM_CONFIGS.get(key);
                }
            }
        }

        return null;
    }

    static void setStreamConfig(StreamConfig streamConfig) {
        STREAM_CONFIGS.put(streamConfig.getStreamName(), streamConfig);
    }

    /**
     * Submit an event to be enqueued and sent to the Event Platform for each destination stream
     * that applies to the submitted event.
     *
     * @param instrument instrument name
     * @param event event data
     */
    public static synchronized void submit(String instrument, Event event) {
        if (!EVENTS_TO_STREAMS.containsKey(instrument)) {
            return;
        }

        for (String stream : EVENTS_TO_STREAMS.get(instrument)) {
            if (!SamplingController.isInSample(stream)) {
                return;
            }
            StreamConfig config = getStreamConfig(stream);
            if (!FilterByValueController.filterByValue(config, event)) {
                return;
            }
            addCoreEventMetadata(stream, event);
            addRequestedProducerContext(config, event);
            OutputBuffer.schedule(event);
        }
    }

    /**
     * Supplement the outgoing event with additional metadata, if not already present.
     * These include:
     * - dt: ISO 8601 timestamp
     * - app_session_id: the current session ID
     * - app_install_id: app install ID
     *
     * @param stream stream name
     * @param event event data
     * @return updated event data object
     * @throws RuntimeException if no schema is defined for the indicated event type
     */
    static Event addCoreEventMetadata(String stream, Event event) {
        String schema = SCHEMAS.get(stream);
        if (schema == null) {
            throw new RuntimeException("No schema is registered for stream " + stream);
        }
        event.setSchema(schema);
        event.setAppInstallId(Prefs.getAppInstallId());
        event.setSessionId(AssociationController.getSessionId());
        event.setStream(stream);

        // Default meta.domain to the hostname for the current WikiSite. If a different domain
        // actually applies to the event, it should be provided by the caller. If no domain applies,
        // the caller can set the domain to an empty string to avoid its being set by default here.
        if (event.getDomain() == null) {
            String hostname = WikipediaApp.getInstance().getWikiSite().uri().getHost();
            event.setDomain(hostname);
        }
        return event;
    }

    static Event addRequestedProducerContext(StreamConfig config, Event event) {
        // TODO: Decide on supported configurable metadata values, and add support for them.
        // https://docs.google.com/document/d/1Z_KRHWw7hNXldePxJP1YTvxJHOtKzZ3kSTo7YPkdp4k
        return event;
    }

    /**
     * OutputBuffer: buffers events in a queue prior to transmission
     *
     * Transmissions are not sent at a uniform offset but are shaped into
     * 'bursts' using a combination of queue size and debounce time.
     *
     * These concentrate requests (and hence, theoretically, radio awake state)
     * so as not to contribute to battery drain.
     */
    static class OutputBuffer {

        private static final List<Event> QUEUE = new ArrayList<>();

        /*
         * When an item is added to QUEUE, wait this many ms before sending.
         * If another item is added to QUEUE during this time, reset the countdown.
         */
        private static final int WAIT_MS = 30000;

        private static final int MAX_QUEUE_SIZE = 128;

        private static final Runnable SEND_RUNNABLE = OutputBuffer::sendAllScheduled;

        static synchronized void sendAllScheduled() {
            WikipediaApp app = WikipediaApp.getInstance();
            app.getMainThreadHandler().removeCallbacks(SEND_RUNNABLE);
            if (isEnabled()) {
                send();
                QUEUE.clear();
            }
        }

        /**
         * Schedule a request to be sent.
         *
         * @param event event data
         */
        static synchronized void schedule(Event event) {
            if (!isEnabled()) {
                return;
            }
            QUEUE.add(event);

            if (QUEUE.size() >= MAX_QUEUE_SIZE) {
                sendAllScheduled();
            } else {
                //The arrival of a new item interrupts the timer and resets the countdown.
                WikipediaApp.getInstance().getMainThreadHandler().removeCallbacks(SEND_RUNNABLE);
                WikipediaApp.getInstance().getMainThreadHandler().postDelayed(SEND_RUNNABLE, WAIT_MS);
            }
        }

        /**
         * If sending is enabled, attempt to send the provided events.
         * Also batch the events ordered by their streams, as the QUEUE
         * can contain events of different streams
         */
        private static void send() {
            Map<String, ArrayList<Event>> eventsByStream = new HashMap<>();
            for (Event event : QUEUE) {
                String stream = event.getStream();
                if (!eventsByStream.containsKey(stream) || eventsByStream.get(stream) == null) {
                    eventsByStream.put(stream, new ArrayList<>());
                }
                eventsByStream.get(stream).add(event);
            }
            for (String stream : eventsByStream.keySet()) {
                sendEventsForStream(getStreamConfig(stream), eventsByStream.get(stream));
            }
        }

        private static void sendEventsForStream(@NonNull StreamConfig streamConfig, @NonNull List<Event> events) {
            ServiceFactory.getAnalyticsRest(streamConfig).postEvents(events)
                    .subscribeOn(Schedulers.io())
                    .subscribe(response -> {
                        switch (response.code()) {
                            case HTTP_CREATED: // 201 - Success
                            case HTTP_ACCEPTED: // 202 - Hasty success
                                break;
                            // Status 207 will not be received when sending hastily.
                            // case 207: // Partial Success
                            //    TODO: Retry failed events?
                            //    L.logRemoteError(new RuntimeException(response.toString()));
                            //    break;
                            case HTTP_BAD_REQUEST: // 400 - Failure
                                L.logRemoteError(new RuntimeException(response.toString()));
                                break;
                            // Occasional server errors are unfortunately not unusual, so log the error
                            // but don't crash even on pre-production builds.
                            case HTTP_INTERNAL_ERROR: // 500
                            case HTTP_UNAVAILABLE: // 503
                            case HTTP_GATEWAY_TIMEOUT: // 504
                                L.logRemoteError(new RuntimeException(response.message()));
                                break;
                            default:
                                // Something unexpected happened. Crash if this is a pre-production build.
                                L.logRemoteErrorIfProd(
                                        new RuntimeException("Unexpected EventGate response: "
                                                + response.toString())
                                );
                                break;
                        }
                    });
        }

    }

    /**
     * AssociationController: provides associative identifiers and manage their
     * persistence
     *
     * Identifiers correspond to various scopes e.g. 'pageview', 'session', and 'device'.
     *
     * TODO: Possibly get rid of the pageview type?  Does it make sense on apps?  It is not in the iOS library currently.
     * On apps, a "session" starts when the app is loaded, and ends when completely closed, or after 15 minutes of inactivity
     * Save a ts when going into bg, then when returning to foreground, & if it's been more than 15 mins, start a new session, else continue session from before
     * Possible to query/track time since last interaction? (For future)
     */
    static class AssociationController {
        private static String PAGEVIEW_ID = null;
        private static String SESSION_ID = null;

        /**
         * Generate a pageview identifier.
         *
         * @return pageview ID
         *
         * The identifier is a string of 20 zero-padded hexadecimal digits
         * representing a uniformly random 80-bit integer.
         */
        static String getPageViewId() {
            if (PAGEVIEW_ID == null) {
                PAGEVIEW_ID = generateRandomId();
            }
            return PAGEVIEW_ID;
        }

        /**
         * Generate a session identifier.
         *
         * @return session ID
         *
         * The identifier is a string of 20 zero-padded hexadecimal digits
         * representing a uniformly random 80-bit integer.
         */
        static String getSessionId() {
            if (SESSION_ID == null) {
                // If there is no runtime value for SESSION_ID, try to load a
                // value from persistent store.
                SESSION_ID = Prefs.getEventPlatformSessionId();

                if (SESSION_ID == null) {
                    // If there is no value in the persistent store, generate a new value for
                    // SESSION_ID, and write the update to the persistent store.
                    SESSION_ID = generateRandomId();
                    Prefs.setEventPlatformSessionId(SESSION_ID);
                }
            }
            return SESSION_ID;
        }

        static void beginNewSession() {
            // Clear runtime and persisted value for SESSION_ID.
            SESSION_ID = null;
            Prefs.setEventPlatformSessionId(null);

            // A session refresh implies a pageview refresh, so clear runtime value of PAGEVIEW_ID.
            PAGEVIEW_ID = null;
        }

        /**
         * @return a string of 20 zero-padded hexadecimal digits representing a uniformly random
         * 80-bit integer
         */
        @SuppressWarnings("checkstyle:magicnumber")
        public static String generateRandomId() {
            Random random = new Random();
            return String.format("%08x", random.nextInt()) + String.format("%08x", random.nextInt()) + String.format("%04x", random.nextInt() & 0xFFFF);
        }
    }

    /**
     * SamplingController: computes various sampling functions on the client
     *
     * Sampling is based on associative identifiers, each of which have a
     * well-defined scope, and sampling config, which each stream provides as
     * part of its configuration.
     */
    static class SamplingController {

        static Map<String, Boolean> SAMPLING_CACHE = new HashMap<>();

        /**
         * @param stream stream name
         * @return true if in sample or false otherwise
         */
        static boolean isInSample(String stream) {
            if (SAMPLING_CACHE.containsKey(stream)) {
                return SAMPLING_CACHE.get(stream);
            }

            StreamConfig streamConfig = getStreamConfig(stream);

            if (streamConfig == null) {
                return false;
            }

            SamplingConfig samplingConfig = streamConfig.getSamplingConfig();

            if (samplingConfig == null || samplingConfig.getRate() == 1.0) {
                return true;
            }
            if (samplingConfig.getRate() == 0.0) {
                return false;
            }

            boolean inSample = getSamplingValue(samplingConfig.getIdentifier()) < samplingConfig.getRate();
            SAMPLING_CACHE.put(stream, inSample);

            return inSample;
        }

        /**
         * @param identifier identifier type from sampling config
         * @return a floating point value between 0.0 and 1.0 (inclusive)
         */
        @SuppressWarnings("checkstyle:magicnumber")
        static double getSamplingValue(SamplingConfig.Identifier identifier) {
            String token = getSamplingId(identifier).substring(0, 8);
            return (double) Long.parseLong(token, 16) / (double) 0xFFFFFFFFL;
        }

        static String getSamplingId(SamplingConfig.Identifier identifier) {
            if (identifier == SESSION) {
                return AssociationController.getSessionId();
            }
            if (identifier == PAGEVIEW) {
                return AssociationController.getPageViewId();
            }
            if (identifier == DEVICE) {
                return Prefs.getAppInstallId();
            }
            throw new RuntimeException("Bad identifier type");
        }

    }

    private static synchronized void updateStreamConfigs(@NonNull Map<String, StreamConfig> streamConfigs) {
        Map<String, Set<String>> eventsToStreams = new HashMap<>();

        for (Map.Entry<String, StreamConfig> entry : STREAM_CONFIGS.entrySet()) {
            Set<String> subscribedEvents = new HashSet<>(entry.getValue().getSubscribedEvents());
            eventsToStreams.put(entry.getKey(), subscribedEvents);
        }

        EVENTS_TO_STREAMS = eventsToStreams;
        STREAM_CONFIGS = streamConfigs;
    }

    private static boolean isEnabled() {
        return WikipediaApp.getInstance().isOnline() && isEventLoggingEnabled();
    }

    private EventPlatformClient() {
    }
}
