package org.wikipedia.analytics.eventplatform;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import static org.wikipedia.util.log.L.logRemoteErrorIfProd;

class FilterByValueController {

    /**
     * Supports filtering values according to whether the values at a specified set of paths equal
     * or do not equal certain values. Filtering is to be specified in stream configuration as
     * in this example:
     *
     *  {
     *      producer: {
     *          filter_values: {
     *              allow_if_match: {
     *                  a: 1
     *              },
     *              allow_if_not_match: {
     *                  'b.c': 2
     *              }
     *          }
     *      }
     *  }
     *
     * Given the above, an event data object would pass filtering if it contained a field `a`
     * with value 1, and did not contain a field `b` with a subfield `c` with value 2.
     *
     * If a path specified in an `allow_if_match` condition does not exist, the event fails the
     * filtering condition. If a path specified in an `allow_if_not_match` condition does not
     * exist, the event passes the filtering condition.
     *
     * @param config stream config
     * @param event event data
     * @return true if the event data passes all filtering conditions in the stream config,
     *   otherwise false
     */
    static boolean filterByValue(@NonNull StreamConfig config, @NonNull Event event) {
        FilteringValues filteringValues = config.getFilteringValues();
        if (filteringValues == null) {
            return true;
        }

        Map<String, Object> allowIfMatch = filteringValues.getAllowIfMatch();
        for (String path : allowIfMatch.keySet()) {
            Object expected = allowIfMatch.get(path);
            Object actual;
            try {
                actual = valueForPath(event, path);
            } catch (NoSuchFieldException e) {
                return false;
            } catch (IllegalAccessException e) {
                logRemoteErrorIfProd(e);
                return false;
            }
            if (actual == null && expected != null) {
                return false;
            }
            if (actual != null && !actual.equals(expected)) {
                return false;
            }
        }

        Map<String, Object> allowIfNotMatch = filteringValues.getAllowIfNotMatch();
        for (String path : allowIfNotMatch.keySet()) {
            Object unexpected = allowIfNotMatch.get(path);
            Object actual;
            try {
                actual = valueForPath(event, path);
            } catch (NoSuchFieldException e) {
                continue;
            } catch (IllegalAccessException e) {
                logRemoteErrorIfProd(e);
                return false;
            }
            if (actual == null && unexpected == null) {
                return false;
            }
            if (actual != null && actual.equals(unexpected)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the value at the location within the provided eventData object, with the path
     * represented as a dot-delimited string ('path.to.value'). Throws if the path argument
     * evaluates to a path that does not exist.
     * @param event event data
     * @param path period-delimited path string
     * @return value at path within the event data
     * @throws NoSuchFieldException if the path does not exist
     * @throws IllegalAccessException (this shouldn't happen)
     */
    @Nullable static Object valueForPath(@NonNull Event event, @NonNull String path)
            throws NoSuchFieldException, IllegalAccessException {
        String[] pathSegments = path.split("\\.");
        Object value = event;
        Class<?> clazz;
        Field field;
        for (String segment : pathSegments) {
            if (value == null) {
                throw new NoSuchFieldException(segment);
            }
            clazz = value.getClass();
            field = clazz.getDeclaredField(segment);
            field.setAccessible(true);
            value = field.get(value);
        }
        return value;
    }

    static class FilteringValues {
        @Nullable private Map<String, Object> allowIfMatch;
        @Nullable private Map<String, Object> allowIfNotMatch;

        FilteringValues(
                @Nullable Map<String, Object> allowIfMatch,
                @Nullable Map<String, Object> allowIfNotMatch
        ) {
            this.allowIfMatch = allowIfMatch;
            this.allowIfNotMatch = allowIfNotMatch;
        }

        @NonNull private Map<String, Object> getAllowIfMatch() {
            return allowIfMatch != null ? allowIfMatch : Collections.emptyMap();
        }

        @NonNull private Map<String, Object> getAllowIfNotMatch() {
            return allowIfNotMatch != null ? allowIfNotMatch : Collections.emptyMap();
        }
    }

}
