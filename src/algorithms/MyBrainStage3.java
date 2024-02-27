package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
import grafcet.IState;
import grafcet.State;

public class MyBrainStage3 extends Brain {
    private IState INIT;
    private IState GONORTH;
    private IState TURNINGWEST;
    private IState GOWEST;
    private IState TURNINGEAST;
    private IState GOEAST;
    private IState TURNROUND;
    private IState GOMEETING;
    private IState SINK;

    private int STOP = 0;

    private IState state;
    private int myX;

    public MyBrainStage3() {
        super();
    }

    public void activate() {
        state = buildGrafcet();
    }

    public void step() {
        state.evalState();
        state = state.evalTransitionsCondition();
    }

    private boolean isAtQuarter() {
        return (STOP == 0 && myX >= 1000) || (STOP == 1 && myX >= 1500) || (STOP == 2 && myX >= 2000);
    }

    private IState buildGrafcet() {
        INIT = new State(() -> {
            stepTurn(Parameters.Direction.LEFT);
        });
        GONORTH = new State(() -> {
            move();
        });
        TURNINGWEST = new State(() -> {
            stepTurn(Parameters.Direction.LEFT);
        });
        GOWEST = new State(() -> {
            move();
        });
        TURNINGEAST = new State(() -> {
            stepTurn(Parameters.Direction.LEFT);
            myX = 0;
        });
        GOEAST = new State(() -> {
            move();
            myX += Parameters.teamAMainBotSpeed;
        });
        TURNROUND = new State(() -> {
            stepTurn(Parameters.Direction.LEFT);
        });
        GOMEETING = new State(() -> {
            STOP++;
        });
        SINK = new State(() -> {
        });

        INIT.addNextState(GONORTH);

        GONORTH.addNextState(TURNINGWEST);
        GONORTH.addTransitionCondition(INIT.getId(), () -> Helpers.isSameDirection(getHeading(), Parameters.NORTH));

        TURNINGWEST.addNextState(GOWEST);
        TURNINGWEST.addTransitionCondition(GONORTH.getId(), () -> Helpers.isFrontWall(detectFront().getObjectType()));

        GOWEST.addNextState(TURNINGEAST);
        GOWEST.addTransitionCondition(TURNINGWEST.getId(),
                () -> Helpers.isSameDirection(getHeading(), Parameters.WEST));

        TURNINGEAST.addNextState(GOEAST);
        TURNINGEAST.addTransitionCondition(GOWEST.getId(), () -> Helpers.isFrontWall(detectFront().getObjectType()));

        GOEAST.addNextState(TURNROUND);
        GOEAST.addNextState(SINK);
        GOEAST.addTransitionCondition(TURNINGEAST.getId(),
                () -> Helpers.isSameDirection(getHeading(), Parameters.EAST));
        GOEAST.addTransitionCondition(GOMEETING.getId(), () -> true);

        TURNROUND.addNextState(GOMEETING);
        TURNROUND.addTransitionCondition(GOEAST.getId(), () -> isAtQuarter());
        SINK.addTransitionCondition(GOEAST.getId(), () -> Helpers.isFrontWall(detectFront().getObjectType()));

        GOMEETING.addNextState(GOEAST);
        GOMEETING.addTransitionCondition(TURNROUND.getId(),
                () -> Helpers.isSameDirection(getHeading(), Parameters.EAST));

        return INIT;
    }
}
