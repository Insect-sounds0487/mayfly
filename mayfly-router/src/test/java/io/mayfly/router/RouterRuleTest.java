package io.mayfly.router;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RouterRule 单元测试")
class RouterRuleTest {
    
    @Test
    @DisplayName("测试使用Builder创建完整规则")
    void testBuilderWithAllFields() {
        RouterRule rule = RouterRule.builder()
            .name("test-rule")
            .condition("prompt.messages[0].text.contains('urgent')")
            .targetModel("fast-model")
            .priority(1)
            .build();
        
        assertEquals("test-rule", rule.getName());
        assertEquals("prompt.messages[0].text.contains('urgent')", rule.getCondition());
        assertEquals("fast-model", rule.getTargetModel());
        assertEquals(1, rule.getPriority());
    }
    
    @Test
    @DisplayName("测试使用默认优先级创建规则")
    void testBuilderWithDefaultPriority() {
        RouterRule rule = RouterRule.builder()
            .name("default-priority-rule")
            .condition("true")
            .targetModel("default-model")
            .build();
        
        assertEquals(100, rule.getPriority());
    }
    
    @Nested
    @DisplayName("规则属性测试")
    class RulePropertyTests {
        
        @Test
        @DisplayName("测试规则名称")
        void testRuleName() {
            RouterRule rule = RouterRule.builder()
                .name("my-custom-rule")
                .condition("true")
                .targetModel("model-1")
                .build();
            
            assertEquals("my-custom-rule", rule.getName());
        }
        
        @Test
        @DisplayName("测试条件表达式")
        void testConditionExpression() {
            RouterRule rule = RouterRule.builder()
                .name("condition-test")
                .condition("prompt.messages[0].text.length() > 100")
                .targetModel("model-1")
                .build();
            
            assertEquals("prompt.messages[0].text.length() > 100", rule.getCondition());
        }
        
        @Test
        @DisplayName("测试目标模型名称")
        void testTargetModel() {
            RouterRule rule = RouterRule.builder()
                .name("target-test")
                .condition("true")
                .targetModel("gpt-4-backup")
                .build();
            
            assertEquals("gpt-4-backup", rule.getTargetModel());
        }
        
        @Test
        @DisplayName("测试优先级设置")
        void testPriority() {
            RouterRule lowPriority = RouterRule.builder()
                .name("low-priority")
                .condition("true")
                .targetModel("model-1")
                .priority(1000)
                .build();
            
            assertEquals(1000, lowPriority.getPriority());
            
            RouterRule highPriority = RouterRule.builder()
                .name("high-priority")
                .condition("true")
                .targetModel("model-1")
                .priority(1)
                .build();
            
            assertEquals(1, highPriority.getPriority());
        }
    }
    
    @Nested
    @DisplayName("Getter和Setter测试")
    class GetterSetterTests {
        
        @Test
        @DisplayName("测试Setter和Getter")
        void testSettersAndGetters() {
            RouterRule rule = RouterRule.builder()
                .name("original")
                .condition("false")
                .targetModel("old-model")
                .priority(50)
                .build();
            
            rule.setName("updated");
            rule.setCondition("true");
            rule.setTargetModel("new-model");
            rule.setPriority(10);
            
            assertEquals("updated", rule.getName());
            assertEquals("true", rule.getCondition());
            assertEquals("new-model", rule.getTargetModel());
            assertEquals(10, rule.getPriority());
        }
    }
    
    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {
        
        @Test
        @DisplayName("测试优先级为0")
        void testPriorityZero() {
            RouterRule rule = RouterRule.builder()
                .name("zero-priority")
                .condition("true")
                .targetModel("model-1")
                .priority(0)
                .build();
            
            assertEquals(0, rule.getPriority());
        }
        
        @Test
        @DisplayName("测试优先级为负数")
        void testNegativePriority() {
            RouterRule rule = RouterRule.builder()
                .name("negative-priority")
                .condition("true")
                .targetModel("model-1")
                .priority(-1)
                .build();
            
            assertEquals(-1, rule.getPriority());
        }
        
        @Test
        @DisplayName("测试优先级为最大值")
        void testMaxPriority() {
            RouterRule rule = RouterRule.builder()
                .name("max-priority")
                .condition("true")
                .targetModel("model-1")
                .priority(Integer.MAX_VALUE)
                .build();
            
            assertEquals(Integer.MAX_VALUE, rule.getPriority());
        }
        
        @Test
        @DisplayName("测试空条件表达式")
        void testEmptyCondition() {
            RouterRule rule = RouterRule.builder()
                .name("empty-condition")
                .condition("")
                .targetModel("model-1")
                .build();
            
            assertEquals("", rule.getCondition());
        }
        
        @Test
        @DisplayName("测试空目标模型名称")
        void testEmptyTargetModel() {
            RouterRule rule = RouterRule.builder()
                .name("empty-target")
                .condition("true")
                .targetModel("")
                .build();
            
            assertEquals("", rule.getTargetModel());
        }
        
        @Test
        @DisplayName("测试null条件表达式")
        void testNullCondition() {
            RouterRule rule = RouterRule.builder()
                .name("null-condition")
                .condition(null)
                .targetModel("model-1")
                .build();
            
            assertNull(rule.getCondition());
        }
        
        @Test
        @DisplayName("测试null目标模型名称")
        void testNullTargetModel() {
            RouterRule rule = RouterRule.builder()
                .name("null-target")
                .condition("true")
                .targetModel(null)
                .build();
            
            assertNull(rule.getTargetModel());
        }
    }
    
    @Nested
    @DisplayName("规则比较测试")
    class RuleComparisonTests {
        
        @Test
        @DisplayName("测试不同优先级的规则独立性")
        void testDifferentPriorityRules() {
            RouterRule highPriority = RouterRule.builder()
                .name("high")
                .condition("true")
                .targetModel("model-1")
                .priority(1)
                .build();
            
            RouterRule lowPriority = RouterRule.builder()
                .name("low")
                .condition("true")
                .targetModel("model-2")
                .priority(100)
                .build();
            
            assertNotSame(highPriority, lowPriority);
            assertTrue(highPriority.getPriority() < lowPriority.getPriority());
        }
        
        @Test
        @DisplayName("测试相同属性的规则独立性")
        void testIdenticalRulesIndependence() {
            RouterRule rule1 = RouterRule.builder()
                .name("same")
                .condition("true")
                .targetModel("model-1")
                .priority(50)
                .build();
            
            RouterRule rule2 = RouterRule.builder()
                .name("same")
                .condition("true")
                .targetModel("model-1")
                .priority(50)
                .build();
            
            assertNotSame(rule1, rule2);
            assertEquals(rule1.getName(), rule2.getName());
            assertEquals(rule1.getCondition(), rule2.getCondition());
            assertEquals(rule1.getTargetModel(), rule2.getTargetModel());
            assertEquals(rule1.getPriority(), rule2.getPriority());
        }
    }
}
