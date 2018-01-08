/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.guard_conditions;

import APFE.data_log.LogEntry;
import java.util.Collection;
import java.util.function.Predicate;

/**
 *
 * @author victor
 */
public abstract class GuardCondition {
    public abstract boolean evaluate(Collection<LogEntry> logEntries);
    
    public Collection<String> getInvolvedVariables() {
        return null;
    }
}
