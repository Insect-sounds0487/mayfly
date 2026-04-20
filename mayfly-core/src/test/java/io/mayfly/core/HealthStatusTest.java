package io.mayfly.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HealthStatus 枚举单元测试")
class HealthStatusTest {
    
    @Test
    @DisplayName("测试枚举值存在")
    void testEnumValuesExist() {
        HealthStatus[] values = HealthStatus.values();
        
        assertEquals(3, values.length);
    }
    
    @Test
    @DisplayName("测试HEALTHY枚举值")
    void testHealthyEnumValue() {
        HealthStatus status = HealthStatus.valueOf("HEALTHY");
        
        assertEquals(HealthStatus.HEALTHY, status);
    }
    
    @Test
    @DisplayName("测试UNHEALTHY枚举值")
    void testUnhealthyEnumValue() {
        HealthStatus status = HealthStatus.valueOf("UNHEALTHY");
        
        assertEquals(HealthStatus.UNHEALTHY, status);
    }
    
    @Test
    @DisplayName("测试COOLDOWN枚举值")
    void testCooldownEnumValue() {
        HealthStatus status = HealthStatus.valueOf("COOLDOWN");
        
        assertEquals(HealthStatus.COOLDOWN, status);
    }
    
    @Test
    @DisplayName("测试枚举值比较")
    void testEnumValueComparison() {
        assertNotEquals(HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);
        assertNotEquals(HealthStatus.HEALTHY, HealthStatus.COOLDOWN);
        assertNotEquals(HealthStatus.UNHEALTHY, HealthStatus.COOLDOWN);
    }
    
    @Test
    @DisplayName("测试枚举名称")
    void testEnumNames() {
        assertEquals("HEALTHY", HealthStatus.HEALTHY.name());
        assertEquals("UNHEALTHY", HealthStatus.UNHEALTHY.name());
        assertEquals("COOLDOWN", HealthStatus.COOLDOWN.name());
    }
    
    @Test
    @DisplayName("测试枚举ordinal值")
    void testEnumOrdinals() {
        assertEquals(0, HealthStatus.HEALTHY.ordinal());
        assertEquals(1, HealthStatus.UNHEALTHY.ordinal());
        assertEquals(2, HealthStatus.COOLDOWN.ordinal());
    }
}
