package algorithms;

import characteristics.IRadarResult;
import characteristics.Parameters;
import robotsimulator.Brain;
import slayers.Helpers;
import slayers.IState;
import slayers.State;

public class MyBrainStage4 extends Brain {
    IState INIT;
    IState GOSOUTH;
    IState TURNEAST;
    IState GOEAST;
    IState RDV;

    IState currentState;

    private static final int ROCKY = 0x1EADDA;
    private static final int MARIO = 0x5EC0;

    // ---VARIABLES---//
    private double myX;
    private int whoAmI;

    public MyBrainStage4() {
        super();
    }

    public void activate() {
        // ODOMETRY CODE
        whoAmI = ROCKY;
        for (IRadarResult o : detectRadar())
            if (Helpers.isSameDirection(o.getObjectDirection(), Parameters.NORTH))
                whoAmI = MARIO;
        if (whoAmI == MARIO) {
            myX = Parameters.teamASecondaryBot2InitX;
        } else {
            myX = 0;
        }
        currentState = buildGrafcet();
    }

    public void step() {
        currentState.evalState();
        currentState = currentState.evalTransitionsCondition();
    }

    private IState buildGrafcet() {
        INIT = new State(() -> {
            stepTurn(Parameters.Direction.LEFT);
        });
        GOSOUTH = new State(() -> {
            move();
        });
        TURNEAST = new State(() -> {
            stepTurn(Parameters.Direction.LEFT);
            myX=0;
        });
        GOEAST = new State(() -> {
            move();
            myX+=Parameters.teamASecondaryBotSpeed;
        });
        RDV = new State(() -> {
        });

        INIT.addNextState(GOSOUTH);
        GOSOUTH.addTransitionCondition(INIT.getId(), () -> Helpers.isSameDirection(getHeading(), Parameters.SOUTH));

        GOSOUTH.addNextState(TURNEAST);
        TURNEAST.addTransitionCondition(GOSOUTH.getId(), () -> Helpers.isFrontWall(detectFront().getObjectType()));

        TURNEAST.addNextState(GOEAST);
        GOEAST.addTransitionCondition(TURNEAST.getId(), () -> Helpers.isSameDirection(getHeading(), Parameters.EAST));

        GOEAST.addNextState(RDV);
        RDV.addTransitionCondition(GOEAST.getId(), () -> stop());

        return INIT;
    }

    private boolean stop(){
        return ((whoAmI==ROCKY && myX>=1000) || (whoAmI==MARIO && myX>=1500));
    }
}
