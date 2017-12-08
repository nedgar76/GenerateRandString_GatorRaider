package ufl.cs1.controllers;

import game.controllers.DefenderController;
import game.models.Defender;
import game.models.Game;
import game.models.Node;

import java.util.List;

public final class StudentController implements DefenderController
{
    // constants
    private static final int RED_GHOST_ID = 0;      // Chaser, 1st out
    private static final int PINK_GHOST_ID = 1;     // Trapper, 2nd out
    private static final int ORANGE_GHOST_ID = 2;   // Guard, 3rd out
    private static final int BLUE_GHOST_ID = 3;     // Bait, 4th out

    // variables
    private int currLevel = -1;     // first level is 0, so this forces updatePowerPillNodes at the start.
    private List<Node> powerPillNodes;
    private Defender Chaser;
    private Defender Trapper;
    private Defender Guard;
    private Defender Bait;

    public void init(Game game)
    {
        Chaser = game.getDefender(RED_GHOST_ID);
        Trapper = game.getDefender(PINK_GHOST_ID);
        Guard = game.getDefender(ORANGE_GHOST_ID);
        Bait = game.getDefender(BLUE_GHOST_ID);
    }

    public void shutdown(Game game) { }

    public int[] update(Game game, long timeDue)
    {
        int[] actions = new int[Game.NUM_DEFENDER];

        updatePowerPillNodes(game);

        actions[RED_GHOST_ID] = getChaserBehavior(game);
        actions[PINK_GHOST_ID] = getTrapperBehavior(game);
        actions[ORANGE_GHOST_ID] = getGuardBehavior(game);
        actions[BLUE_GHOST_ID] = getBaitBehavior(game);

        return actions;
    }

    private void updatePowerPillNodes(Game game)
    {
        if (game.getLevel() != currLevel) {
            powerPillNodes = game.getPowerPillList();
            currLevel = game.getLevel();
        }
    }

	// Nathan
	// TODO: Add description
    private int getChaserBehavior(Game game)
	{
        // TODO: implement
        return 0;
	}

	// Nathan
	// TODO: Add description
	private int getTrapperBehavior(Game game)
	{
	    // TODO: implement
	    return 0;
	}

	// Tyler
	// TODO: Add description
	private int getGuardBehavior(Game game)
	{
        // TODO: implement
        return 0;
	}

	// Ryan
	// TODO: Add description
	private int getBaitBehavior(Game game)
	{
        // TODO: implement
        return 0;
	}
}