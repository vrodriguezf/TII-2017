/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.WFnet_OP;

import business.Token;
import java.util.Objects;

/**
 *
 * @author victor
 */
public class Firing {
    private Token token;
    private business.Transition transition;
    
    //Additional parameter (optional)
    private Long duration;

    public Firing(Token token, business.Transition transition) {
        this.token = token;
        this.transition = transition;
    }

    public Token getToken() {
        return token;
    }

    public business.Transition getTransition() {
        return transition;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public void setTransition(business.Transition transition) {
        this.transition = transition;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + Objects.hashCode(this.token);
        hash = 53 * hash + Objects.hashCode(this.transition);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Firing other = (Firing) obj;
        if (!Objects.equals(this.token, other.token)) {
            return false;
        }
        if (!Objects.equals(this.transition, other.transition)) {
            return false;
        }
        return true;
    }
}
