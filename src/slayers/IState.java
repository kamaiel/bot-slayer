package slayers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BooleanSupplier;

public interface IState {

    public Integer getId();

    public HashMap<Integer, BooleanSupplier> getTransitionConditions();

    public Runnable getAction();

    public ArrayList<IState> getNextSates();

    public void evalState();

    public IState evalTransitionsCondition();

    public void addTransitionCondition(Integer stateId, BooleanSupplier supplier);

    public void addNextState(IState... state);
}