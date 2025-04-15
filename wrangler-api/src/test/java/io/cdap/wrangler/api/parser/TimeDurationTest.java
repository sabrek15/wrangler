/*
 * Copyright 2024 Your Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.cdap.wrangler.api.parser;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import org.junit.Test;
import static org.junit.Assert.*;

public class TimeDurationTest {
    @Test
    public void testValidTimeDurationParsing() {
        // Test all units
        assertEquals(Long.valueOf(1L), new TimeDuration("1000000ns").value()); // 1,000,000 ns = 1 ms
        assertEquals(Long.valueOf(1L), new TimeDuration("1000us").value());    // 1,000 μs = 1 ms
        assertEquals(Long.valueOf(1L), new TimeDuration("1ms").value());       // 1 ms = 1 ms
        assertEquals(Long.valueOf(1000L), new TimeDuration("1s").value());     // 1 s = 1,000 ms
        assertEquals(Long.valueOf(60000L), new TimeDuration("1m").value());    // 1 m = 60,000 ms
        assertEquals(Long.valueOf(3600000L), new TimeDuration("1h").value());  // 1 h = 3,600,000 ms
        assertEquals(Long.valueOf(86400000L), new TimeDuration("1d").value()); // 1 d = 86,400,000 ms

        // Test fractional
        assertEquals(Long.valueOf(1500L), new TimeDuration("1.5s").value());   // 1.5 s = 1,500 ms
    }

    @Test
    public void testTimeUnitConversion() {
        TimeDuration duration = new TimeDuration("90s");
        assertEquals(90000L, duration.get(TimeUnit.MILLISECONDS));
        assertEquals(90L, duration.get(TimeUnit.SECONDS));
        assertEquals(1L, duration.get(TimeUnit.MINUTES));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingUnit() {
        new TimeDuration("100"); // No unit
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNumber() {
        new TimeDuration("abcms"); // Invalid number
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownUnit() {
        new TimeDuration("10xy"); // Invalid unit
    }

    @Test
    public void testJsonSerialization() {
        TimeDuration duration = new TimeDuration("1.5h");
        JsonElement json = duration.toJson();
        assertEquals("TIME_DURATION", json.getAsJsonObject().get("type").getAsString());
        assertEquals("1.5h", json.getAsJsonObject().get("value").getAsString());
        assertEquals(5400000L, json.getAsJsonObject().get("milliseconds").getAsLong());
    }
}