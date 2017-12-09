package ufl.cs1.controllers;

import game.controllers.DefenderController;
import game.models.Attacker;
import game.models.Defender;
import game.models.Game;
import game.models.Node;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class StudentController implements DefenderController
{
    // behavior constants
    private static final int CHASER_FLEE_DISTANCE = 20;     // How close Pacman can be to a power pill before chasers start running
    private static final int HOMING_DISTANCE = 35;       // How close Trapper gets before switching from predictive tracking to direct tracking

    // ghost IDs
    private static final int CHASER_ID = 0;      // Chaser, red, 1st out
    private static final int TRAPPER_ID = 1;     // Trapper, pink, 2nd out
    private static final int GUARD_ID = 2;   // Guard, orange, 3rd out
    private static final int BAIT_ID = 3;     // Bait, blue, 4th out

    boolean inHomingDistance = false;

    /* GAME METHODS */

    public void init(Game game) { }

    public void shutdown(Game game) { }

    public int[] update(Game game, long timeDue)
    {
        int[] actions = new int[Game.NUM_DEFENDER];

        actions[CHASER_ID] = getChaserBehavior(game);
        actions[TRAPPER_ID] = getTrapperBehavior(game);
        actions[GUARD_ID] = getGuardBehavior(game);
        actions[BAIT_ID] = getBaitBehavior(game);

        return actions;
    }



    /* BEHAVIOR METHODS */

	// Nathan
	// Description: Chases after Pacman, fleeing when he gets too close to a power pill node OR when vulnerable.
    private int getChaserBehavior(Game game)
	{
	    Defender Chaser = game.getDefender(CHASER_ID);
	    Attacker Pacman = game.getAttacker();

	    return Chaser.getNextDir(Pacman.getLocation(), isSafeToTargetPacman(game, Chaser, CHASER_FLEE_DISTANCE));
	}

	// Nathan
	// Tries to predict Pacman's future location, intercepting him along the way.
	private int getTrapperBehavior(Game game)
	{
        Defender Trapper = game.getDefender(TRAPPER_ID);
        Attacker Pacman = game.getAttacker();

        int distanceToTrapper = Pacman.getLocation().getPathDistance(Trapper.getLocation());
        List<Node> powerPillList = game.getPowerPillList();

	    if (distanceToTrapper > HOMING_DISTANCE)
        {
            if (powerPillList.size() > 0)
            {
                Node closestPowerPill = Pacman.getTargetNode(game.getPowerPillList(), true);
                return Trapper.getNextDir(closestPowerPill, isSafeToTargetPacman(game, Trapper, CHASER_FLEE_DISTANCE));
            }
            else
            {
                Node closestPill = Pacman.getTargetNode(game.getPillList(), true);
                return Trapper.getNextDir(closestPill, isSafeToTargetPacman(game, Trapper, CHASER_FLEE_DISTANCE));
            }
        }
        else
        {
            return Trapper.getNextDir(Pacman.getLocation(), isSafeToTargetPacman(game, Trapper, CHASER_FLEE_DISTANCE));
        }
	}

	// Tyler
	// TODO: Add description
	private int getBaitBehavior(Game game)
	{
        Defender Bait = game.getDefender(BAIT_ID);
        Attacker Pacman = game.getAttacker();

        return Bait.getNextDir(Pacman.getLocation(), true);

	}

	// Ryan
	// As long as there is a power pill, the guard ghost
    // will target the same pill that Pacman is targeting
    // and path to it. If there aren't pills left, then the
    // ghost will path to Pacman's location.
	private int getGuardBehavior(Game game)
	{
	    Defender Guard = game.getDefender(GUARD_ID);
	    Attacker Pacman = game.getAttacker();
        Node closestNode = Pacman.getTargetNode(game.getPowerPillList(), true);

        if (closestNode != null)
	        return Guard.getNextDir(closestNode, true);
        else
            return Guard.getNextDir(Pacman.getLocation(), true);
	}



	/* UTILITY METHODS */

	private boolean isSafeToTargetPacman(Game game, Defender targeter, int fleeDistance)
    {
        Attacker Pacman = game.getAttacker();
        boolean isSafe = true;

        int distanceToTargeter = Pacman.getLocation().getPathDistance(targeter.getLocation());
        List<Node> powerPillList = game.getPowerPillList();

        if (powerPillList.size() > 0)
        {
            Node closestPowerPill = Pacman.getTargetNode(game.getPowerPillList(), true);

            if (Pacman.getLocation().getPathDistance(closestPowerPill) < fleeDistance && distanceToTargeter < getMaxVulnerableTime(game))
                isSafe = false;
        }

        if (distanceToTargeter < targeter.getVulnerableTime())
            isSafe = false;

        return isSafe;
    }

    // Returns vulnerable time specific to this level.
    private int getMaxVulnerableTime(Game game)
    {
        return (int) ( Game.VULNERABLE_TIME * Math.pow(Game.VULNERABLE_TIME_REDUCTION, game.getLevel()) );
    }
}