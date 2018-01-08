/* Copyright Guillem Catala. www.guillemcatala.com/petrinetsim. Licensed http://creativecommons.org/licenses/by-nc-sa/3.0/ */
package business;

import APFE.WFnet.WorkflowNet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.codehaus.janino.CompileException;
import org.codehaus.janino.Parser;
import org.codehaus.janino.Scanner;
import presentation.FrmViewSource;
import presentation.GUI;

/**
 *
 * @author Guillem
 */
public class Simulation extends Thread {

    private static final Logger LOG = Logger.getLogger(Simulation.class.getName());

    protected boolean step = false;
    protected boolean paused = false;
    protected boolean stop = false;
    /** Time delay between a transition is fired */
    public static int DELAY = 0;
    /** Default time delay between each transition firing process */
    public static int COMPONENTDELAY = 100;
    protected GUI gui;

    /** Initializes Simulation.
     * @param step
     * @param gui 
     */
    public Simulation(boolean step, GUI gui) {
        this.step = step;
        this.gui = gui;
        NetClass n = new NetClass();
        try {
            // If we are in Headless execution, the object Global.PetriNet is already set
            if (!Global.HEADLESS_EXECUTION) {
                n.compile(n.generateNetSource());
            }
        } catch (CompileException | Parser.ParseException | Scanner.ScanException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            this.stop = true;
            if (gui!=null) {
                FrmViewSource jform = new FrmViewSource(JOptionPane.getFrameForComponent(gui), true, e.getMessage(), "Compilation errors");
                jform.setVisible(true);                
            }
            LOG.log(Level.SEVERE,null,e);
        }
    }

    @Override
    public void run() {
        while (!isFinished() && !stop) {
            fireTransition();
            
            //Reset currentCase (if any)
            if (Global.petriNet instanceof WorkflowNet) {
                ((WorkflowNet) Global.petriNet).currentCase.setPassedTransitionGuard(false);
                ((WorkflowNet) Global.petriNet).currentCase = null;
            }
        }
        if (stop) {
            this.gui.getJTextArea1().append("Stopped.\n");
        } else {
            this.gui.getJTextArea1().append("Deadlock.\n");
        }
        this.gui.getJTextArea1().setCaretPosition(this.gui.getJTextArea1().getText().length());
    }

    /** Checks whether the Simulation has ended
     * @return  
     */
    public boolean isFinished() {
        return Global.petriNet.isDead();
    }

    /** Fires a single transition of the enabled transition list */
    protected void fireTransition() {
        enabledTransitionList();
        if (!this.enabledTransitionList().isEmpty()) {
            getRandomTransition().fire(this.gui, 0);

                pauseResumeSimulation();
            
        }
    }

    public synchronized void pauseResumeSimulation() {
        if (step && !stop) {
            paused = true;
            try {
                synchronized (this) {
                    this.wait();
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                Thread.sleep(DELAY);
            } catch (InterruptedException l) {
            }
        }
    }

    /** Returns a random transition from the enabled transition list */
    public Transition getRandomTransition() {
        ArrayList enabledTransitions = this.enabledTransitionList();
        Random generator = new Random();
        int rand = generator.nextInt(enabledTransitions.size());
        return (Transition) enabledTransitions.get(rand);
    }

    /** Returns a list of enabled transitions */
    public ArrayList enabledTransitionList() {
        Iterator it = Global.petriNet.getTransitions().iterator();
        ArrayList enabledTransitions = new ArrayList();
        while (it.hasNext()) {
            Transition transition = (Transition) it.next();
            if (transition.enabled(0)) {
                enabledTransitions.add(transition);
            }
        }
        return enabledTransitions;
    }

    /**
     * @return the step
     */
    public boolean isStep() {
        return step;
    }

    /**
     * @param step the step to set
     */
    public void setStep(boolean step) {
        this.step = step;
    }

    /**
     * @return the paused
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * @param paused the paused to set
     */
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    /**
     * @return the stop
     */
    public boolean isStop() {
        return stop;
    }

    /**
     * @param stop the stop to set
     */
    public void setStop(boolean stop) {
        if (this.paused) {
            synchronized (this) {
                this.notify();
            }
        }
        this.stop = stop;
    }
}
