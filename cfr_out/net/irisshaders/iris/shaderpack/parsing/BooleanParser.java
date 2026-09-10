/*
 * Decompiled with CFR 0.152.
 */
package net.irisshaders.iris.shaderpack.parsing;

import java.util.EmptyStackException;
import java.util.Stack;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.shaderpack.option.values.OptionValues;

public class BooleanParser {
    public static boolean parse(String expression, OptionValues valueLookup) {
        try {
            StringBuilder option = new StringBuilder();
            Stack<Operation> operationStack = new Stack<Operation>();
            Stack<Boolean> valueStack = new Stack<Boolean>();
            block10: for (int i = 0; i < expression.length(); ++i) {
                char c = expression.charAt(i);
                switch (c) {
                    case '!': {
                        operationStack.push(Operation.NOT);
                        continue block10;
                    }
                    case '&': {
                        if (!option.isEmpty()) {
                            valueStack.push(BooleanParser.processValue(option.toString(), valueLookup, operationStack));
                            option = new StringBuilder();
                        }
                        if (operationStack.isEmpty() || !operationStack.peek().equals((Object)Operation.AND)) {
                            operationStack.push(Operation.OPEN);
                        }
                        ++i;
                        operationStack.push(Operation.AND);
                        continue block10;
                    }
                    case '|': {
                        if (!option.isEmpty()) {
                            valueStack.push(BooleanParser.processValue(option.toString(), valueLookup, operationStack));
                            option = new StringBuilder();
                        }
                        if (!operationStack.isEmpty() && operationStack.peek().equals((Object)Operation.AND)) {
                            BooleanParser.evaluate(operationStack, valueStack, true);
                        }
                        ++i;
                        operationStack.push(Operation.OR);
                        continue block10;
                    }
                    case '(': {
                        operationStack.push(Operation.OPEN);
                        continue block10;
                    }
                    case ')': {
                        if (!option.isEmpty()) {
                            valueStack.push(BooleanParser.processValue(option.toString(), valueLookup, operationStack));
                            option = new StringBuilder();
                        }
                        if (!operationStack.isEmpty() && operationStack.peek().equals((Object)Operation.AND)) {
                            BooleanParser.evaluate(operationStack, valueStack, true);
                        }
                        BooleanParser.evaluate(operationStack, valueStack, true);
                        continue block10;
                    }
                    case ' ': {
                        continue block10;
                    }
                    default: {
                        option.append(c);
                    }
                }
            }
            if (!option.isEmpty()) {
                valueStack.push(BooleanParser.processValue(option.toString(), valueLookup, operationStack));
            }
            BooleanParser.evaluate(operationStack, valueStack, false);
            boolean result = valueStack.pop();
            if (!valueStack.isEmpty() || !operationStack.isEmpty()) {
                Iris.logger.warn("Failed to parse the following boolean operation correctly, stacks not empty, defaulting to true!: '{}'", expression);
                return true;
            }
            return result;
        }
        catch (EmptyStackException emptyStackException) {
            Iris.logger.warn("Failed to parse the following boolean operation correctly, stacks empty when it shouldn't, defaulting to true!: '{}'", expression);
            return true;
        }
    }

    private static boolean processValue(String value, OptionValues valueLookup, Stack<Operation> operationStack) {
        boolean booleanValue;
        switch (value) {
            case "true": 
            case "1": {
                boolean bl = true;
                break;
            }
            case "false": 
            case "0": {
                boolean bl = false;
                break;
            }
            default: {
                boolean bl = booleanValue = valueLookup != null && valueLookup.getBooleanValueOrDefault(value);
            }
        }
        if (!operationStack.isEmpty() && operationStack.peek() == Operation.NOT) {
            operationStack.pop();
            return !booleanValue;
        }
        return booleanValue;
    }

    private static void evaluate(Stack<Operation> operationStack, Stack<Boolean> valueStack, boolean currentBracket) {
        boolean value = valueStack.pop();
        while (!(operationStack.isEmpty() || currentBracket && operationStack.peek() == Operation.OPEN)) {
            value = operationStack.pop().compute(value, valueStack);
        }
        if (!operationStack.isEmpty() && operationStack.peek() == Operation.OPEN) {
            operationStack.pop();
            if (!operationStack.isEmpty() && operationStack.peek() == Operation.NOT) {
                value = operationStack.pop().compute(value, valueStack);
            }
        }
        valueStack.push(value);
    }

    private static enum Operation {
        AND{

            @Override
            boolean compute(boolean value, Stack<Boolean> valueStack) {
                return valueStack.pop() != false && value;
            }
        }
        ,
        OR{

            @Override
            boolean compute(boolean value, Stack<Boolean> valueStack) {
                return valueStack.pop() != false || value;
            }
        }
        ,
        NOT{

            @Override
            boolean compute(boolean value, Stack<Boolean> valueStack) {
                return !value;
            }
        }
        ,
        OPEN;


        boolean compute(boolean value, Stack<Boolean> valueStack) {
            return value;
        }
    }
}

