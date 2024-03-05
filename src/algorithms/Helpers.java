package algorithms;

import robotsimulator.Brain;

import java.util.ArrayList;

import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;

public abstract class Helpers extends Brain {

  private static IFrontSensorResult.Types WALL = IFrontSensorResult.Types.WALL;
  private static IFrontSensorResult.Types OPPONENTMAIN = IFrontSensorResult.Types.OpponentMainBot;
  private static IFrontSensorResult.Types OPPONENTSECOND = IFrontSensorResult.Types.OpponentSecondaryBot;
  private static IFrontSensorResult.Types TEAMMATEMAIN = IFrontSensorResult.Types.TeamMainBot;
  private static IFrontSensorResult.Types TEAMMATESECOND = IFrontSensorResult.Types.TeamSecondaryBot;

  private static final int ALPHA = 0x1EADDE;
  private static final int BETA = 0x5EC1;
  private static final int GAMMA = 0x333;
  private static final int ROCKY = 0x1EADDA;
  private static final int MARIO = 0x5EC0;
  private static final int TEAM = 0xBADDAD;

  private static final double ANGLEPRECISION = 0.01;

  private static double normalize(double dir) {
    double res = dir;
    while (res < 0)
      res += 2 * Math.PI;
    while (res >= 2 * Math.PI)
      res -= 2 * Math.PI;
    return res;
  }

  public static boolean isSameDirection(double dir1, double dir2) {
    return Math.abs(normalize(dir1) - normalize(dir2)) < ANGLEPRECISION;
  }

  public static boolean isFrontWall(IFrontSensorResult.Types obstaclType) {
    return obstaclType == WALL;
  }

  public static boolean isFrontOpponent(IFrontSensorResult.Types obstaclType) {
    IFrontSensorResult.Types sensor = obstaclType;
    return sensor == OPPONENTMAIN || sensor == OPPONENTSECOND;
  }

  public static boolean isFrontTeamMate(IFrontSensorResult.Types obstaclType) {
    IFrontSensorResult.Types sensor = obstaclType;
    return sensor == TEAMMATEMAIN || sensor == TEAMMATESECOND;
  }

  public static ArrayList<IRadarResult> isRadarOpponent(ArrayList<IRadarResult> objects) {
    ArrayList<IRadarResult> res = new ArrayList<>();

    for (IRadarResult o: objects){
      if (o.getObjectType()==IRadarResult.Types.OpponentMainBot || o.getObjectType()==IRadarResult.Types.OpponentSecondaryBot) {
        res.add(o);
      }
    }
    return res;
  }

  public static ArrayList<IRadarResult> isRadarTeamMate(ArrayList<IRadarResult> objects) {
    ArrayList<IRadarResult> res = new ArrayList<>();

    for (IRadarResult o: objects){
      if (o.getObjectType()==IRadarResult.Types.TeamMainBot || o.getObjectType()==IRadarResult.Types.TeamSecondaryBot) {
        res.add(o);
      }
    }
    return res;
  }

  public static boolean asCompletelyTurned(double heading, double oldAngle) {
    return Math.abs(normalize(heading) - normalize(oldAngle + Parameters.RIGHTTURNFULLANGLE)) < ANGLEPRECISION;
  }

  public static boolean asTurnInFrontOfPoint(double myX, double myY, double targetX, double targetY, double heading) {
    double angle = Math.atan2(targetY-myY, targetX-myX);
    return isSameDirection(heading, angle);
  }

  public static Parameters.Direction getDirection(double myX, double myY, double targetX, double targetY, double heading) {
    double angle = Math.atan2(targetY-myY, targetX-myX);
    if(angle - heading < 0){
      return Parameters.Direction.LEFT;
    }
    return Parameters.Direction.RIGHT;
  }

  public static String initPosition(ArrayList<IRadarResult> detectRadar, double heading){
    boolean detectNorth = false;
    boolean detectSouth = false;
    boolean detectWest = false;
    boolean detectEast = false;
    boolean teamA = false;
    int whoAmI = -1;;
    String res="";
    
    for (IRadarResult o: detectRadar) {
        if (Helpers.isSameDirection(o.getObjectDirection(),Parameters.NORTH))
            detectNorth = true;
        if (Helpers.isSameDirection(o.getObjectDirection(),Parameters.SOUTH)) 
            detectSouth = true;
        if (Helpers.isSameDirection(o.getObjectDirection(),Parameters.EAST)) 
            detectEast = true;
        if (Helpers.isSameDirection(o.getObjectDirection(),Parameters.WEST)) 
            detectWest = true;
    }
    if (Helpers.isSameDirection(heading,Parameters.EAST))
        teamA = true;

      if(detectNorth){
        if(detectSouth)
            whoAmI=ALPHA;
        else if(detectEast)
            whoAmI=teamA ? BETA : MARIO;
        else if(detectWest)
            whoAmI=teamA ? MARIO : BETA;
    }else{
        if(detectEast)
            whoAmI=teamA ? GAMMA : ROCKY;
        else if(detectWest)
            whoAmI=teamA ? ROCKY : GAMMA;
    }
    res += whoAmI + ":";

    if (whoAmI == GAMMA){
        res += (teamA ? Parameters.teamAMainBot1InitX :  Parameters.teamBMainBot1InitX) + ":";
        res +=teamA ? Parameters.teamAMainBot1InitY :  Parameters.teamBMainBot1InitY;
    } else if (whoAmI == ALPHA) {
        res +=(teamA ? Parameters.teamAMainBot2InitX :Parameters.teamBMainBot2InitX) + ":";
        res +=teamA ? Parameters.teamAMainBot2InitY :Parameters.teamBMainBot2InitY;
    } else if (whoAmI == BETA){
        res+=(teamA ? Parameters.teamAMainBot3InitX : Parameters.teamBMainBot3InitX) + ":";
        res+=teamA ? Parameters.teamAMainBot3InitY : Parameters.teamBMainBot3InitY;
    } else if (whoAmI==ROCKY) {
        res+=(teamA ? Parameters.teamASecondaryBot1InitX : Parameters.teamBSecondaryBot1InitX) + ":";
        res+=teamA ? Parameters.teamASecondaryBot1InitY : Parameters.teamBSecondaryBot1InitY;
    } else {
        res+=(teamA ? Parameters.teamASecondaryBot2InitX : Parameters.teamBSecondaryBot2InitX) + ":";
        res+=teamA ? Parameters.teamASecondaryBot2InitY : Parameters.teamBSecondaryBot2InitY;
    }
    return res;
  }
}
