package algorithms;

import robotsimulator.Brain;
import characteristics.IFrontSensorResult;
import characteristics.Parameters;

public abstract class Helpers extends Brain {

  private static IFrontSensorResult.Types WALL = IFrontSensorResult.Types.WALL;
  private static IFrontSensorResult.Types OPPONENTMAIN = IFrontSensorResult.Types.OpponentMainBot;
  private static IFrontSensorResult.Types OPPONENTSECOND = IFrontSensorResult.Types.OpponentSecondaryBot;
  private static IFrontSensorResult.Types TEAMMATEMAIN = IFrontSensorResult.Types.TeamMainBot;
  private static IFrontSensorResult.Types TEAMMATESECOND = IFrontSensorResult.Types.TeamSecondaryBot;

  private static final double ANGLEPRECISION = 0.1;

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

  public static boolean asCompletelyTurned(double heading, double oldAngle) {
    return Math.abs(normalize(heading) - normalize(oldAngle + Parameters.RIGHTTURNFULLANGLE)) < ANGLEPRECISION;
  }

  public static boolean asTurnInFrontOfPoint(double myX, double myY, double targetX, double targetY, double heading) {
    double angle = Math.atan2(targetY-myY, targetX-myX);
    return isSameDirection(heading, angle);
  }
}
