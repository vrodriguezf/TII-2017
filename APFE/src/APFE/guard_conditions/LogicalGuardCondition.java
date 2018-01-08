/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.guard_conditions;

import APFE.data_log.LogEntry;
import static APFE.utils.Maps.entriesToMap;
import static APFE.utils.Maps.entry;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author victor
 */
public class LogicalGuardCondition extends GuardCondition {

    private static final Logger LOG = Logger.getLogger(LogicalGuardCondition.class.getName());
    public enum TypeOfLogic {ANY_MATCH, ALL_MATCH};
    
    /**
     * Constants
     */
    private static final Map<TypeOfLogic,TypeOfLogic> complementaryLogics = 
            Collections.unmodifiableMap(
                    Stream.of(
                            entry(TypeOfLogic.ANY_MATCH, TypeOfLogic.ALL_MATCH),
                            entry(TypeOfLogic.ALL_MATCH, TypeOfLogic.ANY_MATCH))
                            .collect(entriesToMap())
            );

    /**
     * Statics
     */
    public static class LogicalExpression {
        
        public LogicalExpression(String varname, 
                                    String operator, 
                                    Object value) {
            this.varname = varname;
            this.operator = operator;
            this.value = value;
        }
        
        public LogicalExpression getComplementary() {
            String complementaryOperator;
            
            switch (this.getOperator()) {
                case "=":
                    complementaryOperator = "!=";
                    break;
                case "!=":
                    complementaryOperator = "=";
                    break;
                case ">":
                    complementaryOperator = "<";
                    break;
                case "<":
                    complementaryOperator = ">";
                    break;
                default:
                    LOG.log(Level.SEVERE, "Unrecognized operator {0}", 
                            new Object[]{this.getOperator()});
                    complementaryOperator = this.getOperator();
            }
            
            return new LogicalExpression(varname, complementaryOperator, value);
        }
        
        /**
         * Getters & Setters
         */
        
        public String getVarname() {
            return this.varname;
        }
        
        public String getOperator() {
            return this.operator;
        }
        
        public Object getValue() {
            return this.value;
        }
        
        public Double getValueAsDouble() {
            return (Double) value;
        }
        
        public Integer getValueAsInteger() {
            /*
            Double aux = getValueAsDouble();
            if (aux != null) 
                return aux.intValue();
            else
                return null;
            */
            return (Integer) value;
        }
    
        public Boolean getValueAsBoolean() {
            return (Boolean) value;
        }

        public String getValueAsString() {
            return String.valueOf(value);
        }
        
        /**
         * Attributes
         */
        private String varname;
        private String operator;
        private Object value;
    }

    public LogicalGuardCondition(Collection<LogicalExpression> logicalExpressions, TypeOfLogic typeOfLogic) {
        this.logicalExpressions = logicalExpressions;
        this.typeOfLogic = typeOfLogic;
    }

    /**
     * Constructor
     * @param logicalExpressions 
     */
    public LogicalGuardCondition(Collection<LogicalExpression> logicalExpressions) {
        this(logicalExpressions,TypeOfLogic.ANY_MATCH);
    }
    

    @Override
    public Collection<String> getInvolvedVariables() {
        return this.logicalExpressions.stream()
                .map(LogicalExpression::getVarname)
                .distinct()
                .collect(Collectors.toSet());
    }
    
