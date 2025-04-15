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

import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.RecipePipeline;
import io.cdap.wrangler.test.TestingRig;
import io.cdap.wrangler.test.api.TestRecipe;
import io.cdap.wrangler.test.api.TestRows;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class AggregateMetricsDirectiveTest {

    @Test
    public void testTotalAggregationWithUnitConversion() throws Exception {
        TestRows rows = new TestRows();
        rows.add(createRow("data_size", "1KB", "proc_time", "100ms"));
        rows.add(createRow("data_size", "2KB", "proc_time", "200ms"));

        TestRecipe recipe = new TestRecipe();
        recipe.add("aggregate-metrics :data_size :proc_time total_size_mb total_time_sec sizeUnit=MB timeUnit=sec");

        try (RecipePipeline pipeline = TestingRig.pipeline(AggregateMetricsDirective.class, recipe)) {
            List<Row> results = pipeline.execute(rows.toList());
            assertEquals(1, results.size());
            Row result = results.get(0);
            assertEquals(0.0029296875, ((Number) result.getValue("total_size_mb")).doubleValue(), 0.000001);
            assertEquals(0.3, ((Number) result.getValue("total_time_sec")).doubleValue(), 0.001);
        }
    }

    @Test
    public void testAverageAggregation() throws Exception {
        TestRows rows = new TestRows();
        rows.add(createRow("size", "1MB", "time", "1s"));
        rows.add(createRow("size", "2MB", "time", "2s"));

        TestRecipe recipe = new TestRecipe();
        recipe.add("aggregate-metrics :size :time avg_size_mb avg_time_sec sizeUnit=MB timeUnit=sec aggregationType=average");

        try (RecipePipeline pipeline = TestingRig.pipeline(AggregateMetricsDirective.class, recipe)) {
            List<Row> results = pipeline.execute(rows.toList());
            assertEquals(1, results.size());
            Row result = results.get(0);
            assertEquals(1.5, ((Number) result.getValue("avg_size_mb")).doubleValue(), 0.001);
            assertEquals(1.5, ((Number) result.getValue("avg_time_sec")).doubleValue(), 0.001);
        }
    }

    @Test
    public void testMixedInputUnits() throws Exception {
        TestRows rows = new TestRows();
        rows.add(createRow("size", "1KB", "time", "1000ms"));
        rows.add(createRow("size", "0.5MB", "time", "0.5s"));

        TestRecipe recipe = new TestRecipe();
        recipe.add("aggregate-metrics :size :time total_size_kb total_time_ms sizeUnit=KB timeUnit=ms");

        try (RecipePipeline pipeline = TestingRig.pipeline(AggregateMetricsDirective.class, recipe)) {
            List<Row> results = pipeline.execute(rows.toList());
            assertEquals(1, results.size());
            Row result = results.get(0);
            assertEquals(513.0, ((Number) result.getValue("total_size_kb")).doubleValue(), 0.001);
            assertEquals(1500.0, ((Number) result.getValue("total_time_ms")).doubleValue(), 0.001);
        }
    }

    @Test(expected = Exception.class)
    public void testInvalidSizeValue() throws Exception {
        TestRows rows = new TestRows();
        rows.add(createRow("size", "invalid", "time", "100ms"));

        TestRecipe recipe = new TestRecipe();
        recipe.add("aggregate-metrics :size :time total_size total_time");

        try (RecipePipeline pipeline = TestingRig.pipeline(AggregateMetricsDirective.class, recipe)) {
            pipeline.execute(rows.toList());
        }
    }

    @Test
    public void testEmptyInput() throws Exception {
        TestRows rows = new TestRows();
        TestRecipe recipe = new TestRecipe();
        recipe.add("aggregate-metrics :size :time total_size total_time");

        try (RecipePipeline pipeline = TestingRig.pipeline(AggregateMetricsDirective.class, recipe)) {
            List<Row> results = pipeline.execute(rows.toList());
            assertEquals(1, results.size());
            Row result = results.get(0);
            assertEquals(0.0, ((Number) result.getValue("total_size")).doubleValue(), 0.001);
            assertEquals(0.0, ((Number) result.getValue("total_time")).doubleValue(), 0.001);
        }
    }

    private Row createRow(String col1, String val1, String col2, String val2) {
        Row row = new Row();
        row.add(col1, val1);
        row.add(col2, val2);
        return row;
    }
}