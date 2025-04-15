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

package io.cdap.directives.aggregates;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.*;
import io.cdap.wrangler.api.annotations.*;
import io.cdap.wrangler.api.parser.*;
import io.cdap.wrangler.api.EntityCountMetric;

import java.util.*;

/**
 * Aggregates byte sizes and time durations across rows with configurable output units.
 */
@Description(
        "Aggregates byte sizes (e.g., '10MB') and time durations (e.g., '500ms') across rows. " +
                "Supports output units: bytes/KB/MB/GB/TB for sizes and ns/ms/sec/min/hour for time. " +
                "Aggregation types: 'total' or 'average'."
)
@Categories(categories = {"aggregate"})
@Plugin(type = Directive.TYPE)
public class AggregateMetricsDirective implements Directive {
    public static final String NAME = "aggregate-metrics";
    private static final TransientVariableScope SCOPE = TransientVariableScope.GLOBAL;
    private static final String BYTE_TOTAL_KEY = NAME + "-byte-total";
    private static final String TIME_TOTAL_KEY = NAME + "-time-total";
    private static final String ROW_COUNT_KEY = NAME + "-row-count";

    // Configuration parameters
    private String sizeColumn;
    private String timeColumn;
    private String outputSizeColumn;
    private String outputTimeColumn;
    private String sizeUnit = "bytes";
    private String timeUnit = "milliseconds";
    private String aggregationType = "total";

    @Override
    public UsageDefinition define() {
        UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
        builder.define("sizeColumn", TokenType.COLUMN_NAME, "Source column containing byte sizes");
        builder.define("timeColumn", TokenType.COLUMN_NAME, "Source column containing time durations");
        builder.define("outputSizeColumn", TokenType.COLUMN_NAME, "Target column for size results");
        builder.define("outputTimeColumn", TokenType.COLUMN_NAME, "Target column for time results");
        builder.define("sizeUnit", TokenType.TEXT, "Output unit for size (bytes, KB, MB, GB, TB)", true);
        builder.define("timeUnit", TokenType.TEXT, "Output unit for time (ns, ms, sec, min, hour)", true);
        builder.define("aggregationType", TokenType.TEXT, "'total' or 'average'", true);
        return builder.build();
    }

    @Override
    public void initialize(Arguments args) throws DirectiveParseException {
        // Required arguments
        this.sizeColumn = ((ColumnName) args.value("sizeColumn")).value();
        this.timeColumn = ((ColumnName) args.value("timeColumn")).value();
        this.outputSizeColumn = ((ColumnName) args.value("outputSizeColumn")).value();
        this.outputTimeColumn = ((ColumnName) args.value("outputTimeColumn")).value();

        // Optional arguments with validation
        if (args.contains("sizeUnit")) {
            this.sizeUnit = validateSizeUnit(((Text) args.value("sizeUnit")).value());
        }
        if (args.contains("timeUnit")) {
            this.timeUnit = validateTimeUnit(((Text) args.value("timeUnit")).value());
        }
        if (args.contains("aggregationType")) {
            this.aggregationType = validateAggregationType(((Text) args.value("aggregationType")).value());
        }
    }

    @Override
    public void destroy() {
        // Clean up transient store if needed
        // This would require access to ExecutorContext which we don't have here
        // Consider moving cleanup to execute() if needed
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
        TransientStore store = context.getTransientStore();

        // Initialize or get existing aggregation state
        Long byteTotal = (Long) store.get(BYTE_TOTAL_KEY);
        Long timeTotal = (Long) store.get(TIME_TOTAL_KEY);
        Integer rowCount = (Integer) store.get(ROW_COUNT_KEY);

        if (byteTotal == null) byteTotal = 0L;
        if (timeTotal == null) timeTotal = 0L;
        if (rowCount == null) rowCount = 0;

        // Process each row
        for (Row row : rows) {
            try {
                // Process byte size if column exists
                Object sizeValue = row.getValue(sizeColumn);
                if (sizeValue != null) {
                    byteTotal += parseByteSize(sizeValue);
                }

                // Process time duration if column exists
                Object timeValue = row.getValue(timeColumn);
                if (timeValue != null) {
                    timeTotal += parseTimeDuration(timeValue);
                }

                rowCount++;
            } catch (Exception e) {
                throw new DirectiveExecutionException(
                        String.format("%s: Error processing row - %s", NAME, e.getMessage()), e);
            }
        }

        // Update store with current state
        store.set(SCOPE, BYTE_TOTAL_KEY, byteTotal);
        store.set(SCOPE, TIME_TOTAL_KEY, timeTotal);
        store.set(SCOPE, ROW_COUNT_KEY, rowCount);

        // Calculate final values with unit conversion
        double finalSize = convertSize(byteTotal);
        double finalTime = convertTime(timeTotal);

        // Apply aggregation type if average requested
        if ("average".equalsIgnoreCase(aggregationType)) {
            if (rowCount > 0) {
                finalSize /= rowCount;
                finalTime /= rowCount;
            }
        }

        // Create result row
        Row result = new Row();
        result.add(outputSizeColumn, finalSize);
        result.add(outputTimeColumn, finalTime);

        return Collections.singletonList(result);
    }

