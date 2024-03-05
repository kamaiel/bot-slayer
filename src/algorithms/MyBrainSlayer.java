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
    private IState BROADCAST_EAST_ENEMY;
    private IState BROADCAST_WEST_ENEMY;

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
    private int waitMoveSecondary=0;

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
        buildSecondary();
        buildRocky();
        buildMario();
        return INIT;
    }

    public void buildSecondary(){
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
            if(Helpers.isSameDirection(getHeading(), Parameters.EAST)){
                move();
                myX+=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
                myY+=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
            }else {
                moveBack();
                myX-=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
                myY-=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
            }
        });      
        MOVEWEST_SECONDARY = new State(() -> {
            sendLogMessage("Secondary scanning : west");
            if(Helpers.isSameDirection(getHeading(), Parameters.WEST)){
                move();
                myX+=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
                myY+=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
            }else{
                moveBack();
                myX-=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
                myY-=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());    
            }
        });  
        BROADCAST_WEST_ENEMY = new State(() -> {
            for (IRadarResult o: opponents){
                double enemyX=myX+o.getObjectDistance()*Math.cos(o.getObjectDirection());
                double enemyY=myY+o.getObjectDistance()*Math.sin(o.getObjectDirection());
                broadcast(whoAmI+":"+TEAM+":"+HELP+":"+enemyX+":"+enemyY+":"+OVER);
                messages = fetchAllMessages();
            }
        });

        BROADCAST_EAST_ENEMY = new State(() -> {
            for (IRadarResult o: opponents){
                double enemyX=myX+o.getObjectDistance()*Math.cos(o.getObjectDirection());
                double enemyY=myY+o.getObjectDistance()*Math.sin(o.getObjectDirection());
                broadcast(whoAmI+":"+TEAM+":"+HELP+":"+enemyX+":"+enemyY+":"+OVER);
                messages = fetchAllMessages();
            }
        });

        TURNEAST_SECONDARY.addNextState(MOVEEAST_SECONDARY);
        MOVEEAST_SECONDARY.addTransitionCondition(TURNEAST_SECONDARY.getId(), () -> Helpers.asTurnInFrontOfPoint(myX,myY, 3000, myY,getHeading()));
        
        TURNWEST_SECONDARY.addNextState(MOVEWEST_SECONDARY);
        MOVEWEST_SECONDARY.addTransitionCondition(TURNWEST_SECONDARY.getId(), () -> Helpers.asTurnInFrontOfPoint(myX,myY, 0, myY,getHeading()) && waitMoveSecondary>666);

        MOVEEAST_SECONDARY.addNextState(MOVEWEST_SECONDARY);
        MOVEEAST_SECONDARY.addNextState(BROADCAST_EAST_ENEMY);
        MOVEWEST_SECONDARY.addTransitionCondition(MOVEEAST_SECONDARY.getId(), () -> myX>=2500);
        
        // broadcast from east
        BROADCAST_EAST_ENEMY.addTransitionCondition(MOVEEAST_SECONDARY.getId(), () -> {
            if (opponents!=null && opponents.size()!=0) return false;
            opponents = Helpers.isRadarOpponent(detectRadar());
            return opponents.size()!=0;
        });
        BROADCAST_EAST_ENEMY.addNextState(MOVEWEST_SECONDARY);
        // move west when broadcast
        MOVEWEST_SECONDARY.addTransitionCondition(BROADCAST_EAST_ENEMY.getId(), () -> true);
        
        MOVEWEST_SECONDARY.addNextState(MOVEEAST_SECONDARY);
        MOVEWEST_SECONDARY.addNextState(BROADCAST_WEST_ENEMY);
        MOVEEAST_SECONDARY.addTransitionCondition(MOVEWEST_SECONDARY.getId(), () -> myX<=500);

        // broadcast from west
        BROADCAST_WEST_ENEMY.addTransitionCondition(MOVEWEST_SECONDARY.getId(), () -> {
            if (opponents!=null && opponents.size()!=0) return false;
            opponents = Helpers.isRadarOpponent(detectRadar());
            return opponents.size()!=0;
        });
        BROADCAST_WEST_ENEMY.addNextState(MOVEEAST_SECONDARY);
        // move east when broadcast
        MOVEEAST_SECONDARY.addTransitionCondition(BROADCAST_WEST_ENEMY.getId(), () -> true);
    }

    public void buildRocky(){
        INIT_POS_ROCKY = new State(() -> {
            sendLogMessage("Turning to init pos");
            stepTurn(Helpers.getDirection(myX, myY, 500, 500, getHeading()));
        });
        MOVETO_INIT_POS_ROCKY = new State(() -> {
            sendLogMessage("Moving to init pos");
            move();
            myX+=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
            myY+=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
        });

        INIT.addNextState(INIT_POS_ROCKY);
        INIT_POS_ROCKY.addTransitionCondition(INIT.getId(), () -> whoAmI == ROCKY);

        INIT_POS_ROCKY.addNextState(MOVETO_INIT_POS_ROCKY);
        MOVETO_INIT_POS_ROCKY.addTransitionCondition(INIT_POS_ROCKY.getId(), () -> Helpers.asTurnInFrontOfPoint(myX,myY,500,500,getHeading()));

        MOVETO_INIT_POS_ROCKY.addNextState(TURNWEST_SECONDARY);
        TURNWEST_SECONDARY.addTransitionCondition(MOVETO_INIT_POS_ROCKY.getId(), () -> myY<=500);
    }

    public void buildMario() {
        INIT_POS_MARIO = new State(() -> {
            sendLogMessage("Turning to init pos");
            stepTurn(Helpers.getDirection(myX, myY, 500, 1500, getHeading()));
        });
        MOVETO_INIT_POS_MARIO = new State(() -> {
            sendLogMessage("Moving to init pos");
            move();
            myX+=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
            myY+=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
        });

        INIT.addNextState(INIT_POS_MARIO);
        INIT_POS_MARIO.addTransitionCondition(INIT.getId(), () -> whoAmI == MARIO);

        INIT_POS_MARIO.addNextState(MOVETO_INIT_POS_MARIO);
        MOVETO_INIT_POS_MARIO.addTransitionCondition(INIT_POS_MARIO.getId(), () -> Helpers.asTurnInFrontOfPoint(myX, myY, 500, 1500, getHeading()));
        
        MOVETO_INIT_POS_MARIO.addNextState(TURNEAST_SECONDARY);
        TURNEAST_SECONDARY.addTransitionCondition(MOVETO_INIT_POS_MARIO.getId(), () ->  myY>=1500);
    }
}