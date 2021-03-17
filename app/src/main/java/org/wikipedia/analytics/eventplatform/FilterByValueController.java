package org.wikipedia.analytics.eventplatform;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;

class FilterByValueController {

    /**
     * Filter pending events based on their contents.
     *
     * @param config stream config
     * @param event event data
     * @return true if the event data passes all filtering conditions in the stream config,
     *   otherwise false
     */
    static boolean filterByValue(@NonNull StreamConfig config, @NonNull Event event) {
        // TODO: Implement all operations defined in the design doc.
        // https://docs.google.com/document/d/1odTxj9tpn1WD-xo1IjVx-V4Ak53tmmPwjYQHH0hhdVU
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

    static class FilteringRules {
    }

}
