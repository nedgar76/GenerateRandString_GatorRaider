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
    private static final int FLEE_DISTANCE = 20;     // How close Pacman can be to a power pill before ghosts start running
    private static final float VULNERABLE_FLEE_BIAS = 2.1f;      // How much vulnerability affects safe radius
    private static final int BAIT_DANGER_DISTANCE = 5;      // How close Pacman can be to a power pill before Bait starts checking the safety of other ghosts
    private static final int BAIT_BIAS = 20;                  // How much Bait modifies the safe distance of other ghosts by (to speed up baiting)
    private static final int PACMAN_TRACKING_RADIUS = 50;    // Max distance at which Pacman checks for edible ghosts
    private static final float PACMAN_TRACKING_RADIUS_WEIGHT = 2.4f;   // Amount tracking radius is modified by to ensure safety

    // ghost IDs
    private static final int FLAREON_ID = 0;      // Chaser type, red, 1st out
    private static final int ESPEON_ID = 1;     // Trapper type, pink, 2nd out
    private static final int JOLTEON_ID = 2;   // Chaser type, orange, 3rd out
    private static final int VAPOREON_ID = 3;     // Bait type, blue, 4th out

    // Bait flags
    private boolean[] isGhostSafe;
    private boolean isBaitBaiting = false;



    /* GAME METHODS */

    public void init(Game game) { isGhostSafe = new boolean[] { false, false, false, false }; }

    public void shutdown(Game game) { }

    public int[] update(Game game, long timeDue)
    {
        int[] actions = new int[Game.NUM_DEFENDER];

        actions[FLAREON_ID] = getChaserBehavior(game, FLAREON_ID);
        actions[ESPEON_ID] = getTrapperBehavior(game);
        actions[JOLTEON_ID] = getChaserBehavior(game, JOLTEON_ID);
        actions[VAPOREON_ID] = getBaitBehavior(game);

        return actions;
    }



    /* BEHAVIOR METHODS */

    // Determines if it is safe to approach Pacman; if so, heads directly for him to apply constant pressure.
    private int getChaserBehavior(Game game, int defenderID)
    {
        Defender Chaser = game.getDefender(defenderID);
        Attacker Pacman = game.getAttacker();

        if (isSafeFromPacman(game, Chaser, 0))
            return Chaser.getNextDir(Pacman.getLocation(), true);
        else
            return getFleeDirection(game, Chaser);
    }

    // Tries to predict Pacman's future location, intercepting him along the way.
    // Specifically, tries to get on Pacman's node path, ensuring that Pacman must go through the Trapper to reach his objective.
    private int getTrapperBehavior(Game game)
    {
        Defender Trapper = game.getDefender(ESPEON_ID);
        Attacker Pacman = game.getAttacker();
        int direction = -1;     // using a variable allows isSafeFromPacman to have ultimate priority

        int distanceToTrapper = Pacman.getLocation().getPathDistance(Trapper.getLocation());
        List<Node> pillList;

        // Pacman will target power pills if they exist, and normal pills otherwise.
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

    // In default state, simply chases Pacman.
    // When Pacman is guarding a power pill, waits for all ghosts to be safe and then approaches,
    // triggering Pacman's feast and minimizing casualties (hopefully to just one).
    private int getBaitBehavior(Game game)
    {
        Defender Bait = game.getDefender(VAPOREON_ID);
        Attacker Pacman = game.getAttacker();

        Node closestNode = Pacman.getTargetNode(game.getPowerPillList(), true);
        boolean shouldApproach = true;

        // First, make sure closestNode exists, e.g. the power pill list wasn't empty, e.g. there are still power pills.
        // Then verify that Pacman is on top of that power pill he is closest to.
        if (closestNode != null && Pacman.getLocation().getPathDistance(closestNode) < BAIT_DANGER_DISTANCE)
        {
            // Don't approach until all the other ghosts are safe.
            shouldApproach = false;

            // isGhostSafe starts as a list of 'false', and as each ghost reaches a safe distance, it is tagged.
            // They must be tagged separately, as a ghost at a safe distance may have a holding pattern that briefly takes it within unsafe distance.
            // BAIT_BIAS gives some wiggle room so the ghosts don't all have to be 100% safe before Bait starts moving in
            if (isSafeFromPacman(game, game.getDefender(FLAREON_ID), BAIT_BIAS))
                isGhostSafe[FLAREON_ID] = true;
            if (isSafeFromPacman(game, game.getDefender(ESPEON_ID), BAIT_BIAS))
                isGhostSafe[ESPEON_ID] = true;
            if (isSafeFromPacman(game, game.getDefender(JOLTEON_ID), BAIT_BIAS))
                isGhostSafe[JOLTEON_ID] = true;

            // Once all four ghosts are safe, OR if we are still in the baiting behavior...
            if (isGhostSafe[FLAREON_ID] && isGhostSafe[ESPEON_ID] && isGhostSafe[JOLTEON_ID] || isBaitBaiting)
            {
                // Make sure the behavior is flagged first time through.
                if (!isBaitBaiting)
                    isBaitBaiting = true;

                // Start approaching to trigger the power pill.
                shouldApproach = true;
            }
        }

        // If vulnerable time is not zero, a pill must have been eaten.
        // If a pill was eaten AND the baiting state was active...
        if (Bait.getVulnerableTime() > 0 && isBaitBaiting)
        {
            // ...it needs to be switched off, so Bait can flee, then hopefully tag Pacman before he switches from his feast state.
            // Reset the Bait flags for the next run.
            isBaitBaiting = false;
            resetGhostSafeFlags();
        }

        return Bait.getNextDir(Pacman.getLocation(), shouldApproach);
    }



    /* UTILITY METHODS */

    // Determines if a ghost is currently within a dangerous distance.
    // Possible causes of an "unsafe" status:
    // - Pacman is camping a power pill and the ghost is close enough that Pacman can tag him before vulnerability ends
    // - The ghost is vulnerable, and Pacman is close enough to tag him before vulnerability ends
    // Uses of this method:
    // - Chasers and the Trapper can determine whether it is safe to approach Pacman
    // - Bait can track the safety of the other ghosts, ensuring they are far enough away before triggering the feast
    private boolean isSafeFromPacman(Game game, Defender target, int bias)
    {
        Attacker Pacman = game.getAttacker();
        boolean isSafe = true;

        // Basing this on Pacman's tracking radius hopefully only tags ghosts as safe when they are invisible to him while in his feast state.
        // The bias is for Bait, so he can start approaching Pacman while the other ghosts finish getting to a safe state.
        int safeRadius = (int)(PACMAN_TRACKING_RADIUS * PACMAN_TRACKING_RADIUS_WEIGHT) - bias;

        int distanceToTarget = Pacman.getLocation().getPathDistance(target.getLocation());
        int vulnerableTime = target.getVulnerableTime();
        List<Node> powerPillList = game.getPowerPillList();

        // If there are still power pills, use those to determine safety first.
        if (powerPillList.size() > 0)
        {
            Node closestPowerPill = Pacman.getTargetNode(game.getPowerPillList(), true);

            // If a ghost is vulnerable, it will consider Pacman to be an unsafe distance from a power pill at larger distances.
            // This means the ghosts are more cautious of Pacman approaching a power pill while they are still vulnerable,
            // hopefully keeping Pacman from stacking pills to eliminate the whole team at once.
            float vulnerableBias = vulnerableTime > 0 ? VULNERABLE_FLEE_BIAS : 1.0f;

            // First, check if Pacman is too close to a power pill.
            // Then, verify the ghost is within the danger distance.
            if (Pacman.getLocation().getPathDistance(closestPowerPill) < (FLEE_DISTANCE * vulnerableBias) && distanceToTarget < safeRadius)
                isSafe = false;
        }

        // Safe distance is either remaining time vulnerable or the minimum safe radius, whichever is smaller.
        // This ensures that the ghost gets closer to Pacman as time runs out, without running into the problem of
        // setting the safe distance to 150+ (most of the map) right after becoming vulnerable.
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
        // If there are power pills, track where they are to run away if Pacman begins approaching. They may be of more immediate danger than him.
        // If not (this is the case when the last power pill is eaten), just run from Pacman.

        List<Node> powerPillList = game.getPowerPillList();

        if (powerPillList.size() > 0)
            dangerNode = Pacman.getTargetNode(powerPillList, true);
        else
            dangerNode = Pacman.getLocation();

        // If Pacman is closer than the danger node (which is likely a power pill), the danger node is irrelevant: flee from Pacman.
        if (Pacman.getLocation().getPathDistance(target.getLocation()) < dangerNode.getPathDistance(target.getLocation()))
            dangerNode = Pacman.getLocation();

        // Now that the most immediate source of danger is determined, chart out the best directions to flee in.

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

        // Figure out which has the longer viable path.
        // There are two possible flight directions: a vertical, and a horizontal. The ghost should pick whichever one has the longest path,
        // e.g. whichever one will not result in a loop that either gets the ghost stuck, or sends it right back towards Pacman.
        if (getBoundDistanceInDirection(game, target.getLocation(), possibleDirs[0]) > getBoundDistanceInDirection(game, target.getLocation(), possibleDirs[1]))
            return possibleDirs[0];
        else
            return possibleDirs[1];
    }

    // Counts nodes from a location until hitting a wall.
    // Can help determine which direction has the longest viable straight path.
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

    // Resets Bait's flags to go back to default state and prepare for Pacman camping the next power pill.
    private void resetGhostSafeFlags()
    {
        isGhostSafe[FLAREON_ID] = false;
        isGhostSafe[ESPEON_ID] = false;
        isGhostSafe[JOLTEON_ID] = false;
    }
}