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
    private static final int CHASER_FLEE_DISTANCE = 20;     // How close Pacman can be to a power pill before chasers start running
    private static final float VULNERABLE_FLEE_BIAS = 2.1f;      // How much vulnerability affects safe radius
    private static final int BAIT_DANGER_DISTANCE = 5;      // How close Pacman can be to a power pill before Bait starts checking the safety of other ghosts
    private static final int BAIT_BIAS = 20;                  // How much Bait modifies the safe distance of other ghosts by (to speed up baiting)
    private static final int PACMAN_TRACKING_RADIUS = 50;    // Max distance at which Pacman checks for edible ghosts
    private static final float PACMAN_TRACKING_RADIUS_WEIGHT = 2.4f;   // Amount tracking radius is modified by to ensure safety

    // ghost IDs
    private static final int CHASER_ID = 0;      // Chaser, red, 1st out
    private static final int TRAPPER_ID = 1;     // Trapper, pink, 2nd out
    private static final int GUARD_ID = 2;   // Guard, orange, 3rd out
    private static final int BAIT_ID = 3;     // Bait, blue, 4th out

    // trackers
    private boolean[] isGhostSafe;
    private boolean isBaitBaiting = false;



    /* GAME METHODS */

    public void init(Game game)
    {
        isGhostSafe = new boolean[] { false, false, false, false };
    }

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

        if (isSafeFromPacman(game, Chaser, 0))
            return Chaser.getNextDir(Pacman.getLocation(), true);
        else
            return getFleeDirection(game, Chaser);
    }

    // Nathan
    // Tries to predict Pacman's future location, intercepting him along the way.
    private int getTrapperBehavior(Game game)
    {
        Defender Trapper = game.getDefender(TRAPPER_ID);
        Attacker Pacman = game.getAttacker();
        int direction = -1;

        int distanceToTrapper = Pacman.getLocation().getPathDistance(Trapper.getLocation());
        List<Node> pillList;

        // Pacman will target power pills if they exist, and normal pills otherwise,
        // so trap him by targeting it first
        if (game.getPowerPillList().size() > 0)
            pillList = game.getPowerPillList();
        else
            pillList = game.getPillList();

        Node closestPill = Pacman.getTargetNode(pillList, true);

        if (Trapper.getLocation().getPathDistance(closestPill) > Pacman.getLocation().getPathDistance(closestPill))
            // If Trapper is further away from the target pill, we need to try to beat Pacman there
            direction = Trapper.getNextDir(closestPill, true);
        else
        {
            // This section focuses on getting onto Pacman's closest path to the pill.
            // Being on his most likely path greatly increases our chances of catching him.

            List<Node> pacmanPath = Pacman.getPathTo(closestPill);

            if (pacmanPath.contains(Trapper.getLocation()))     // On the path, so switch to targeting him
                direction = Trapper.getNextDir(Pacman.getLocation(), true);
            else
                direction = Trapper.getNextDir(Trapper.getTargetNode(pacmanPath, true), true);
        }

        // If not safe from Pacman, ignore everything else and flee
        if (!isSafeFromPacman(game, Trapper, 0))
            direction = getFleeDirection(game, Trapper);

        return direction;
    }

    // Tyler
    // TODO: Add description
    private int getBaitBehavior(Game game)
    {
        Defender Bait = game.getDefender(BAIT_ID);
        Attacker Pacman = game.getAttacker();
        Node closestNode = Pacman.getTargetNode(game.getPowerPillList(), true);
        boolean shouldApproach = true;

        if (closestNode != null && Pacman.getLocation().getPathDistance(closestNode) < BAIT_DANGER_DISTANCE)
        {
            shouldApproach = false;

            if (isSafeFromPacman(game, game.getDefender(CHASER_ID), BAIT_BIAS))
                isGhostSafe[CHASER_ID] = true;
            if (isSafeFromPacman(game, game.getDefender(TRAPPER_ID), BAIT_BIAS))
                isGhostSafe[TRAPPER_ID] = true;
            if (isSafeFromPacman(game, game.getDefender(GUARD_ID), BAIT_BIAS))
                isGhostSafe[GUARD_ID] = true;

            if (isGhostSafe[CHASER_ID] && isGhostSafe[TRAPPER_ID] && isGhostSafe[GUARD_ID] || isBaitBaiting)
            {
                if (!isBaitBaiting)
                    isBaitBaiting = true;

                shouldApproach = true;
            }
        }

        if (Bait.getVulnerableTime() > 0 && isBaitBaiting)       // tracked power pill was eaten, go back to default state
        {
            isBaitBaiting = false;
            resetGhostSafeFlags();
        }

        return Bait.getNextDir(Pacman.getLocation(), shouldApproach);
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

        if (isSafeFromPacman(game, Guard, 0))
            return Guard.getNextDir(Pacman.getLocation(), true);
        else
            return getFleeDirection(game, Guard);
	    /*/
        Node closestNode = Pacman.getTargetNode(game.getPowerPillList(), true);

        if (closestNode != null)
	        return Guard.getNextDir(closestNode, true);
        else
            return Guard.getNextDir(Pacman.getLocation(), true);*/

    }



    /* UTILITY METHODS */

    private boolean isSafeFromPacman(Game game, Defender target, int bias)
    {
        Attacker Pacman = game.getAttacker();
        boolean isSafe = true;

        int safeRadius = (int)(PACMAN_TRACKING_RADIUS * PACMAN_TRACKING_RADIUS_WEIGHT) - bias;      // bias lets Bait categorize others as "ready" a little earlier
        int distanceToTarget = Pacman.getLocation().getPathDistance(target.getLocation());
        int vulnerableTime = target.getVulnerableTime();
        List<Node> powerPillList = game.getPowerPillList();

        if (powerPillList.size() > 0)
        {
            Node closestPowerPill = Pacman.getTargetNode(game.getPowerPillList(), true);

            float vulnerableBias = vulnerableTime > 0 ? VULNERABLE_FLEE_BIAS : 1.0f;

            if (Pacman.getLocation().getPathDistance(closestPowerPill) < (CHASER_FLEE_DISTANCE * vulnerableBias) && distanceToTarget < safeRadius)
                isSafe = false;
        }

        // Safe distance is either remaining time vulnerable or the minimum safe radius, whichever is smaller.
        int safeDistance = vulnerableTime > safeRadius ? safeRadius : vulnerableTime;

        if (distanceToTarget < safeDistance)
            isSafe = false;

        return isSafe;
    }

    // Gives a ghost the correct direction to get away from Pacman.
    private int getFleeDirection(Game game, Defender target)
    {
        Attacker Pacman = game.getAttacker();
        Node dangerNode;
        int[] possibleDirs = new int[2];

        // First, determine the greatest source of danger.

        List<Node> powerPillList = game.getPowerPillList();

        if (powerPillList.size() > 0)
            dangerNode = Pacman.getTargetNode(powerPillList, true);
        else
            dangerNode = Pacman.getLocation();

        // If Pacman is closer than the danger node, which may or may not be a power pill, flee from Pacman.
        if (Pacman.getLocation().getPathDistance(target.getLocation()) < dangerNode.getPathDistance(target.getLocation()))
            dangerNode = Pacman.getLocation();

        // If node's X is less, it is to the left, so flee right
        if (dangerNode.getX() < target.getLocation().getX())
            possibleDirs[0] = Game.Direction.RIGHT;
            // Otherwise, flee left
        else
            possibleDirs[0] = Game.Direction.LEFT;

        // If node's Y is greater, it is below, so flee up
        if (dangerNode.getY() > target.getLocation().getY())
            possibleDirs[1] = Game.Direction.UP;
            // Otherwise, flee down
        else
            possibleDirs[1] = Game.Direction.DOWN;

        // Figure out which has the longer viable path
        if (getBoundDistanceInDirection(game, target.getLocation(), possibleDirs[0]) > getBoundDistanceInDirection(game, target.getLocation(), possibleDirs[1]))
            return possibleDirs[0];
        else
            return possibleDirs[1];
    }

    private int getBoundDistanceInDirection(Game game, Node location, int direction)
    {
        int distFromBound = 0;
        Node testNode = location;

        while (testNode.getNeighbor(direction) != null)
        {
            distFromBound++;
            testNode = testNode.getNeighbor(direction);
        }

        return distFromBound;
    }

    private void resetGhostSafeFlags()
    {
        isGhostSafe[CHASER_ID] = false;
        isGhostSafe[TRAPPER_ID] = false;
        isGhostSafe[GUARD_ID] = false;
    }
}