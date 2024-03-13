package algorithms;

import java.util.ArrayList;

import characteristics.IRadarResult;
import characteristics.Parameters;
import grafcet.IState;
import grafcet.State;
import robotsimulator.Brain;

public class MyBrainEx1 extends Brain {

    private IState currentState;
    private IState INIT;
    private IState MOVE;
    private IState BROADCAST;
    private IState TURN;
    private IState RETREAT; 

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
        String[] initPositionAndSpeed = Helpers.initPositionAndSpeed(detectRadar(), getHeading()).split(":");
        whoAmI = Integer.parseInt(initPositionAndSpeed[0]);
        myX = Double.parseDouble(initPositionAndSpeed[1]);
        myY = Double.parseDouble(initPositionAndSpeed[2]);
        currentState = buildGrafcet();
    }

    public void step() {
        currentState = currentState.evalTransitionsCondition();
        currentState.evalState();
    }

    private IState buildGrafcet(){
        INIT = new State(() -> {});
        MOVE = new State(() -> {
            move();
            myX+=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
            myY+=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
        });
        TURN = new State(() -> {stepTurn(Parameters.Direction.RIGHT);});
        RETREAT = new State(() -> {moveBack();});
        BROADCAST = new State(() -> {
            for (IRadarResult o: opponents){
                double enemyX=myX+o.getObjectDistance()*Math.cos(o.getObjectDirection());
                double enemyY=myY+o.getObjectDistance()*Math.sin(o.getObjectDirection());
                broadcast(whoAmI+":"+TEAM+":"+MOVEBACK+":"+enemyX+":"+enemyY+":"+OVER);
                messages = fetchAllMessages();
            }
        });

        INIT.addNextState(MOVE,TURN);
        MOVE.addTransitionCondition(INIT.getId(), () -> whoAmI==MARIO);
        TURN.addTransitionCondition(INIT.getId(), () -> {
            messages = fetchAllMessages();
           return whoAmI!=MARIO && messages.size()>0;
        });
        TURN.addTransitionCondition(BROADCAST.getId(), () -> true);

        MOVE.addNextState(BROADCAST);
        BROADCAST.addTransitionCondition(MOVE.getId(), () -> {
            opponents = Helpers.isRadarOpponent(detectRadar());
            return opponents.size()!=0;
        });
        
        BROADCAST.addNextState(TURN);

        TURN.addNextState(RETREAT);
        RETREAT.addTransitionCondition(TURN.getId(), () -> {
            if (messages!=null && messages.size()>0) {
                for (String m: messages) {
                    enemyX = Double.parseDouble(m.split(":")[3]);
                    enemyY = Double.parseDouble(m.split(":")[4]);
                }
                return Helpers.asTurnInFrontOfPoint(myX, myY, enemyX, enemyY, getHeading());
            } return false;
        });

        return INIT;
    }
}
