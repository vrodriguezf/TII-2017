/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE;

/**
 *
 * @author victor
 */
public class OPResponse {

    public OPTriggering OPTriggering;
    protected Operation operation;
    protected String focusedUAV;

    public OPResponse(OPTriggering alert, Operation operation, String focusedUAV) {
        this.OPTriggering = alert;
        this.operation = operation;
        this.focusedUAV = focusedUAV;
    }
    
    public OPResponse(OPTriggering alert, Operation op) {
        this(alert,op,null);
    }

    public OPTriggering getOPTriggering() {
        return OPTriggering;
    }

    public Operation getOperation() {
        return operation;
    }

    public String getFocusedUAV() {
        return focusedUAV;
    }

    @Override
    public String toString() {
        return "AlertResponse{" + "focusedUAV=" + focusedUAV + '}';
    }
}
