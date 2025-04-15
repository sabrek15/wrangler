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
import java.util.concurrent.TimeUnit;

@PublicEvolving
public class TimeDuration implements Token {
    private final long milliseconds;
    private final String original;

    public TimeDuration(String value) {
        this.original = value;
        this.milliseconds = parse(value);
    }

    @Override
    public Long value() {
        return milliseconds;
    }

    @Override
    public TokenType type() {
        return TokenType.TIME_DURATION;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", TokenType.TIME_DURATION.name());
        object.addProperty("value", original);
        object.addProperty("milliseconds", milliseconds);
        return object;
    }

    public long get(TimeUnit unit) {
        return unit.convert(milliseconds, TimeUnit.MILLISECONDS);
    }

    private long parse(String value) {
        value = value.trim().toLowerCase();
        int splitPos = findSplitPosition(value);

        double duration = parseNumber(value.substring(0, splitPos));
        String unit = value.substring(splitPos).trim();

        switch (unit) {
            case "ns": return (long) (duration / 1_000_000);
            case "us": return (long) (duration / 1_000);
            case "ms": return (long) duration;
            case "s": return (long) (duration * 1000);
            case "m": return (long) (duration * 1000 * 60);
            case "h": return (long) (duration * 1000 * 60 * 60);
            case "d": return (long) (duration * 1000 * 60 * 60 * 24);
            default: throw new IllegalArgumentException("Invalid time unit: " + unit);
        }
    }

    private int findSplitPosition(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isDigit(c) && c != '.') {
                return i;
            }
        }
        throw new IllegalArgumentException("Invalid time duration format: " + value);
    }

    private double parseNumber(String numStr) {
        try {
            return Double.parseDouble(numStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number in time duration: " + numStr);
        }
    }
}