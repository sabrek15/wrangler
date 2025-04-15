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

import com.google.gson.JsonElement;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class ByteSizeTest {
    @Test
    public void testValidByteSizeParsing() {
        // Test standard units
        assertEquals(Long.valueOf(1024L), new ByteSize("1KB").value());
        assertEquals(Long.valueOf(1024L), new ByteSize("1kb").value()); // case insensitive
        assertEquals(Long.valueOf(1536L), new ByteSize("1.5KB").value()); // fractional
        assertEquals(Long.valueOf(1048576L), new ByteSize("1MB").value());
        assertEquals(Long.valueOf(1073741824L), new ByteSize("1GB").value());
        assertEquals(Long.valueOf(1099511627776L), new ByteSize("1TB").value());
        assertEquals(Long.valueOf(1125899906842624L), new ByteSize("1PB").value());

        // Test bytes
        assertEquals(Long.valueOf(500L), new ByteSize("500B").value());
        assertEquals(Long.valueOf(0L), new ByteSize("0B").value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFormatMissingUnit() {
        new ByteSize("1024"); // No unit
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNumberFormat() {
        new ByteSize("abcKB"); // Invalid number
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownUnit() {
        new ByteSize("10XB"); // Invalid unit
    }

    @Test
    public void testBoundaryValues() {
        assertEquals(Long.valueOf(Long.MAX_VALUE), new ByteSize(Long.MAX_VALUE + "B").value());
        assertEquals(Long.valueOf(0L), new ByteSize("0B").value());
    }

    @Test
    public void testJsonSerialization() {
        ByteSize size = new ByteSize("1.5MB");
        JsonElement json = size.toJson();
        assertEquals("BYTE_SIZE", json.getAsJsonObject().get("type").getAsString());
        assertEquals("1.5MB", json.getAsJsonObject().get("value").getAsString());
        assertEquals(1572864L, json.getAsJsonObject().get("bytes").getAsLong());
    }
}