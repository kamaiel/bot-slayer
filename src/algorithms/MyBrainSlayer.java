package algorithms;

import java.util.ArrayList;

import characteristics.IRadarResult;
import characteristics.IFrontSensorResult;
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
    private IState TURNSOUTH_SECONDARY;
    private IState TURNNORTH_SECONDARY;
    private IState 

    private IState MOVETO_ALPHA;
    private IState TURN_ALPHA;
    private IState MOVETO_BETA;
    private IState TURN_BETA;

    private IState MOVETO_MAIN;
    private IState TURN_MAIN;
    private IState FIRE_MAIN;

    private static final int ALPHA = 0x1EADDE;
    private static final int BETA = 0x5EC1;
    private static final int GAMMA = 0x333;
    private static final int ROCKY = 0x1EADDA;
    private static final int MARIO = 0x5EC0;
    private static final int TEAM = 0xBADDAD;

    private static final int MOVEBACK = 0xB52;
    private static final int OVER = 0xC00010FF;
    private static final int HELP = 0xB53;
    private static final int FOLLOW = 0xB54;

    private static final int FOLLOW_STEP = 100;
    private static int FIRE_TIMEOUT_STEP = 300;

    private ArrayList<IRadarResult> team, opponents;

    private double myX, myY;
    private double followX, followY = -1;
    private double speed;
    private ArrayList<String> messages;
    private int whoAmI;
    private int waitMoveSecondary; // only for secondary robots
    private int whenUpdateFollows; // counter for updating follow coords
    private int lastMessageCode; // last message received
    private int firing; // counter for firing state
    private int following; // which robot to follow
    private int tracking = FIRE_TIMEOUT_STEP; // counter after firing before update follow
    private boolean secondaryHasStartBroadcast = false; // because Main broadcast first

    public void activate() {
        String[] initPositionAndSpeed = Helpers.initPositionAndSpeed(detectRadar(), getHeading()).split(":");
        whoAmI = Integer.parseInt(initPositionAndSpeed[0]);
        myX = Double.parseDouble(initPositionAndSpeed[1]);
        myY = Double.parseDouble(initPositionAndSpeed[2]);
        speed = Double.parseDouble(initPositionAndSpeed[3]);
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
        buildMain();
        return INIT;
    }

    public void buildSecondary() {
        TURNEAST_SECONDARY = new State(() -> {
            if (getHealth() > 0) {
                sendLogMessage("Turning east");
                stepTurn(Helpers.getDirection(myX, myY, 3000, myY, getHeading()));
            }
        });
        TURNWEST_SECONDARY = new State(() -> {
            if (getHealth() > 0) {
                sendLogMessage("Turning west");
                stepTurn(Helpers.getDirection(myX, myY, 0, myY, getHeading()));
                sendLogMessage("nb : " + waitMoveSecondary);
                waitMoveSecondary++;
            }
        });


        MOVEEAST_SECONDARY = new State(() -> {
            if (getHealth() > 0) {
                sendLogMessage("Secondary scanning : east | X: " + String.format("%.2f", myX) + ", Y: "
                        + String.format("%.2f", myY));
                if (Helpers.isSameDirection(getHeading(), Parameters.EAST)) {
                    if (Helpers.isFrontRangeObstacle(detectRadar(), getHeading(), myX, myY) < 0) {
                        move();
                        myX += speed * Math.cos(getHeading());
                        myY += speed * Math.sin(getHeading());
                    }
                } else {
                    if (Helpers.isFrontRangeObstacle(detectRadar(), -getHeading(), myX, myY) < 0) {
                        moveBack();
                        myX -= speed * Math.cos(getHeading());
                        myY -= speed * Math.sin(getHeading());
                    }
                }
            }
        });
        MOVEWEST_SECONDARY = new State(() -> {
            if (getHealth() > 0) {
                sendLogMessage("Secondary scanning : west | X: " + String.format("%.2f", myX) + ", Y: "
                        + String.format("%.2f", myY));
                if (Helpers.isSameDirection(getHeading(), Parameters.WEST)) {
                    if (Helpers.isFrontRangeObstacle(detectRadar(), getHeading(), myX, myY) < 0) {
                        move();
                        myX += speed * Math.cos(getHeading());
                        myY += speed * Math.sin(getHeading());
                    }
                } else {
                    if (Helpers.isFrontRangeObstacle(detectRadar(), -getHeading(), myX, myY) < 0) {
                        moveBack();
                        myX -= speed * Math.cos(getHeading());
                        myY -= speed * Math.sin(getHeading());
                    }
                }
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
            if (getHealth() > 0) {
                sendLogMessage("Turning to init pos");
                stepTurn(Helpers.getDirection(myX, myY, myX, 500, getHeading()));
            }
        });
        MOVETO_INIT_POS_ROCKY = new State(() -> {
            if (getHealth() > 0) {
                sendLogMessage("Moving to init pos");
                if (Helpers.isFrontRangeObstacle(detectRadar(), getHeading(), myX, myY) < 0) {
                    move();
                    myX += speed * Math.cos(getHeading());
                    myY += speed * Math.sin(getHeading());
                }
            }
        });

        INIT.addNextState(INIT_POS_ROCKY);
        INIT_POS_ROCKY.addTransitionCondition(INIT.getId(), () -> whoAmI == ROCKY);

        INIT_POS_ROCKY.addNextState(MOVETO_INIT_POS_ROCKY);
        MOVETO_INIT_POS_ROCKY.addTransitionCondition(INIT_POS_ROCKY.getId(),
                () -> Helpers.asTurnInFrontOfPoint(myX, myY, myX, 500, getHeading()));

        MOVETO_INIT_POS_ROCKY.addNextState(TURNWEST_SECONDARY);
        TURNWEST_SECONDARY.addTransitionCondition(MOVETO_INIT_POS_ROCKY.getId(), () -> myY <= 500);
    }

    public void buildMario() {
        INIT_POS_MARIO = new State(() -> {
            if (getHealth() > 0) {
                // sendLogMessage("Turning to init pos");
                stepTurn(Helpers.getDirection(myX, myY, myX, 1500, getHeading()));
            }
        });
        MOVETO_INIT_POS_MARIO = new State(() -> {
            if (getHealth() > 0) {
                // sendLogMessage("Moving to init pos");
                if (Helpers.isFrontRangeObstacle(detectRadar(), getHeading(), myX, myY) < 0) {
                    move();
                    myX += speed * Math.cos(getHeading());
                    myY += speed * Math.sin(getHeading());
                }
            }
        });

        INIT.addNextState(INIT_POS_MARIO);
        INIT_POS_MARIO.addTransitionCondition(INIT.getId(), () -> whoAmI == MARIO);

        INIT_POS_MARIO.addNextState(MOVETO_INIT_POS_MARIO);
        MOVETO_INIT_POS_MARIO.addTransitionCondition(INIT_POS_MARIO.getId(),
                () -> Helpers.asTurnInFrontOfPoint(myX, myY, myX, 1500, getHeading()));

        MOVETO_INIT_POS_MARIO.addNextState(TURNEAST_SECONDARY);
        TURNEAST_SECONDARY.addTransitionCondition(MOVETO_INIT_POS_MARIO.getId(), () -> myY >= 1500);
    }

    public void buildMain() {

        // Suivre follow (follow init à un robot secondaire)
        // Si help -> diriger vers l'ennemi que follow a détecté
        // While rencontre ennemi ((tirer, reculer)*temps_firing, avancer *
        // temps_tracking)
        // Si plus d'ennemi -> (Si help -> suivre help), (Si follow -> suivre follow),
        // sinon avancer(pattern ?)

        MOVETO_MAIN = new State(() -> {
            if (getHealth() > 0) {
                sendLogMessage("X: " + String.format("%.2f", myX) + ", Y:" + String.format("%.2f", myY) + ", dir: "
                        + getHeading());
                int obstacle = Helpers.isFrontRangeObstacle(detectRadar(), getHeading(),myX,myY);
                if (lastMessageCode==HELP) tracking++;

                if (obstacle < 0) {
                    whenUpdateFollows++;
                    if (lastMessageCode == HELP && whenUpdateFollows % 7 == 0) {
                        boolean canFire = true;
                        double angleFollow = Math.atan2(followY - myY, followX - myX);
                        if (messages != null && messages.size() > 0) {
                            for (String msg : messages) {
                                String[] tokens = msg.split(":");
                                if (Integer.parseInt(tokens[0]) != whoAmI && Integer.parseInt(tokens[2])==FOLLOW) {
                                    double tmpX = Double.parseDouble(tokens[3]);
                                    double tmpY = Double.parseDouble(tokens[4]);
                                    double angle = Math.atan2(tmpY - myY, tmpX - myX);
                                    if (Math.abs(Helpers.normalize(angleFollow) - Helpers.normalize(angle)) < Math.PI/6 || Math.sqrt(Math.pow(myY-followY,2) + Math.pow(myX-followX,2)) > 1000) {
                                        canFire = false;
                                        break;
                                    }
                                }
                            }
                        }
                        if (canFire) {
                            sendLogMessage("FIRE FAR AWAY");
                            fire(angleFollow);
                        }
                    } else {
                        move();
                        myX += speed * Math.cos(getHeading());
                        myY += speed * Math.sin(getHeading());
                    }

                } else if (obstacle == 1 || obstacle == 2) {
                    stepTurn(Parameters.Direction.LEFT);
                } else if (obstacle == 0) {
                    stepTurn(Parameters.Direction.RIGHT);
                } else if (obstacle == 3) {
                    followX = -followX;
                } else {
                    followY = -followY;
                }
            }
        });
        FIRE_MAIN = new State(() -> {
            if (getHealth() > 0) {
                sendLogMessage("FIRE");
                lastMessageCode = 0;
                firing++;
                // if (firing%7==0) {
                IFrontSensorResult o = detectFront();
                if (o.getObjectType() == IFrontSensorResult.Types.OpponentMainBot
                        || o.getObjectType() == IFrontSensorResult.Types.OpponentSecondaryBot)
                    fire(getHeading());

                for (IRadarResult op : opponents) {
                    fire(op.getObjectDirection());
                }
                // } else {
                // if (Helpers.isFrontRangeObstacle(detectRadar(),-getHeading(), myX, myY)<0) {
                // moveBack();
                // myX -= speed * Math.cos(getHeading());
                // myY -= speed * Math.sin(getHeading());
                // }
                // }
            }
        });
        TURN_MAIN = new State(() -> {
            if (getHealth() > 0) {
                sendLogMessage("TURN");
                if (messages != null && messages.size() > 0) {
                    boolean followingHasBroadcast = false;
                    boolean followingNeedHelp = false;
                    boolean otherNeedHelp = false;
                    for (String msg : messages) {
                        String[] tokens = msg.split(":");
                        if (Integer.parseInt(tokens[0]) == following) {
                            secondaryHasStartBroadcast = true;
                            followingHasBroadcast = true;
                            if (lastMessageCode != HELP) {
                                lastMessageCode = Integer.parseInt(tokens[2]);
                                if (lastMessageCode == HELP)
                                    followingNeedHelp = true;
                                followX = Double.parseDouble(tokens[3]);
                                followY = Double.parseDouble(tokens[4]);
                                if (lastMessageCode == FOLLOW && Math.abs(followY - myY) <= 250)
                                    followY = myY;
                            }
                        } else if (Integer.parseInt(tokens[2]) == HELP)
                            otherNeedHelp = true;
                    }
                    if ((!followingHasBroadcast && secondaryHasStartBroadcast)
                            || (otherNeedHelp && !followingNeedHelp)) {
                        for (String msg : messages) {
                            String[] tokens = msg.split(":");
                            if (lastMessageCode != HELP) {
                                lastMessageCode = Integer.parseInt(tokens[2]);
                                followX = Double.parseDouble(tokens[3]);
                                followY = Double.parseDouble(tokens[4]);
                                if (lastMessageCode == FOLLOW && Math.abs(followY - myY) <= 250)
                                    followY = myY;
                                following = Integer.parseInt(tokens[0]);
                                if (lastMessageCode == HELP)
                                    break;
                            }
                        }
                    }

                    sendLogMessage(String.valueOf(followingHasBroadcast) + " " + String.valueOf(otherNeedHelp) + " "
                            + String.valueOf(followingNeedHelp));

                }
                if (followX != -1 && followY != -1)
                    stepTurn(Helpers.getDirection(myX, myY, followX, followY, getHeading()));
            }
        });

        INIT.addNextState(TURN_MAIN);
        MOVETO_MAIN.addNextState(FIRE_MAIN, TURN_MAIN);

        // TURN MAIN
        TURN_MAIN.addTransitionCondition(INIT.getId(), () -> {
            if (whoAmI == ALPHA || whoAmI == BETA || whoAmI == GAMMA) {
                following = MARIO;
                return true;
            }

            if (whoAmI == GAMMA) {
                following = ROCKY;
                return true;
            }
            return false;
        });
        TURN_MAIN.addTransitionCondition(MOVETO_MAIN.getId(), () -> {
            boolean isUpdatingTime = (whenUpdateFollows >= FOLLOW_STEP && tracking >= FIRE_TIMEOUT_STEP);
            if (isUpdatingTime) {
                whenUpdateFollows = 0;
                //lastMessageCode = 0;
            }
            return isUpdatingTime;
        });
        TURN_MAIN.addNextState(FIRE_MAIN, MOVETO_MAIN);

        // FIRE_MAIN
        FIRE_MAIN.addTransitionCondition(TURN_MAIN.getId(), () -> opponents != null && opponents.size() > 0);
        FIRE_MAIN.addTransitionCondition(MOVETO_MAIN.getId(), () -> opponents != null && opponents.size() > 0);
        FIRE_MAIN.addNextState(MOVETO_MAIN);

        // MOVE MAIN
        MOVETO_MAIN.addTransitionCondition(FIRE_MAIN.getId(), () -> {
            boolean isUpdatingTime = (firing >= FIRE_TIMEOUT_STEP);
            if (isUpdatingTime) {
                firing = 0;
                tracking = 0;
            }
            return isUpdatingTime;
        });
        MOVETO_MAIN.addTransitionCondition(TURN_MAIN.getId(),
                () -> followX >= 0 && followY >= 0
                        && Helpers.asTurnInFrontOfPoint(myX, myY, followX, followY, getHeading()));

    }

    public void myBroadcastReceive() {
        sendLogMessage("Broadcast");
        boolean asHelp = false;
        opponents = Helpers.isRadarOpponent(detectRadar());
        for (IRadarResult o : Helpers.isRadarOpponent(detectRadar())) {
            asHelp = true;
            double enemyX = myX + o.getObjectDistance() * Math.cos(o.getObjectDirection());
            double enemyY = myY + o.getObjectDistance() * Math.sin(o.getObjectDirection());
            broadcast(whoAmI + ":" + TEAM + ":" + HELP + ":" + enemyX + ":" + enemyY + ":" + OVER);
            messages = fetchAllMessages();
        }
        if (!asHelp && getHealth() > 0) {
            broadcast(whoAmI + ":" + TEAM + ":" + FOLLOW + ":" + myX + ":" + myY + ":" + OVER);
            messages = fetchAllMessages();
        }
    }
}