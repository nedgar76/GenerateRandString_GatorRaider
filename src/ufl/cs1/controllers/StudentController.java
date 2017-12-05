package ufl.cs1.controllers;

import game.controllers.DefenderController;
import game.models.Defender;
import game.models.Game;
import game.models.Node;

import java.util.List;

public final class StudentController implements DefenderController {
    private int currLevel;
    private List<Node> powerPillLocs;
    Defender defender1;
    Defender defender2;
    Defender defender3;
    Defender defender4;

    public void init(Game game) {
        currLevel = game.getLevel();
        powerPillLocs = game.getPowerPillList();

    }

    public void shutdown(Game game) {
    }

    public int[] update(Game game, long timeDue) {
        if (game.getLevel() != currLevel) {
            powerPillLocs = game.getPowerPillList();
            currLevel = game.getLevel();
        }

        int[] actions = new int[Game.NUM_DEFENDER];
        List<Defender> enemies = game.getDefenders();

        for (int i = 0; i < 4; i++) {
            actions[i] = game.getDefender(i).getNextDir(powerPillLocs.get(i), true);

        }


        /*
		//Chooses a random LEGAL action if required. Could be much simpler by simply returning
		//any random number of all of the ghosts
		for(int i = 0; i < actions.length; i++)
		{
			Defender defender = enemies.get(i);
			List<Integer> possibleDirs = defender.getPossibleDirs();
			if (possibleDirs.size() != 0)
				actions[i]=possibleDirs.get(Game.rng.nextInt(possibleDirs.size()));
			else
				actions[i] = -1;
		}



	*/
        return actions;

    }
}