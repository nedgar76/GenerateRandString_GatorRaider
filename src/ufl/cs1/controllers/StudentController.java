package ufl.cs1.controllers;

import game.controllers.DefenderController;
import game.models.Attacker;
import game.models.Defender;
import game.models.Game;
import game.models.Node;

import java.util.List;

public final class StudentController implements DefenderController
{
    // behavior constants
    private static final int MIN_FLEE_DISTANCE = 5;     // How close Pacman can be to a power pill before chasers start running
    private static final int HOMING_DISTANCE = 100;       // How close Trapper gets before switching from predictive tracking to direct tracking

    // ghost IDs
    private static final int CHASER_ID = 0;      // Chaser, red, 1st out
    private static final int TRAPPER_ID = 1;     // Trapper, pink, 2nd out
    private static final int GUARD_ID = 2;   // Guard, orange, 3rd out
    private static final int BAIT_ID = 3;     // Bait, blue, 4th out

    // trackers
    private int currLevel = -1;     // first level is 0, so this forces updatePowerPillNodes at the start.
    private List<Node> powerPillNodes;



    /* GAME METHODS */

    public void init(Game game) { }

    public void shutdown(Game game) { }

    public int[] update(Game game, long timeDue)
    {
        int[] actions = new int[Game.NUM_DEFENDER];

        updatePowerPillNodes(game);

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
	    boolean shouldApproach;

	    if (Pacman.getLocation().getPathDistance(Pacman.getTargetNode(game.getPowerPillList(), true)) > MIN_FLEE_DISTANCE)
	        shouldApproach = true;
	    else
        {
            if (Pacman.getLocation().getPathDistance(Chaser.getLocation()) < getMaxVulnerableTime(game))
                shouldApproach = false;
            else
                shouldApproach = true;
        }

	    if (Pacman.getLocation().getPathDistance(Chaser.getLocation()) < Chaser.getVulnerableTime())
	        shouldApproach = false;

	    return Chaser.getNextDir(Pacman.getLocation(), shouldApproach);
	}

	// Nathan
	// Tries to predict Pacman's location, intercepting him along the way.
	private int getTrapperBehavior(Game game)
	{
        Defender Trapper = game.getDefender(CHASER_ID);
        Attacker Pacman = game.getAttacker();

	    if (Pacman.getLocation().getPathDistance(Trapper.getLocation()) > HOMING_DISTANCE)
	        return 0;
	    return 0;
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
	// Uses the getPacmanClosestPowerPill method to figure out
    // which Power Pill Pacman is closest to and paths to it.
	private int getGuardBehavior(Game game)
	{
	    Defender Guard = game.getDefender(GUARD_ID);
	    Attacker Pacman = game.getAttacker();
        Node closestNode = Pacman.getTargetNode(powerPillNodes, true);


	    return Guard.getNextDir(closestNode, true);
	}



	/* UTILITY METHODS */

	// When a new level is loaded, updates the power pill tracker to use the new nodes.
    private void updatePowerPillNodes(Game game)
    {
        if (game.getLevel() != currLevel) {
            powerPillNodes = game.getPowerPillList();
            currLevel = game.getLevel();
        }
    }

    // Returns vulnerable time for this level.
    private int getMaxVulnerableTime(Game game)
    {
        return (int) ( Game.VULNERABLE_TIME * Math.pow(Game.VULNERABLE_TIME_REDUCTION, game.getLevel()) );
    }
}