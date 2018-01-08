/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.output;

import APFE.OPTriggering;
import APFE.OPResponse;
import APFE.DBConf;
import APFE.Operation;
import APFE.ProcedureExecution;
import APFE.WFnet_OP.Deadlock;
import APFE.WFnet_OP.Firing;
import APFE.WFnet_OP.MarkingOld;
import APFE.evaluation.EvaluationResult;
import APFE.evaluation.Overreaction;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bson.Document;

/**
 *
 * @author victor
 */
public class OutputManager {

    private static final Logger LOG = Logger.getLogger(OutputManager.class.getName());
    
    public static void saveEvaluationResults(Operation op, Map<OPTriggering,EvaluationResult> evaluations) {
        //MONGO DB
        DBConf.currentDB.getCollection(DBConf.MBPFE_COLLECTION_NAME).insertOne(new Document()
                .append("operation", 
                        new Document()
                        .append("id", op.getId())
                        .append("logFilePath", op.getLog().getId())
                        .append("startTime", op.getStartTime())
                        .append("endTime", op.getEndTime())
                )
                .append("operator", 
                        new Document()
                        .append("id",op.getOperator().getId())
                )                
                .append("evaluations",
                        evaluations.entrySet()
                                .stream()
                                .map((Map.Entry<OPTriggering, EvaluationResult> entry) -> {
                                   OPTriggering alert = entry.getKey();
                                   EvaluationResult ev = entry.getValue();

                                   return new Document()
                                           .append("alert",
                                                   new Document()
                                                    .append("alertId", alert.getId())
                                                   .append("startTime", alert.getTriggerTs())
                                                   .append("endTime", alert.getEndTime())
                                                   .append("params", new Document())
                                           )
                                           .append("eop",
                                                   new Document()
                                                   .append("eopId",alert.getWFNetOP().getClass().getSimpleName())
                                           )
                                           .append("traces",
                                                   new Document()
                                                   .append("firings", 
                                                        ev.getForwardExecutionTraces()
                                                           .stream()
                                                           .map((ArrayList<ProcedureExecution> forwards) -> {
                                                               return forwards
                                                                       .stream()
                                                                       .map((ProcedureExecution pe) -> {
                                                                            return pe.getFiringTrace()
                                                                                    .stream()
                                                                                    .map((Firing f) -> {
                                                                                         return new Document()
                                                                                                 .append("id", f.getTransition().getId())
                                                                                                 .append("label",f.getTransition().getLabel())
                                                                                                 .append("UAV",((OPResponse) f.getToken().getObject()).getFocusedUAV())
                                                                                                 .append("timestamp",f.getToken().getTimestamp())
                                                                                                 .append("duration",f.getDuration())
                                                                                                 ;                                                                               
                                                                                    })
                                                                                    .collect(Collectors.toList());                                                                           
                                                                                    }
                                                                       )
                                                                       .collect(Collectors.toList());
                                                           })
                                                           .collect(Collectors.toList())                                                           
                                                    )
                                                   .append("markings",
                                                        ev.getForwardExecutionTraces()
                                                           .stream()
                                                            .map((ArrayList<ProcedureExecution> forwards) -> {
                                                                return forwards
                                                                        .stream()
                                                                        .map((ProcedureExecution pe) -> {
                                                                            return pe.getMarkingTrace()
                                                                                    .stream()
                                                                                    .map((MarkingOld m) -> {
                                                                                        return m.toString();
                                                                                    })
                                                                                    .collect(Collectors.toList());
                                                                        })
                                                                        .collect(Collectors.toList());                                                                   
                                                            })
                                                            .collect(Collectors.toList())    
                                                    )
                                           )
                                           .append("metrics",
                                                   /* TODO implement metrics
                                                   new Document()
                                                   .append("reach", Metrics.Reach(ev, alert.getWFNetOP()))
                                                   .append("fastness", Metrics.Fastness(ev,alert.getWFNetOP(),alert))
                                                   .append("informationOrdering", Metrics.InformationOrdering(ev, alert.getWFNetOP()))
                                                   .append("precision", Metrics.Precision(ev, alert.getWFNetOP()))
                                                   */
                                                new Document()
                                                   .append("reach", 0)
                                                   .append("fastness", 0)
                                                   .append("informationOrdering", 0)
                                                   .append("precision", 0)
                                           )
                                           .append("rightActions",
                                                   ev.getRightActions()
                                                           .stream()
                                                           .map((Firing f) -> {
                                                               return new Document()
                                                                       .append("id", f.getTransition().getId())
                                                                       .append("label",f.getTransition().getLabel())
                                                                       .append("UAV",((OPResponse) f.getToken().getObject()).getFocusedUAV())
                                                                       .append("timestamp",f.getToken().getTimestamp())
                                                                       .append("duration",f.getDuration())
                                                                       ;
                                                           })
                                                           .collect(Collectors.toList())
                                           )
                                           .append("missingActions",
                                                   ev.getMissingActions()
                                                        .stream()
                                                        .map((Deadlock d) -> {
                                                            return new Document()
                                                                    .append("id",d.getLockingTransition().getId())
                                                                    .append("label",d.getLockingTransition().getLabel())
                                                                    .append("UAV",((OPResponse) d.getLockedToken().getObject()).getFocusedUAV())
                                                                    .append("timestamp",d.getLockedToken().getTimestamp())
                                                                    ;
                                                        })
                                                        .collect(Collectors.toList())
                                           )
                                           .append("sequentialMismatches",
                                                   ev.getSequentialMismatches()
                                                        .stream()
                                                        .map((Deadlock d) -> {
                                                            return new Document()
                                                                    .append("id",d.getLockingTransition().getId())
                                                                    .append("label",d.getLockingTransition().getLabel())
                                                                    .append("UAV",((OPResponse) d.getLockedToken().getObject()).getFocusedUAV())
                                                                    .append("timestamp",d.getLockedToken().getTimestamp())
                                                                    .append("liberator",(d.getLiberatorFiring() == null) 
                                                                            ? null
                                                                            : new Document()
                                                                            .append("id", d.getLiberatorFiring().getTransition().getId())
                                                                            .append("label",d.getLiberatorFiring().getTransition().getLabel())
                                                                            .append("UAV", ((OPResponse) d.getLiberatorFiring().getToken().getObject()).getFocusedUAV())
                                                                            .append("timestamp",d.getLiberatorFiring().getToken().getTimestamp())
                                                                            .append("duration", d.getLiberatorFiring().getDuration())
                                                                    )
                                                                    ;
                                                        })
                                                        .collect(Collectors.toList())                                                       
                                            )
                                           .append("overreactions",
                                                   ev.getOverreactions()
                                                    .stream()
                                                    .map((Overreaction o) -> {
                                                        return new Document()
                                                                .append("action", 
                                                                        new Document()
                                                                        .append("id", o.getAction().getTransition().getId())
                                                                        .append("label", o.getAction().getTransition().getLabel())
                                                                       .append("UAV",((OPResponse) o.getAction().getToken().getObject()).getFocusedUAV())
                                                                       .append("timestamp",o.getAction().getToken().getTimestamp())
                                                                        .append("duration", o.getAction().getDuration())
                                                                )
                                                                .append("missingCheck",null)
                                                                /*
                                                                .append("missingCheck",
                                                                        new Document()
                                                                        .append("id", o.getMissingCheck().getId())
                                                                        .append("label", o.getMissingCheck().getLabel())
                                                                )*/
                                                                ;
                                                    })
                                                   .collect(Collectors.toList())
                                            )
                                           ;

                                })
                                .collect(Collectors.toList()))
        );
    }
}