    @Override
    public List<EntityCountMetric> getCountMetrics() {
        // Return null if no metrics to report
        return null;
    }

    // Helper method to parse byte size values
    private long parseByteSize(Object value) throws DirectiveExecutionException {
        try {
            if (value instanceof ByteSize) {
                return ((ByteSize) value).value();
            }
            return new ByteSize(value.toString()).value();
        } catch (Exception e) {
            throw new DirectiveExecutionException(
                    String.format("%s: Invalid byte size value '%s' - %s", NAME, value, e.getMessage()), e);
        }
    }

    // Helper method to parse time duration values
    private long parseTimeDuration(Object value) throws DirectiveExecutionException {
        try {
            if (value instanceof TimeDuration) {
                return ((TimeDuration) value).value();
            }
            return new TimeDuration(value.toString()).value();
        } catch (Exception e) {
            throw new DirectiveExecutionException(
                    String.format("%s: Invalid time duration value '%s' - %s", NAME, value, e.getMessage()), e);
        }
    }

    // Validates and normalizes size units
    private String validateSizeUnit(String unit) throws DirectiveParseException {
        String normalized = unit.toLowerCase();
        if (!Arrays.asList("bytes", "kb", "mb", "gb", "tb").contains(normalized)) {
            throw new DirectiveParseException(
                    String.format("%s: Invalid size unit '%s'. Valid options: bytes, KB, MB, GB, TB", NAME, unit));
        }
        return normalized;
    }

    // Validates and normalizes time units
    private String validateTimeUnit(String unit) throws DirectiveParseException {
        String normalized = unit.toLowerCase();
        if (!Arrays.asList("ns", "ms", "sec", "min", "hour").contains(normalized)) {
            throw new DirectiveParseException(
                    String.format("%s: Invalid time unit '%s'. Valid options: ns, ms, sec, min, hour", NAME, unit));
        }
        return normalized;
    }

    // Validates and normalizes aggregation type
    private String validateAggregationType(String type) throws DirectiveParseException {
        String normalized = type.toLowerCase();
        if (!Arrays.asList("total", "average").contains(normalized)) {
            throw new DirectiveParseException(
                    String.format("%s: Invalid aggregation type '%s'. Valid options: total, average", NAME, type));
        }
        return normalized;
    }

    // Converts bytes to the specified output unit
    private double convertSize(long bytes) {
        switch (sizeUnit.toLowerCase()) {
            case "kb": return bytes / 1024.0;
            case "mb": return bytes / (1024.0 * 1024.0);
            case "gb": return bytes / (1024.0 * 1024.0 * 1024.0);
            case "tb": return bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);
            default: return bytes; // bytes
        }
    }

    // Converts nanoseconds to the specified output unit
    private double convertTime(long nanoseconds) {
        switch (timeUnit.toLowerCase()) {
            case "ms": return nanoseconds / 1_000_000.0;
            case "sec": return nanoseconds / 1_000_000_000.0;
            case "min": return nanoseconds / (60.0 * 1_000_000_000.0);
            case "hour": return nanoseconds / (3600.0 * 1_000_000_000.0);
            default: return nanoseconds; // ns
        }
    }
}