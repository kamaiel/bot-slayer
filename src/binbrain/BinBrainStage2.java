/* ******************************************************
* Simovies − Eurobot 2015 Robomovies Simulator.
* Copyright (C) 2014 <Binh−Minh.Bui−Xuan@ens−lyon.org>.
* GPL version>=3 <http://www.gnu.org/licenses/>.
* $Id: algorithms/Stage2SecondaryA.java 2019−03−01 buixuan.
* ******************************************************/
package binbrain;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import java.util.ArrayList;

public class BinBrainStage2 extends Brain {
    // −−−PARAMETERS−−−//
    private static final double HEADINGPRECISION = 0.001;
    private static final double ANGLEPRECISION = 0.1;
    private static final int ROCKY = 0x1EADDA;
    private static final int MARIO = 0x5EC0;
    private static final int UNDEFINED = 0xBADC0DE0;
    private static final int TURNNORTHTASK = 1;
    private static final int MOVENORTHTASK = 2;
    private static final int TURNEASTTASK = 3;
    private static final int MOVEEASTTASK = 4;
    private static final int UTURNTASK = 5;
    private static final int MOVEWESTTASK = 6;
    private static final int SINK = 0xBADC0DE1;
    // −−−VARIABLES−−−//
    private int state;
    private double myX, myY;
    private boolean isMoving;
    private int whoAmI;
    private double width;
    private int isWidth=0;

    // −−−CONSTRUCTORS−−−//
    public BinBrainStage2() {
        super();
    }

    // −−−ABSTRACT−METHODS−IMPLEMENTATION−−−//
    public void activate() {
        // ODOMETRY CODE
        whoAmI = ROCKY;
        for (IRadarResult o : detectRadar())
            if (isSameDirection(o.getObjectDirection(), Parameters.NORTH))
                whoAmI = UNDEFINED;
        if (whoAmI == ROCKY) {
            myX = Parameters.teamASecondaryBot1InitX;
            myY = Parameters.teamASecondaryBot1InitY;
        } else {
            myX = 0;
            myY = 0;
        }
        // INIT
        state = (whoAmI == ROCKY) ? TURNNORTHTASK : SINK;
        isMoving = false;
    }

    public void step() {
        // ODOMETRY CODE
        if (isMoving && whoAmI == ROCKY) {
            myX += Parameters.teamASecondaryBotSpeed * Math.cos(getHeading());
            myY += Parameters.teamASecondaryBotSpeed * Math.sin(getHeading());
            isMoving = false;
        }
        // DEBUG MESSAGE
        if (whoAmI == ROCKY && state != SINK) {
            sendLogMessage("#ROCKY *thinks* he is rolling at position (" + (int) myX + ", " + (int) myY + ").");
        }
        // AUTOMATON
        if (state == TURNNORTHTASK && !(isSameDirection(getHeading(), Parameters.NORTH))) {
            stepTurn(Parameters.Direction.LEFT);
            // sendLogMessage("Initial TeamA Secondary Bot1 position. Heading North!");
            return;
        }
        if (state == TURNNORTHTASK && isSameDirection(getHeading(), Parameters.NORTH)) {
            state = MOVENORTHTASK;
            myMove();
            // sendLogMessage("Moving a head. Waza!");
            return;
        }
        if (state == MOVENORTHTASK && detectFront().getObjectType() != IFrontSensorResult.Types.WALL && isWidth==0) {
            myMove(); // And what to do when blind blocked?
            // sendLogMessage("Moving a head. Waza!");
            return;
        }
        if (state == MOVENORTHTASK && detectFront().getObjectType() == IFrontSensorResult.Types.WALL && isWidth==0) {
            state = TURNEASTTASK;
            stepTurn(Parameters.Direction.RIGHT);
            // sendLogMessage("Iceberg at 12 o’clock. Heading 3!");
            return;
        }
        if (state == TURNEASTTASK && !(isSameDirection(getHeading(), Parameters.EAST))) {
            stepTurn(Parameters.Direction.RIGHT);
            // sendLogMessage("Initial TeamA Secondary Bot1 position. Heading East!");
            return;
        }
        if (state == TURNEASTTASK && isSameDirection(getHeading(), Parameters.EAST)) {
            state = MOVEEASTTASK;
            myMove();
            // sendLogMessage("Moving a head. Waza!");
            return;
        }
        if (state == MOVEEASTTASK && detectFront().getObjectType() != IFrontSensorResult.Types.WALL) {
            myMove(); // And what to do when blind blocked?
            // sendLogMessage("Moving a head. Waza!");
            return;
        }
        if (state == MOVEEASTTASK && detectFront().getObjectType() == IFrontSensorResult.Types.WALL) {
            width = Parameters.teamASecondaryBotFrontalDetectionRange;
            System.out.println("Detected width = " + width);
            state = UTURNTASK;
            stepTurn(Parameters.Direction.RIGHT);
            // sendLogMessage("Iceberg at 12 o’clock. Heading 6!");
            return;
        }
        if (state == UTURNTASK && !(isSameDirection(getHeading(), Parameters.WEST))) {
            stepTurn(Parameters.Direction.RIGHT);
            // sendLogMessage("Iceberg at 12 o’clock. Heading 6!");
            return;
        }
        if (state == UTURNTASK && isSameDirection(getHeading(), Parameters.WEST)) {
            state = MOVEWESTTASK;
            width += Parameters.teamASecondaryBotSpeed;
            System.out.println("Detected width = " + width);
            myMove();
            // sendLogMessage("Moving a head. Waza!");
            return;
        }
        if (state == MOVEWESTTASK && detectFront().getObjectType() != IFrontSensorResult.Types.WALL) {
            width += Parameters.teamASecondaryBotSpeed;
            System.out.println("Detected width = " + width);
            myMove(); // And what to do when blind blocked?
            // sendLogMessage("Moving a head. Waza!");
            return;
        }
        if (state == MOVEWESTTASK && detectFront().getObjectType() == IFrontSensorResult.Types.WALL) {
            state = TURNNORTHTASK;
            isWidth=1;
            width += Parameters.teamASecondaryBotFrontalDetectionRange;
            System.out.println("Detected width = " + width);
            sendLogMessage("Detected width = " + width);
            return;
        }
        if (state == SINK) {
            System.out.println("x" + myX);
            System.out.println("y" + myY);
            // myMove();
            return;
        }
        if (true) {
            return;
        }
    }

    private void myMove() {
        isMoving = true;
        move();
    }

    private boolean isSameDirection(double dir1, double dir2){
        return Math.abs(dir1-dir2)<ANGLEPRECISION;
    }
}