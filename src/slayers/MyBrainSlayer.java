package slayers;

import java.util.ArrayList;
import java.util.Random;

import characteristics.IRadarResult;
import characteristics.IFrontSensorResult;
import characteristics.Parameters;
import robotsimulator.Brain;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;

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
    private IState TURN_SECONDARY;
    private IState MOVE_SECONDARY;

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
    private static final int MAIN_OPPONENT = 0xB52;

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
    private boolean forward = true;
    private double directionSecondary;
    private double oldheading;

    private int helpCooldown = 75; // Cooldown pour suivre un help avant màj
    private int helpCount = 0;

    public void activate() {
        String[] initPositionAndSpeed = Helpers.initPositionAndSpeed(detectRadar(), getHeading()).split(":");
        whoAmI = Integer.parseInt(initPositionAndSpeed[0]);
        myX = Double.parseDouble(initPositionAndSpeed[1]);
        myY = Double.parseDouble(initPositionAndSpeed[2]);
        followX = 3000;
        followY = myY;
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
        TURN_SECONDARY = new State(() -> {
            sendLogMessage("" + directionSecondary);
            if (getHealth() > 0) {
                if (waitMoveSecondary <= 666)
                    waitMoveSecondary++;
                if (myX < 1500 && myY < 1000) {
                    if (oldheading == Parameters.NORTH) {
                        directionSecondary = Parameters.EAST;
                        stepTurn(Parameters.Direction.RIGHT);
                    }
                    if (oldheading == Parameters.WEST) {
                        directionSecondary = Parameters.SOUTH;
                        stepTurn(Parameters.Direction.LEFT);
                    }
                }
                if (myX > 1500 && myY < 1000) {
                    if (oldheading == Parameters.EAST) {
                        directionSecondary = Parameters.SOUTH;
                        stepTurn(Parameters.Direction.RIGHT);
                    }
                    if (oldheading == Parameters.NORTH) {
                        directionSecondary = Parameters.WEST;
                        stepTurn(Parameters.Direction.LEFT);
                    }
                }
                if (myX < 1500 && myY > 1000) {
                    if (oldheading == Parameters.SOUTH) {
                        directionSecondary = Parameters.EAST;
                        stepTurn(Parameters.Direction.LEFT);
                    }
                    if (oldheading == Parameters.WEST) {
                        directionSecondary = Parameters.NORTH;
                        stepTurn(Parameters.Direction.RIGHT);
                    }
                }
                if (myX > 1500 && myY > 1000) {
                    if (oldheading == Parameters.SOUTH) {
                        directionSecondary = Parameters.WEST;
                        stepTurn(Parameters.Direction.RIGHT);
                    }
                    if (oldheading == Parameters.EAST) {
                        directionSecondary = Parameters.NORTH;
                        stepTurn(Parameters.Direction.LEFT);
                    }
                }

            }
        });

        MOVE_SECONDARY = new State(() -> {
            if (getHealth() > 0) {
                if (forward) {
                    if (!Helpers.isFrontRangeOpponent(detectRadar(), myX, myY, directionSecondary)) {
                        // sendLogMessage("MOVE");
                        move();
                        myX += speed * Math.cos(getHeading());
                        myY += speed * Math.sin(getHeading());
                    } else {
                        // sendLogMessage("STOP");
                        if (directionSecondary == Parameters.NORTH)
                            directionSecondary = Parameters.SOUTH;
                        else if (directionSecondary == Parameters.SOUTH)
                            directionSecondary = Parameters.NORTH;
                        else if (directionSecondary == Parameters.WEST)
                            directionSecondary = Parameters.EAST;
                        else if (directionSecondary == Parameters.EAST)
                            directionSecondary = Parameters.WEST;
                        // directionSecondary = Helpers.normalize(directionSecondary + Math.PI);
                        forward = false;
                    }
                } else {
                    if (!Helpers.isFrontRangeOpponent(detectRadar(), myX, myY, directionSecondary)) {
                        // sendLogMessage("MOVE BACK");
                        moveBack();
                        myX -= speed * Math.cos(getHeading());
                        myY -= speed * Math.sin(getHeading());
                    } else {
                        // sendLogMessage("STOP");
                        if (directionSecondary == Parameters.NORTH)
                            directionSecondary = Parameters.SOUTH;
                        else if (directionSecondary == Parameters.SOUTH)
                            directionSecondary = Parameters.NORTH;
                        else if (directionSecondary == Parameters.WEST)
                            directionSecondary = Parameters.EAST;
                        else if (directionSecondary == Parameters.EAST)
                            directionSecondary = Parameters.WEST;
                        // directionSecondary = Helpers.normalize(directionSecondary + Math.PI);
                        forward = true;
                    }
                }
            }
        });

        TURN_SECONDARY.addNextState(MOVE_SECONDARY);
        MOVE_SECONDARY.addTransitionCondition(TURN_SECONDARY.getId(), () -> {
            return waitMoveSecondary > 666 &&
                    (forward ? Helpers.isSameDirection(getHeading(), directionSecondary)
                            : Helpers.isSameDirection(Helpers.normalize(getHeading() + Math.PI), directionSecondary));
        });

        MOVE_SECONDARY.addNextState(TURN_SECONDARY);
        TURN_SECONDARY.addTransitionCondition(MOVE_SECONDARY.getId(), () -> {
            oldheading = directionSecondary;
            return (myX >= 2500 || myX <= 500) && (myY >= 1500 || myY <= 510);
        });
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
        INIT_POS_ROCKY.addTransitionCondition(INIT.getId(), () -> {
            if (whoAmI == ROCKY) {
                waitMoveSecondary = 0;
                return true;
            }
            return false;
        });

        INIT_POS_ROCKY.addNextState(MOVETO_INIT_POS_ROCKY);
        MOVETO_INIT_POS_ROCKY.addTransitionCondition(INIT_POS_ROCKY.getId(),
                () -> Helpers.asTurnInFrontOfPoint(myX, myY, myX, 500, getHeading()));

        MOVETO_INIT_POS_ROCKY.addNextState(TURN_SECONDARY);
        TURN_SECONDARY.addTransitionCondition(MOVETO_INIT_POS_ROCKY.getId(), () -> {
            directionSecondary = Parameters.NORTH;
            oldheading = directionSecondary;
            return myY <= 500;
        });
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
        INIT_POS_MARIO.addTransitionCondition(INIT.getId(), () -> {
            if (whoAmI == MARIO) {
                waitMoveSecondary = 667;
                return true;
            }
            return false;
        });

        INIT_POS_MARIO.addNextState(MOVETO_INIT_POS_MARIO);
        MOVETO_INIT_POS_MARIO.addTransitionCondition(INIT_POS_MARIO.getId(),
                () -> Helpers.asTurnInFrontOfPoint(myX, myY, myX, 1500, getHeading()));

        MOVETO_INIT_POS_MARIO.addNextState(TURN_SECONDARY);
        TURN_SECONDARY.addTransitionCondition(MOVETO_INIT_POS_MARIO.getId(), () -> {
            directionSecondary = Parameters.SOUTH;
            oldheading = directionSecondary;
            return myY >= 1500;
        });
    }

    public void buildMain() {
        MOVETO_MAIN = new State(() -> {
            helpCount++;
            if (getHealth() > 0) {
                sendLogMessage("X: " + String.format("%.2f", myX) + ", Y:" + String.format("%.2f", myY) + ", dir: "
                        + getHeading());
                // sendLogMessage("followx:" + followX + " followY:" + followY);

                int obstacle = Helpers.isFrontRangeObstacle(detectRadar(), getHeading(), myX, myY);
                double fireAngle = Math.atan2(followY - myY, followX - myX); // là où on veut tirer
                for (IRadarResult opp : detectRadar()) {
                    double oX = myX + opp.getObjectDistance() * Math.cos(opp.getObjectDirection());
                    double oY = myY + opp.getObjectDistance() * Math.sin(opp.getObjectDirection());
                    if (opp.getObjectType() == IRadarResult.Types.Wreck
                            && followX + 50 > oX && followX - 50 < oX
                            && followY + 50 > oY && followY - 50 < oY) {
                        // followX = 1500;
                        // followY = 1000;
                        fireAngle = getHeading();
                    }
                }

                /* if (opponents != null && opponents.size() > 0) {
                    for (IRadarResult op : opponents) {
                        fire(op.getObjectDirection());
                    }
                }
                for (IRadarResult op : opponents) {
                    fire(op.getObjectDirection());
                } */

                if (helpCount % 7 == 0) {
                    boolean canFire = true;
                    // Je vérifie si j'ai des mates sur la direction où je tire
                    for (String msg : messages) {
                        String[] tokens = msg.split(":");
                        if (Integer.parseInt(tokens[0]) != whoAmI) {
                            double tmpX = Double.parseDouble(tokens[3]);
                            double tmpY = Double.parseDouble(tokens[4]);
                            double angle = Math.atan2(tmpY - myY, tmpX - myX);
                            // Define line parameters (two points)
                            Point2D.Double point1 = new Point2D.Double(myX, myY);
                            Point2D.Double point2 = new Point2D.Double(followX, followY);
                            // Create Circle2D and Line2D objects
                            Ellipse2D circle = new Ellipse2D.Double(tmpX - 50, tmpY - 50, 2 * 50, 2 * 50);
                            Line2D line = new Line2D.Double(point1, point2);
                            if (line.intersects(circle.getBounds2D())
                                    && Math.sqrt(Math.pow(myY - tmpY, 2) + Math.pow(myX - tmpX, 2)) <= Math
                                            .sqrt(Math.pow(myY - followY, 2) + Math.pow(myX - followX, 2))) {
                                canFire = false;
                                break;
                            }

                        }
                    }
                    if (canFire)
                        fire(fireAngle);
                } else {
                    if (obstacle < 0) {
                        move();
                        myX += speed * Math.cos(getHeading());
                        myY += speed * Math.sin(getHeading());
                    } else if (obstacle == 1 || obstacle == 2) {
                        stepTurn(Parameters.Direction.LEFT);
                    } else if (obstacle == 0) {
                        stepTurn(Parameters.Direction.RIGHT);
                    }
                }
            }
        });
        FIRE_MAIN = new State(() -> {
            if (getHealth() > 0) {
                sendLogMessage("FIRE");
                lastMessageCode = 0;
                firing++;
                IFrontSensorResult o = detectFront();
                if (o.getObjectType() == IFrontSensorResult.Types.OpponentMainBot
                        || o.getObjectType() == IFrontSensorResult.Types.OpponentSecondaryBot)
                    fire(getHeading());

                for (IRadarResult op : opponents) {
                    fire(op.getObjectDirection());
                }
            }
        });
        TURN_MAIN = new State(() -> {
            if (getHealth() > 0) {
                sendLogMessage("TURN");
                if (messages != null && messages.size() > 0) {
                    for (String msg : messages) {
                        String[] tokens = msg.split(":");
                        // Si following nous envoie un message
                        // if (Integer.parseInt(tokens[0]) == following) {
                        // Si following a besoin d'aide (aka ennemi detecté)

                        if (Integer.parseInt(tokens[2]) == HELP) {
                            followX = Double.parseDouble(tokens[3]);
                            followY = Double.parseDouble(tokens[4]);
                            // if (Integer.parseInt(tokens[1]) == MAIN_OPPONENT)
                            //     break;
                        }
                    }

                }
                if (followX != -1 && followY != -1)
                    stepTurn(Helpers.getDirection(myX, myY, followX, followY, getHeading()));
            }
        });

        INIT.addNextState(TURN_MAIN);
        MOVETO_MAIN.addNextState(TURN_MAIN);

        // TURN MAIN
        TURN_MAIN.addTransitionCondition(INIT.getId(), () -> {
            if (whoAmI == ALPHA || whoAmI == BETA || whoAmI == GAMMA) {
                following = MARIO;
                return true;
            }
            return false;
        });
        TURN_MAIN.addTransitionCondition(MOVETO_MAIN.getId(), () -> {
            return helpCount % helpCooldown == 0;
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
        // sendLogMessage("Broadcast");
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