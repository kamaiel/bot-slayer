package algorithms;

import java.util.ArrayList;

import characteristics.IRadarResult;
import characteristics.Parameters;
import grafcet.IState;
import grafcet.State;
import robotsimulator.Brain;

public class MyBrainSlayer {
    
    private IState currentState;
    private IState INIT;

    private static final int ALPHA = 0x1EADDE;
    private static final int BETA = 0x5EC1;
    private static final int GAMMA = 0x333;
    private static final int ROCKY = 0x1EADDA;
    private static final int MARIO = 0x5EC0;
    private static final int TEAM = 0xBADDAD;

    private static final int MOVEBACK = 0xB52;
    private static final int OVER = 0xC00010FF;

    private ArrayList<IRadarResult> team, opponents;

    private double myX, myY;
    private double enemyX, enemyY;
    private ArrayList<String> messages;
    private int whoAmI;

    public void activate() {
        String[] initPosition = Helpers.initPosition(detectRadar(), getHeading()).split(":");
        whoAmI = Integer.parseInt(initPosition[0]);
        myX = Double.parseDouble(initPosition[1]);
        myY = Double.parseDouble(initPosition[2]);
        currentState = buildGrafcet();
    }

    public void step() {
        currentState = currentState.evalTransitionsCondition();
        currentState.evalState();
    }

    private IState buildGrafcet(){
        INIT = new State(() -> {}); 
        return INIT;
    }
}
