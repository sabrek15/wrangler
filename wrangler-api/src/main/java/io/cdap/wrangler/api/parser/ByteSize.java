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
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;


@PublicEvolving
public class ByteSize implements Token {
    private final long bytes;
    private final String original;


    public ByteSize(String value) {
        this.original = value;
        this.bytes = parse(value);
    }

    @Override
    public Long value() {
        return bytes;
    }

    @Override
    public TokenType type() {
        return TokenType.BYTE_SIZE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", TokenType.BYTE_SIZE.name());
        object.addProperty("value", original);
        object.addProperty("bytes", bytes);
        return object;
    }

    private long parse(String value) {
        value = value.trim().toLowerCase();
        int splitPos = findSplitPosition(value);

        double size = parseNumber(value.substring(0, splitPos));
        String unit = value.substring(splitPos).trim();

        switch (unit) {
            case "b": return (long) size;
            case "kb": return (long) (size * 1024);
            case "mb": return (long) (size * 1024 * 1024);
            case "gb": return (long) (size * 1024 * 1024 * 1024);
            case "tb": return (long) (size * 1024 * 1024 * 1024 * 1024);
            case "pb": return (long) (size * 1024 * 1024 * 1024 * 1024 * 1024);
            default: throw new IllegalArgumentException("Invalid byte size unit: " + unit);
        }
    }

    private int findSplitPosition(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isDigit(c) && c != '.') {
                return i;
            }
        }
        throw new IllegalArgumentException("Invalid byte size format: " + value);
    }

    private double parseNumber(String numStr) {
        try {
            return Double.parseDouble(numStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number in byte size: " + numStr);
        }
    }
}