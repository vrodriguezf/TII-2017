/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.guard_conditions;

import APFE.data_log.LogEntry;
import java.util.Collection;

/**
 *
 * @author victor
 */
public class TrivialGuardCondition extends GuardCondition {
    @Override
    public boolean evaluate(Collection<LogEntry> logEntries) {
        return true;
    }
}
