/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE;

import APFE.WFnet_OP.WFNetOP;

/**
 *
 * @author victor
 */
public class OPTriggering {
    
    private String id;
    private Long triggerTs;
    private WFNetOP wfNetOP;
    private Long endTime;
    
    public OPTriggering(String id, Long triggerTs, Long endTime, WFNetOP emergencyProcedure) {
        this.id = id;
        this.triggerTs = triggerTs;
        this.endTime = endTime;
        this.wfNetOP = emergencyProcedure;
    }

    public long getTriggerTs() {
        return triggerTs;
    }

    public WFNetOP getWFNetOP() {
        return wfNetOP;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getId() {
        return id;
    }
}
