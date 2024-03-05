package algorithms;

import robotsimulator.Brain;
import characteristics.IRadarResult;
import characteristics.Parameters;
import grafcet.IState;
import grafcet.State;

public class MyBrainStage5 extends Brain {
    IState INIT;
    IState MOVETOPOINT;
    IState TURNAROUND;
    IState TURNINGMOVE;

    private static final int ROCKY = 0x1EADDA;
    private static final int MARIO = 0x5EC0;

    private static final double targetX = 2000;
    private static final double targetY = 500;

    // ---VARIABLES---//
    IState currentState;
    private double myX;
    private double myY;
    private int whoAmI;
    

    public MyBrainStage5() {
        super();
    }

    public void activate() {
        whoAmI = ROCKY;
        for (IRadarResult o : detectRadar())
            if (Helpers.isSameDirection(o.getObjectDirection(), Parameters.NORTH))
                whoAmI = MARIO;

        if (whoAmI == MARIO) {
            myX = Parameters.teamASecondaryBot2InitX;
            myY = Parameters.teamASecondaryBot2InitY;
        } else {
            myX = Parameters.teamASecondaryBot1InitX;
            myY = Parameters.teamASecondaryBot1InitY;
        }
        currentState = buildGrafcet();
    }

    public void step() {
        if (whoAmI == ROCKY) {
            currentState = currentState.evalTransitionsCondition();
            currentState.evalState();
        }

    }

    public IState buildGrafcet() {
        INIT = new State(() -> {
            stepTurn(Parameters.Direction.LEFT);
        });
        MOVETOPOINT = new State(() -> {
            move();
            myX+=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
            myY+=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
        });
        TURNAROUND = new State(() -> {
            stepTurn(Parameters.Direction.LEFT);
        });
        TURNINGMOVE = new State(() -> {
            move();
        });

        INIT.addNextState(MOVETOPOINT);
        MOVETOPOINT.addTransitionCondition(INIT.getId(),
                () -> Helpers.asTurnInFrontOfPoint(myX, myY, targetX, targetY, getHeading()));

        MOVETOPOINT.addNextState(TURNAROUND);
        TURNAROUND.addTransitionCondition(MOVETOPOINT.getId(), () -> (myX >= targetX && myY >= targetY));

        TURNAROUND.addNextState(TURNINGMOVE);
        TURNINGMOVE.addTransitionCondition(TURNAROUND.getId(), () -> true);

        TURNINGMOVE.addNextState(TURNAROUND);
        TURNAROUND.addTransitionCondition(TURNINGMOVE.getId(), () -> true);

        return INIT;
    }
}
