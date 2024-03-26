/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/BrainCanevas.java 2014-10-19 buixuan.
 * ******************************************************/
package algorithms;

import robotsimulator.Brain;
import slayers.Helpers;
import slayers.IState;
import slayers.State;
import characteristics.Parameters;

public class MyBrainStage1 extends Brain {
  private IState stateMachineBot;
  private double oldAngle;

  private IState INITSTATE;
  private IState MOVESTATE;
  private IState TURNSTATE;

  public MyBrainStage1() {
    super();
  }

  public void activate() {
    stateMachineBot = buildStateMachine();
    stateMachineBot.getId();
  }

  public void step() {
    stateMachineBot = stateMachineBot.evalTransitionsCondition();
    stateMachineBot.evalState();
  }

  private IState buildStateMachine() {
    INITSTATE = new State(() -> {
      stepTurn(Parameters.Direction.LEFT);
    });
    MOVESTATE = new State(() -> {
      move();
      oldAngle = getHeading();
    });
    TURNSTATE = new State(() -> {
      stepTurn(Parameters.Direction.RIGHT);
    });

    INITSTATE.addNextState(MOVESTATE);
    MOVESTATE.addNextState(TURNSTATE);
    MOVESTATE.addTransitionCondition(INITSTATE.getId(), () -> Helpers.isSameDirection(getHeading(), Parameters.NORTH));
    MOVESTATE.addTransitionCondition(TURNSTATE.getId(), () -> Helpers.asCompletelyTurned(getHeading(), oldAngle));
    TURNSTATE.addNextState(MOVESTATE);
    TURNSTATE.addTransitionCondition(MOVESTATE.getId(), () -> Helpers.isFrontWall(detectFront().getObjectType()));

    return INITSTATE;
  }
}
