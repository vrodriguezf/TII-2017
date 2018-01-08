/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.WFnet_OP;

import APFE.OPResponse;
import java.util.Map;

/**
 *
 * @author victor
 */
@FunctionalInterface
public interface ActionEvaluator {
    public Long getFirstActionTimestamp(OPResponse alertResp, 
            Long startTime, 
            Long endTime,
            Map<String,Object> params);
}
