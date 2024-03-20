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
  private static final double BOT_RADIUS = 50.0;

  public enum EnemyDirection {
    EAST, WEST, NORTH, SOUTH, UNDEFINED
  }

  public static double normalize(double dir) {
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

  public static boolean isFrontOpponent(IFrontSensorResult obstacle) {
    IFrontSensorResult.Types sensor = obstacle.getObjectType();
    return sensor == OPPONENTMAIN || sensor == OPPONENTSECOND;
  }

  public static boolean isFrontTeamMate(IFrontSensorResult.Types obstaclType) {
    IFrontSensorResult.Types sensor = obstaclType;
    return sensor == TEAMMATEMAIN || sensor == TEAMMATESECOND;
  }

  public static boolean isFrontRangeOpponent(ArrayList<IRadarResult> objects, double myX, double myY, double dir) {
    /*
    ArrayList<IRadarResult> opponents = isRadarOpponent(objects);
    for (IRadarResult o : objects) {
      if (o.getObjectType() == IRadarResult.Types.BULLET)  opponents.add(o);
   r }
    */
    double myTop = myY - BOT_RADIUS * 1.5;
    double myBottom = myY + BOT_RADIUS * 1.5;
    double myLeft = myX - BOT_RADIUS * 1.5;
    double myRight = myX + BOT_RADIUS * 1.5;
    for (IRadarResult o : objects) {
      if (o.getObjectType() != IRadarResult.Types.BULLET) {
        double enemyX = myX + o.getObjectDistance() * Math.cos(o.getObjectDirection());
        double enemyY = myY + o.getObjectDistance() * Math.sin(o.getObjectDirection());
        if (dir==Parameters.EAST || dir==Parameters.WEST) {
          double enemyTop = enemyY - BOT_RADIUS * 1.5;
          double enemyBottom = enemyY + BOT_RADIUS * 1.5;
          if ((enemyY <= myY && enemyBottom > myTop) || (enemyY >= myY && enemyTop < myBottom)) {
            return ((myX<enemyX && dir==Parameters.EAST)||(myX>=enemyX && dir==Parameters.WEST));
            /*
            if (myX < enemyX)
              return false;//EnemyDirection.WEST;
            else
              return true; //EnemyDirection.EAST;
            */
          }
        } else {
          double enemyLeft = enemyX - BOT_RADIUS * 1.5;
          double enemyRight = enemyX + BOT_RADIUS * 1.5;
          if ((enemyX <= myX && enemyRight > myLeft) || (enemyX >= myX && enemyLeft < myRight)) {
            return ((myY<enemyY && dir==Parameters.SOUTH)||(myY>=enemyY && dir==Parameters.NORTH));
            /*if (myY < enemyY)
              return false; //EnemyDirection.SOUTH;
            else
              return true; //EnemyDirection.NORTH;*/
          }       

        }
      }
    }
    return false;//EnemyDirection.UNDEFINED;
  }

  public static int isFrontRangeObstacle(ArrayList<IRadarResult> objects, double heading, double myX, double myY) {
    int res = -1;
    if (myX<=2*BOT_RADIUS || myX>=3000-2*BOT_RADIUS) return 3;
    if (myY<=2*BOT_RADIUS || myY>=2000-2*BOT_RADIUS) return 4;
    for (IRadarResult o : objects) {
      if (o.getObjectType()!=IRadarResult.Types.BULLET && o.getObjectDistance()<4*BOT_RADIUS) {
        //RIGHT
        if (heading+Math.PI/2.5>=o.getObjectDirection() && o.getObjectDirection()>=heading) {
            if (res>=0) res = 2;
            else res = 1;
        }
        //LEFT
        if (heading-Math.PI/2.5<=o.getObjectDirection() && o.getObjectDirection()<=heading) {
            if (res>=0) res = 2;
            else res = 0;
        }
      }
    }
    return res;
  }

  public static ArrayList<IRadarResult> isRadarOpponent(ArrayList<IRadarResult> objects) {
    ArrayList<IRadarResult> res = new ArrayList<>();

    for (IRadarResult o : objects) {
          if (o.getObjectType() == IRadarResult.Types.OpponentMainBot
          || o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
        res.add(o);
      }
    }
    return res;
  }

  public static ArrayList<IRadarResult> isRadarTeamMate(ArrayList<IRadarResult> objects) {
    ArrayList<IRadarResult> res = new ArrayList<>();

    for (IRadarResult o : objects) {
      if (o.getObjectType() == IRadarResult.Types.TeamMainBot
          || o.getObjectType() == IRadarResult.Types.TeamSecondaryBot) {
        res.add(o);
      }
    }
    return res;
  }

  public static boolean asCompletelyTurned(double heading, double oldAngle) {
    return Math.abs(normalize(heading) - normalize(oldAngle + Parameters.RIGHTTURNFULLANGLE)) < ANGLEPRECISION;
  }

  public static boolean asTurnInFrontOfPoint(double myX, double myY, double targetX, double targetY, double heading) {
    double angle = Math.atan2(targetY - myY, targetX - myX);
    return isSameDirection(heading, angle);
  }

  public static Parameters.Direction getDirection(double myX, double myY, double targetX, double targetY,
      double heading) {
    double angle = Math.atan2(targetY - myY, targetX - myX);
    if (angle - heading < 0) {
      return Parameters.Direction.LEFT;
    }
    return Parameters.Direction.RIGHT;
  }

  public static String initPositionAndSpeed(ArrayList<IRadarResult> detectRadar, double heading) {
    boolean detectNorth = false;
    boolean detectSouth = false;
    boolean detectWest = false;
    boolean detectEast = false;
    boolean teamA = false;
    int whoAmI = -1;
    ;
    String res = "";

    for (IRadarResult o : detectRadar) {
      if (Helpers.isSameDirection(o.getObjectDirection(), Parameters.NORTH))
        detectNorth = true;
      if (Helpers.isSameDirection(o.getObjectDirection(), Parameters.SOUTH))
        detectSouth = true;
      if (Helpers.isSameDirection(o.getObjectDirection(), Parameters.EAST))
        detectEast = true;
      if (Helpers.isSameDirection(o.getObjectDirection(), Parameters.WEST))
        detectWest = true;
    }
    if (Helpers.isSameDirection(heading, Parameters.EAST))
      teamA = true;

    if (detectNorth) {
      if (detectSouth)
        whoAmI = ALPHA;
      else if (detectEast)
        whoAmI = teamA ? BETA : MARIO;
      else if (detectWest)
        whoAmI = teamA ? MARIO : BETA;
    } else {
      if (detectEast)
        whoAmI = teamA ? GAMMA : ROCKY;
      else if (detectWest)
        whoAmI = teamA ? ROCKY : GAMMA;
    }
    res += whoAmI + ":";

    if (whoAmI == GAMMA) {
      res += (teamA ? Parameters.teamAMainBot1InitX : Parameters.teamBMainBot1InitX) + ":";
      res += (teamA ? Parameters.teamAMainBot1InitY : Parameters.teamBMainBot1InitY) + ":";
      res += (teamA ? Parameters.teamAMainBotSpeed : Parameters.teamBMainBotSpeed);
    } else if (whoAmI == ALPHA) {
      res += (teamA ? Parameters.teamAMainBot2InitX : Parameters.teamBMainBot2InitX) + ":";
      res += (teamA ? Parameters.teamAMainBot2InitY : Parameters.teamBMainBot2InitY) + ":";
      res += (teamA ? Parameters.teamAMainBotSpeed : Parameters.teamBMainBotSpeed);
    } else if (whoAmI == BETA) {
      res += (teamA ? Parameters.teamAMainBot3InitX : Parameters.teamBMainBot3InitX) + ":";
      res += (teamA ? Parameters.teamAMainBot3InitY : Parameters.teamBMainBot3InitY) + ":";
      res += (teamA ? Parameters.teamAMainBotSpeed : Parameters.teamBMainBotSpeed);
    } else if (whoAmI == ROCKY) {
      res += (teamA ? Parameters.teamASecondaryBot1InitX : Parameters.teamBSecondaryBot1InitX) + ":";
      res += (teamA ? Parameters.teamASecondaryBot1InitY : Parameters.teamBSecondaryBot1InitY) + ":";
      res += (teamA ? Parameters.teamASecondaryBotSpeed : Parameters.teamBSecondaryBotSpeed);
    } else {
      res += (teamA ? Parameters.teamASecondaryBot2InitX : Parameters.teamBSecondaryBot2InitX) + ":";
      res += (teamA ? Parameters.teamASecondaryBot2InitY : Parameters.teamBSecondaryBot2InitY) + ":";
      res += (teamA ? Parameters.teamASecondaryBotSpeed : Parameters.teamBSecondaryBotSpeed);
    }
    return res;
  }
}
