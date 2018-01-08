/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.data_log;

/**
 *
 * @author victor
 */
public class LogEntry {
    protected Long ts;
    protected String varname;
    protected Object record;

    public LogEntry(Long ts, Object record) {
        this.ts = ts;
        this.record = record;
    }

    public LogEntry(Long ts, String varname, Object record) {
        this.ts = ts;
        this.varname = varname;
        this.record = record;
    }

    public Long getTs() {
        return ts;
    }

    public String getVarname() {
        return varname;
    }

    public Object getRecord() {
        return record;
    }
    
    public Double getRecordAsDouble() {
        return (Double) record;
    }
    
    public Integer getRecordAsInteger() {
        /*
        Double aux = getRecordAsDouble();
        if (aux != null) 
            return aux.intValue();
        else
            return null;
*/
        return (Integer) record;
    }
    
    public Boolean getRecordAsBoolean() {
        return (Boolean) record;
    }
    
    public String getRecordAsString() {
        return String.valueOf(record);
    }
}
