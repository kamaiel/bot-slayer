package algorithms;

import java.util.ArrayList;

import characteristics.IRadarResult;
import characteristics.Parameters;
import grafcet.IState;
import grafcet.State;
import robotsimulator.Brain;

public class MyBrainSlayer extends Brain {

    private IState currentState;
    private IState INIT;
    private IState INIT_POS_ROCKY;
    private IState INIT_POS_MARIO;
    private IState MOVETO_INIT_POS_ROCKY;
    private IState MOVETO_INIT_POS_MARIO;
    private IState TURNEAST_SECONDARY;
    private IState TURNWEST_SECONDARY;
    private IState MOVEEAST_SECONDARY;
    private IState MOVEWEST_SECONDARY;
    // private IState BROADCAST_EAST_ENEMY;
    // private IState BROADCAST_WEST_ENEMY;

    private static final int ALPHA = 0x1EADDE;
    private static final int BETA = 0x5EC1;
    private static final int GAMMA = 0x333;
    private static final int ROCKY = 0x1EADDA;
    private static final int MARIO = 0x5EC0;
    private static final int TEAM = 0xBADDAD;

    private static final int MOVEBACK = 0xB52;
    private static final int OVER = 0xC00010FF;
    private static final int HELP = 0xB53;

    private ArrayList<IRadarResult> team, opponents;

    private double myX, myY;
    private double enemyX, enemyY;
    private ArrayList<String> messages;
    private int whoAmI;
    private int waitMoveSecondary = 0;
    private int waitBroadcast = 500;

    public void activate() {
        String[] initPosition = Helpers.initPosition(detectRadar(), getHeading()).split(":");
        whoAmI = Integer.parseInt(initPosition[0]);
        myX = Double.parseDouble(initPosition[1]);
        myY = Double.parseDouble(initPosition[2]);
        currentState = buildGrafcet();
    }

    public void step() {
        currentState = currentState.evalTransitionsCondition();
        myBroadcastReceive();
        currentState.evalState();
    }

    private IState buildGrafcet() {
        INIT = new State(() -> {
        });
        buildSecondary();
        buildRocky();
        buildMario();
        return INIT;
    }

    public void buildSecondary() {
        TURNEAST_SECONDARY = new State(() -> {
            sendLogMessage("Turning east");
            stepTurn(Helpers.getDirection(myX, myY, 3000, myY, getHeading()));
        });
        TURNWEST_SECONDARY = new State(() -> {
            sendLogMessage("Turning west");
            stepTurn(Helpers.getDirection(myX, myY, 0, myY, getHeading()));
            sendLogMessage("nb : " + waitMoveSecondary);
            waitMoveSecondary++;
        });
        MOVEEAST_SECONDARY = new State(() -> {
            sendLogMessage("Secondary scanning : east");
            // sendLogMessage("myX=" + myX + "myY" + myY);
            // waitBroadcast++;
            if (Helpers.isSameDirection(getHeading(), Parameters.EAST)) {
                move();
                myX += Parameters.teamASecondaryBotSpeed * Math.cos(getHeading());
                myY += Parameters.teamASecondaryBotSpeed * Math.sin(getHeading());
            } else {
                moveBack();
                myX -= Parameters.teamASecondaryBotSpeed * Math.cos(getHeading());
                myY -= Parameters.teamASecondaryBotSpeed * Math.sin(getHeading());
            }
        });
        MOVEWEST_SECONDARY = new State(() -> {
            sendLogMessage("Secondary scanning : west");
            // sendLogMessage("myX=" + myX + "myY" + myY);
            waitBroadcast++;
            if (Helpers.isSameDirection(getHeading(), Parameters.WEST)) {
                move();
                myX += Parameters.teamASecondaryBotSpeed * Math.cos(getHeading());
                myY += Parameters.teamASecondaryBotSpeed * Math.sin(getHeading());
            } else {
                moveBack();
                myX -= Parameters.teamASecondaryBotSpeed * Math.cos(getHeading());
                myY -= Parameters.teamASecondaryBotSpeed * Math.sin(getHeading());
            }
        });

        // TURN EAST
        TURNEAST_SECONDARY.addNextState(MOVEEAST_SECONDARY);
        MOVEEAST_SECONDARY.addTransitionCondition(TURNEAST_SECONDARY.getId(),
                () -> Helpers.asTurnInFrontOfPoint(myX, myY, 3000, myY, getHeading()));

        // TURN WEST
        TURNWEST_SECONDARY.addNextState(MOVEWEST_SECONDARY);
        MOVEWEST_SECONDARY.addTransitionCondition(TURNWEST_SECONDARY.getId(),
                () -> Helpers.asTurnInFrontOfPoint(myX, myY, 0, myY, getHeading()) && waitMoveSecondary > 666);

        // MOVE EAST
        MOVEEAST_SECONDARY.addNextState(MOVEWEST_SECONDARY);
        MOVEWEST_SECONDARY.addTransitionCondition(MOVEEAST_SECONDARY.getId(), () -> myX >= 2500
                || Helpers.isFrontRangeOpponent(detectRadar(), myX, myY) == Helpers.EnemyDirection.WEST);

        // MOVE WEST
        MOVEWEST_SECONDARY.addNextState(MOVEEAST_SECONDARY);
        MOVEEAST_SECONDARY.addTransitionCondition(MOVEWEST_SECONDARY.getId(), () -> myX <= 500
                || Helpers.isFrontRangeOpponent(detectRadar(), myX, myY) == Helpers.EnemyDirection.EAST);
    }

