package io.mayfly.router.impl;

import io.mayfly.core.ModelInstance;
import io.mayfly.core.ModelUnavailableException;
import io.mayfly.router.RouterRule;
import io.mayfly.router.RouterStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 规则路由策略
 * 基于SpEL表达式进行条件路由
 */
@Component
@Slf4j
public class RuleBasedRouterStrategy implements RouterStrategy {
    
    private final ExpressionParser parser = new SpelExpressionParser();
    private List<RouterRule> rules = new ArrayList<>();
    
    public void setRules(List<RouterRule> rules) {
        this.rules = rules != null ? rules : new ArrayList<>();
    }
    
    @Override
    public ModelInstance select(Prompt request, List<ModelInstance> candidates) {
        List<RouterRule> sortedRules = rules.stream()
            .sorted(Comparator.comparingInt(RouterRule::getPriority))
            .collect(Collectors.toList());
        
        for (RouterRule rule : sortedRules) {
            if (matches(rule, request)) {
                String targetModel = rule.getTargetModel();
                return candidates.stream()
                    .filter(m -> m.getConfig().getName().equals(targetModel))
                    .filter(ModelInstance::isAvailable)
                    .findFirst()
                    .orElseThrow(() -> new ModelUnavailableException(
                        "Target model not available: " + targetModel));
            }
        }
        
        throw new ModelUnavailableException("No matching rule found");
    }
    
    private boolean matches(RouterRule rule, Prompt request) {
        try {
            EvaluationContext context = new StandardEvaluationContext();
            context.setVariable("request", request);
            Expression expression = parser.parseExpression(rule.getCondition());
            return Boolean.TRUE.equals(expression.getValue(context, Boolean.class));
        } catch (Exception e) {
            log.warn("Failed to evaluate rule {}: {}", rule.getName(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getName() {
        return "rule-based";
    }
    
    @Override
    public int getOrder() {
        return 50;
    }
}
