/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sandbox;

import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.search.sandbox.Sandbox.ResourceConsumptionLimits;
import org.opensearch.search.sandbox.Sandbox.SandboxAttributes;
import org.opensearch.search.sandbox.Sandbox.SystemResource;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class SandboxTests extends OpenSearchTestCase {
    public static final String NAME_ONE = "sandbox_one";
    public static final String NAME_TWO = "sandbox_two";
    public static final String NAME_NONE_EXISTED = "sandbox_non_existed";
    public static final double THIRTY = 0.3;
    public static final double FORTY = 0.4;
    public static final double FIFTY = 0.5;
    public static final double SIXTY = 0.6;
    public static final String INDICES_NAME_VAL_ONE = "indices_one";
    public static final String INDICES_NAME_VAL_TWO = "indices_two";
    public static final String MONITOR = "monitor";
    public static final String JVM = "jvm";

    public static Sandbox createSandbox(String name, List<String> attributes, List<Double> resourceAllocation, String enforcement) {
        SandboxAttributes sandboxAttributes = new SandboxAttributes(attributes.get(0));
        SystemResource jvm = SystemResource.builder().allocation(resourceAllocation.get(0)).name("jvm").build();
        ResourceConsumptionLimits limit = new ResourceConsumptionLimits(jvm);
        Sandbox sandbox = Sandbox.builder()
            .name(name)
            .sandboxAttributes(sandboxAttributes)
            .resourceConsumptionLimit(limit)
            .enforcement(enforcement)
            .build(true);
        return sandbox;
    }

    public static void compareSandboxes(List<Sandbox> listOne, List<Sandbox> listTwo) {
        assertEquals(listOne.size(), listTwo.size());
        for (Sandbox sandboxOne : listOne) {
            String sandboxOneName = sandboxOne.getName();
            List<Sandbox> sandboxTwoList = listTwo.stream().filter(sb -> sb.getName().equals(sandboxOneName)).collect(Collectors.toList());
            assertEquals(1, sandboxTwoList.size());
            Sandbox sandboxtwo = sandboxTwoList.get(0);
            assertEquals(sandboxOne.get_id(), sandboxtwo.get_id());
            assertEquals(sandboxOne.getName(), sandboxtwo.getName());
            assertEquals(sandboxOne.getEnforcement(), sandboxtwo.getEnforcement());
            assertEquals(sandboxOne.getSandboxAttributes().getIndicesValues(), sandboxtwo.getSandboxAttributes().getIndicesValues());
            assertEquals(
                sandboxOne.getResourceConsumptionLimits().getJvm().getAllocation(),
                sandboxtwo.getResourceConsumptionLimits().getJvm().getAllocation(),
                0
            );
            assertEquals(
                sandboxOne.getResourceConsumptionLimits().getJvm().getName(),
                sandboxtwo.getResourceConsumptionLimits().getJvm().getName()
            );
        }
    }

    public void testSerializationSandbox() throws IOException {
        SandboxAttributes sandboxAttributes = SandboxAttributes.builder().indicesValues(INDICES_NAME_VAL_ONE).build();
        SystemResource jvm = SystemResource.builder().allocation(THIRTY).name(JVM).build();
        ResourceConsumptionLimits limit = new ResourceConsumptionLimits(jvm);
        Sandbox sandbox = Sandbox.builder()
            .name(NAME_ONE)
            .sandboxAttributes(sandboxAttributes)
            .resourceConsumptionLimit(limit)
            .enforcement(MONITOR)
            .build(true);

        BytesStreamOutput out = new BytesStreamOutput();
        sandbox.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();
        Sandbox otherSandbox = new Sandbox(streamInput);
        compareSandboxes(List.of(sandbox), List.of(otherSandbox));
    }

    public void testEmptyName() {
        SandboxAttributes sandboxAttributes = SandboxAttributes.builder().indicesValues(INDICES_NAME_VAL_ONE).build();
        SystemResource jvm = SystemResource.builder().allocation(THIRTY).name(JVM).build();
        ResourceConsumptionLimits limit = new ResourceConsumptionLimits(jvm);
        assertThrows(
            IllegalArgumentException.class,
            () -> Sandbox.builder()
                .name("")
                .sandboxAttributes(sandboxAttributes)
                .resourceConsumptionLimit(limit)
                .enforcement(MONITOR)
                .build(true)
        );
    }

    public void testInvalidNameStart() {
        SandboxAttributes sandboxAttributes = SandboxAttributes.builder().indicesValues(INDICES_NAME_VAL_ONE).build();
        SystemResource jvm = SystemResource.builder().allocation(THIRTY).name(JVM).build();
        ResourceConsumptionLimits limit = new ResourceConsumptionLimits(jvm);
        assertThrows(
            IllegalArgumentException.class,
            () -> Sandbox.builder()
                .name("-test")
                .sandboxAttributes(sandboxAttributes)
                .resourceConsumptionLimit(limit)
                .enforcement(MONITOR)
                .build(true)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> Sandbox.builder()
                .name("_test")
                .sandboxAttributes(sandboxAttributes)
                .resourceConsumptionLimit(limit)
                .enforcement(MONITOR)
                .build(true)
        );
    }

    public void testInvalidCharInName() {
        SandboxAttributes sandboxAttributes = SandboxAttributes.builder().indicesValues(INDICES_NAME_VAL_ONE).build();
        SystemResource jvm = SystemResource.builder().allocation(THIRTY).name(JVM).build();
        ResourceConsumptionLimits limit = new ResourceConsumptionLimits(jvm);
        assertThrows(
            IllegalArgumentException.class,
            () -> Sandbox.builder()
                .name("test:")
                .sandboxAttributes(sandboxAttributes)
                .resourceConsumptionLimit(limit)
                .enforcement(MONITOR)
                .build(true)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> Sandbox.builder()
                .name("test*")
                .sandboxAttributes(sandboxAttributes)
                .resourceConsumptionLimit(limit)
                .enforcement(MONITOR)
                .build(true)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> Sandbox.builder()
                .name("test?")
                .sandboxAttributes(sandboxAttributes)
                .resourceConsumptionLimit(limit)
                .enforcement(MONITOR)
                .build(true)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> Sandbox.builder()
                .name("Test")
                .sandboxAttributes(sandboxAttributes)
                .resourceConsumptionLimit(limit)
                .enforcement(MONITOR)
                .build(true)
        );
    }

    public void testInvalidEnforcement() {
        SandboxAttributes sandboxAttributes = SandboxAttributes.builder().indicesValues(INDICES_NAME_VAL_ONE).build();
        SystemResource jvm = SystemResource.builder().allocation(THIRTY).name(JVM).build();
        ResourceConsumptionLimits limit = new ResourceConsumptionLimits(jvm);
        assertThrows(
            IllegalArgumentException.class,
            () -> Sandbox.builder()
                .name("test")
                .sandboxAttributes(sandboxAttributes)
                .resourceConsumptionLimit(limit)
                .enforcement("random")
                .build(true)
        );
    }

    public void testInvalidAttribute() {
        assertThrows(IllegalArgumentException.class, () -> SandboxAttributes.builder().indicesValues("*indices").build());
        assertThrows(IllegalArgumentException.class, () -> SandboxAttributes.builder().indicesValues("indices?").build());
    }

    public void testInvalidSystemResource() {
        assertThrows(IllegalArgumentException.class, () -> SystemResource.builder().allocation(-3).name(JVM).build());
        assertThrows(IllegalArgumentException.class, () -> SystemResource.builder().allocation(12).name(JVM).build());
        assertThrows(IllegalArgumentException.class, () -> SystemResource.builder().allocation(0.345).name(JVM).build());
        assertThrows(IllegalArgumentException.class, () -> SystemResource.builder().allocation(0.12).name("cpu").build());
    }
}