    public void buildRocky() {
        INIT_POS_ROCKY = new State(() -> {
            sendLogMessage("Turning to init pos");
            stepTurn(Helpers.getDirection(myX, myY, 500, 500, getHeading()));
        });
        MOVETO_INIT_POS_ROCKY = new State(() -> {
            sendLogMessage("Moving to init pos");
            move();
            myX += Parameters.teamASecondaryBotSpeed * Math.cos(getHeading());
            myY += Parameters.teamASecondaryBotSpeed * Math.sin(getHeading());
        });

        INIT.addNextState(INIT_POS_ROCKY);
        INIT_POS_ROCKY.addTransitionCondition(INIT.getId(), () -> whoAmI == ROCKY);

        INIT_POS_ROCKY.addNextState(MOVETO_INIT_POS_ROCKY);
        MOVETO_INIT_POS_ROCKY.addTransitionCondition(INIT_POS_ROCKY.getId(),
                () -> Helpers.asTurnInFrontOfPoint(myX, myY, 500, 500, getHeading()));

        MOVETO_INIT_POS_ROCKY.addNextState(TURNWEST_SECONDARY);
        TURNWEST_SECONDARY.addTransitionCondition(MOVETO_INIT_POS_ROCKY.getId(), () -> myY <= 500);
    }

    public void buildMario() {
        INIT_POS_MARIO = new State(() -> {
            sendLogMessage("Turning to init pos");
            stepTurn(Helpers.getDirection(myX, myY, 500, 1500, getHeading()));
        });
        MOVETO_INIT_POS_MARIO = new State(() -> {
            sendLogMessage("Moving to init pos");
            move();
            myX += Parameters.teamASecondaryBotSpeed * Math.cos(getHeading());
            myY += Parameters.teamASecondaryBotSpeed * Math.sin(getHeading());
        });

        INIT.addNextState(INIT_POS_MARIO);
        INIT_POS_MARIO.addTransitionCondition(INIT.getId(), () -> whoAmI == MARIO);

        INIT_POS_MARIO.addNextState(MOVETO_INIT_POS_MARIO);
        MOVETO_INIT_POS_MARIO.addTransitionCondition(INIT_POS_MARIO.getId(),
                () -> Helpers.asTurnInFrontOfPoint(myX, myY, 500, 1500, getHeading()));

        MOVETO_INIT_POS_MARIO.addNextState(TURNEAST_SECONDARY);
        TURNEAST_SECONDARY.addTransitionCondition(MOVETO_INIT_POS_MARIO.getId(), () -> myY >= 1500);
    }

    public void myBroadcastReceive() {
        sendLogMessage("Secondary broadcast");
        opponents = Helpers.isRadarOpponent(detectRadar());
        for (IRadarResult o : opponents) {
            double enemyX = myX + o.getObjectDistance() * Math.cos(o.getObjectDirection());
            double enemyY = myY + o.getObjectDistance() * Math.sin(o.getObjectDirection());
            broadcast(whoAmI + ":" + TEAM + ":" + HELP + ":" + enemyX + ":" + enemyY + ":" + OVER);
            messages = fetchAllMessages();
        }
    }
}