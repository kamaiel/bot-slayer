package grafcet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BooleanSupplier;

public class State implements IState {
    private static Integer ID = 0;
    private Integer id;

    private HashMap<Integer, BooleanSupplier> transitionConditions;
    private Runnable action;
    private ArrayList<IState> nextSates;

    public State(Runnable action) { 
        this.id = ID++;
        this.transitionConditions = new HashMap<Integer, BooleanSupplier>();
        this.action = action;
        this.nextSates = new ArrayList<IState>();
    }

    public Integer getId() {
        return id;
    }

    public HashMap<Integer, BooleanSupplier> getTransitionConditions() {
        return transitionConditions;
    }

    public Runnable getAction() {
        return action;
    }

    public ArrayList<IState> getNextSates() {
        return nextSates;
    }

    public void addTransitionCondition(Integer stateId, BooleanSupplier supplier) {
        transitionConditions.put(stateId, supplier);
    }

    public void addNextState(IState... states) {
        for (IState state : states)
            nextSates.add(state);
    }

    public IState evalTransitionsCondition() {
        for (IState state : nextSates) {
            if (state.getTransitionConditions().get(id).getAsBoolean()) {
                return state;
            }
        }
        return this;
    }

    public void evalState() {
        if (action != null)
            action.run();
    }
}
