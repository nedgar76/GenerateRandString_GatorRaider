package ufl.cs1.controllers;

import game.controllers.DefenderController;
import game.models.Defender;
import game.models.Game;
import game.models.Node;

import java.util.List;




public final class StudentController implements DefenderController
{
    private int currLevel = -1;     // first level is 0, so this forces updatePowerPillNodes at the start.
    private List<Node> powerPillNodes;
    Defender Chaser;
    Defender Trapper;
    Defender Guard;
    Defender Bait;

    public void init(Game game) { }

    public void shutdown(Game game) { }

    public int[] update(Game game, long timeDue)
    {
        int[] actions = new int[Game.NUM_DEFENDER];

        updatePowerPillNodes(game);
        // TODO: hookup behaviors

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
    public int getChaserBehavior()
	{
        // TODO: implement
        return 0;
	}

	// Nathan
	// TODO: Add description
	public int getTrapperBehavior()
	{
	    // TODO: implement
	    return 0;
	}

	// Tyler
	// TODO: Add description
	public int getGuardBehavior()
	{
        // TODO: implement
        return 0;
	}

	// Ryan
	// TODO: Add description
	public int getBaitBehavior()
	{
        // TODO: implement
        return 0;
	}
}