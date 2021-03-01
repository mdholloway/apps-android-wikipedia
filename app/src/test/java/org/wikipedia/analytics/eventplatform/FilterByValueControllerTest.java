package org.wikipedia.analytics.eventplatform;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.wikipedia.analytics.eventplatform.FilterByValueController.filterByValue;
import static org.wikipedia.analytics.eventplatform.FilterByValueController.valueForPath;

public class FilterByValueControllerTest {

    @Test
    public void testValueForPath() {
        TestEvent event = new TestEvent("b", 'c', 1, 0.0f, null, true, false);
        try {
            assertThat(valueForPath(event, "a"), is("b"));
            assertThat(valueForPath(event, "b"), is('c'));
            assertThat(valueForPath(event, "c"), is(1));
            assertThat(valueForPath(event, "d"), is(0.0f));
            assertThat(valueForPath(event, "e"), is(nullValue()));
            assertThat(valueForPath(event, "h.f"), is(true));
            assertThat(valueForPath(event, "h.g"), is(false));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
        assertThrows(NoSuchFieldException.class, () -> valueForPath(event, "i"));
        assertThrows(NoSuchFieldException.class, () -> valueForPath(event, "h.d"));
        assertThrows(NoSuchFieldException.class, () -> valueForPath(event, ""));
    }

    @Test
    public void testAllowIfMatchPassesWithValidPath() {
        TestEvent event = new TestEvent("b", 'c', 1, 0.0f, null, true, false);
        Map<String, Object> allowIfMatch = new HashMap<String, Object>() {{ put("a", "b"); }};
        FilterByValueController.FilteringValues filteringValues =
                new FilterByValueController.FilteringValues(allowIfMatch, null);
        StreamConfig config = new StreamConfig(filteringValues);
        assertThat(filterByValue(config, event), is(true));
    }

    @Test
    public void testAllowIfNotMatchPassesWithValidPath() {
        TestEvent event = new TestEvent("b", 'c', 1, 0.0f, null, true, false);
        Map<String, Object> allowIfNotMatch = new HashMap<String, Object>() {{ put("c", 0); }};
        FilterByValueController.FilteringValues filteringValues =
                new FilterByValueController.FilteringValues(null, allowIfNotMatch);
        StreamConfig config = new StreamConfig(filteringValues);
        assertThat(filterByValue(config, event), is(true));
    }

    @Test
    public void testAllowIfMatchFailsWithInvalidPath() {
        TestEvent event = new TestEvent("b", 'c', 1, 0.0f, null, true, false);
        Map<String, Object> allowIfMatch = new HashMap<String, Object>() {{ put("i", 1); }};
        FilterByValueController.FilteringValues filteringValues =
                new FilterByValueController.FilteringValues(allowIfMatch, null);
        StreamConfig config = new StreamConfig(filteringValues);
        assertThat(filterByValue(config, event), is(false));
    }

    @Test
    public void testAllowIfNotMatchPassesWithInvalidPath() {
        TestEvent event = new TestEvent("b", 'c', 1, 0.0f, null, true, false);
        Map<String, Object> allowIfNotMatch = new HashMap<String, Object>() {{ put("i", 1); }};
        FilterByValueController.FilteringValues filteringValues =
                new FilterByValueController.FilteringValues(null, allowIfNotMatch);
        StreamConfig config = new StreamConfig(filteringValues);
        assertThat(filterByValue(config, event), is(true));
    }

    private static class TestEvent extends Event {
        private String a;
        private char b;
        private int c;
        private float d;
        private Object e;
        private E h;

        private TestEvent(String a, char b, int c, float d, Object e, boolean f, boolean g) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;
            E h = new E();
            h.f = f;
            h.g = g;
            this.h = h;
        }

        private static class E {
            private boolean f;
            private boolean g;
        }
    }

}