    @Override
    /**
     * Evaluate a set of log entries against a set of logical expressions,
     * Each logical expression in the collection is implicitly linked to each other
     * by an AND logical door. It is not possible to express OR statements.
     * A logical expression is defined as a triple <varname, operator, value>, for
     * example <"foo","=", 3>. Valid operators are ""
     * @param logEntries
     * @return 
     */
    public boolean evaluate(Collection<LogEntry> logEntries) {
        List<Boolean> expressionResults;
        
        expressionResults = this.getLogicalExpressions().stream()
                .map((LogicalExpression le) -> {
                    Boolean expressionResult = false;
                    Collection<LogEntry> varLogEntries = logEntries.stream()
                            .filter(e -> e.getVarname().equals(le.getVarname()))
                            .collect(Collectors.toSet());
                    
                    if (le.getValue() instanceof Integer) {
                        expressionResult = evaluate_(
                                varLogEntries.stream()
                                    .filter(e -> e.getRecord() instanceof Integer)
                                    .map(LogEntry::getRecordAsInteger)
                                    .collect(Collectors.toList()),
                                le.getOperator(),
                                le.getValueAsInteger()
                        );
                    } else if (le.getValue() instanceof Double) {
                        expressionResult = evaluate_(
                                varLogEntries.stream()
                                    .filter(e -> e.getRecord() instanceof Double)
                                    .map(LogEntry::getRecordAsDouble)
                                    .collect(Collectors.toList()),
                                le.getOperator(),
                                le.getValueAsDouble()
                        );
                    } else if (le.getValue() instanceof Boolean) {
                        expressionResult = evaluate_(
                                varLogEntries.stream()
                                    .filter(e -> e.getRecord() instanceof Boolean)
                                    .map(LogEntry::getRecordAsBoolean)
                                    .collect(Collectors.toList()),
                                le.getOperator(),
                                le.getValueAsBoolean()
                        );
                    } else if (le.getValue() instanceof String) {
                        expressionResult = evaluate_(
                                varLogEntries.stream()
                                    .filter(e -> e.getRecord() instanceof String)
                                    .map(LogEntry::getRecordAsString)
                                    .collect(Collectors.toList()),
                                le.getOperator(),
                                le.getValueAsString()
                        );
                    } else {
                        LOG.log(Level.SEVERE, "NOT ALLOWED LOGICAL EXPRESSION VALUE TYPE [{0}] USE ONLY  INT,DOUBLE,STRING OR BOOL", le.getValue());
                        return false;
                    }
                    
                    return expressionResult;
                })
                .collect(Collectors.toList());
        
        // Every condition must be fulfilled
        boolean allExpressionsFulfilled = expressionResults.stream()
                .allMatch(x -> x == true);
        
        return allExpressionsFulfilled;
    }
    
    /**
     * Complementary of a logical guard
     * @return 
     */
    public GuardCondition getComplementary() {
        return new LogicalGuardCondition(
            this.getLogicalExpressions().stream()
                .map((LogicalExpression le) -> le.getComplementary())
                .collect(Collectors.toSet()),
            complementaryLogics.get(this.getTypeOfLogic())
        );
    }

    /**
     * Getters & Setters
     */
    public Collection<LogicalExpression> getLogicalExpressions() {
        return logicalExpressions;
    }

    public TypeOfLogic getTypeOfLogic() {
        return typeOfLogic;
    }
    
    /**
     * Attributes
     */
    private Collection<LogicalExpression> logicalExpressions;
    private TypeOfLogic typeOfLogic;
    
    /**
     * Private methods
     */
    
    private <T extends Comparable<T>> boolean evaluate_(
                                    Collection<T> records,
                                    String operator,
                                    T value) {
        
        //Choose the predicate to apply to each record
        Predicate<T> pred;
        switch (operator) {
            case "=":
                pred = r -> r.equals(value);
                break;
            case "!=":
                pred = r -> !r.equals(value);
                break;
            case "<":
                pred = r -> (r.compareTo(value) < 0);
                break;
            case ">":
                pred = r -> (r.compareTo(value) > 0);
                break;
            default:
                throw new IllegalArgumentException(
                        "Invalid operator for double logical expressions [" + 
                                operator + "]");
        }
        
        //Choose the logic to apply to the collection
        Function<Stream<T>,Boolean> f = null;
        switch (this.getTypeOfLogic()) {
            case ALL_MATCH:
                f = s -> s.allMatch(pred);
                break;
            case ANY_MATCH:
                f = s -> s.anyMatch(pred);
                break;
        }
        
        return f.apply(records.stream());
    }
}
