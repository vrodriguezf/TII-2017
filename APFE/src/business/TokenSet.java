/* Copyright Guillem Catala. www.guillemcatala.com/petrinetsim. Licensed http://creativecommons.org/licenses/by-nc-sa/3.0/ */
package business;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author Guillem
 */
public class TokenSet extends AbstractCollection {

    private static final Logger LOG = Logger.getLogger(TokenSet.class.getName());

    private ArrayList tokenList = new ArrayList();

    public TokenSet() {
    }

    public TokenSet(Object object) {
        if (object instanceof Token) {
            tokenList.add(object);
        } else if (object instanceof TokenSet) {
            tokenList.addAll((TokenSet) object);
        } else {
            tokenList.add(new Token(object));
        }
    }

    public TokenSet(Object object, long time) {
        if (object instanceof TokenSet) {
            this.addAll((TokenSet) object);
        } else {
            tokenList.add(new Token(object, time));
        }
    }

    public TokenSet(Object object, String initialMarkingExpression) {
        tokenList.add(new Token(object, 0, initialMarkingExpression));
    }

    public TokenSet(Object object, long timestamp, String initialMarkingExpression) {
        tokenList.add(new Token(object, timestamp, initialMarkingExpression));
    }

    @Override
    public Iterator iterator() {
        return tokenList.iterator();
    }

    @Override
    public int size() {
        return tokenList.size();
    }

    @Override
    public boolean add(Object token) {
        return tokenList.add(token);
    }

    @Override
    public boolean addAll(Collection tokenSet) {
        boolean b = tokenList.addAll(((TokenSet) tokenSet).getTokenList());
        return b;
    }

    public ArrayList getTokenList() {
        return this.tokenList;
    }
    
    public Set<Token> toSet() {
        return new HashSet<Token>(this.tokenList);
    }

    public Token get(int id) {
        return (Token) tokenList.get(id);
    }

    @Override
    public boolean remove(Object o) {
        return tokenList.remove(o);
    }

    @Override
    public boolean contains(Object o) {
        return tokenList.contains(o); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * if(exists at least one timed token and his time <= GLOBALTIME) true
     */
    public boolean containsTime(long timestamp) {
        boolean found = false;
        boolean allzero = true;
        Iterator it = tokenList.iterator();
        while (it.hasNext()) {
            Token token = (Token) it.next();
            if (token.getTimestamp() <= timestamp) {
                found = true;
            }
            if (token.getTimestamp() != 0) {
                allzero = false;
            }
        }

        if (allzero || found) {
            return true;
        } else {
            return false;
        }
    }

    /** Increments timed tokens by a fixed amount */
    public void incrementTime(long timestamp) {
        Iterator it = tokenList.iterator();
        while (it.hasNext()) {
            Token token = (Token) it.next();
            if (token.getTimestamp() != 0) {
                token.setTimestamp(token.getTimestamp() + timestamp);
            }
        }
    }

    @Override
    public boolean removeAll(Collection c) {
        Iterator it = tokenList.iterator();
        while (it.hasNext()) {
            Token i = (Token) it.next();
            Iterator it2 = c.iterator();
            while (it2.hasNext()) {
                Token j = (Token) it2.next();
                if (j.equals(i)) {
                    it.remove();
                    it2.remove();
                }
            }
        }
        return true;
    }

    @Override
    public void clear() {
        tokenList.clear();
    }

    @Override
    public String toString() {
        return "TokenSet{" + "tokenList=" + tokenList + '}';
    }
}
