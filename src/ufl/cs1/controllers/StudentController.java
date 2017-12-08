package ufl.cs1.controllers;

import game.controllers.DefenderController;
import game.controllers.benchmark.Devastator;
//import game.controllers.example.StudentController;
import game.models.Attacker;
import game.models.Defender;
import game.models.Game;
import game.models.Actor;
import game.models.*;
import game.system._Node;


//top down, left to right


import java.util.ArrayList;
import java.util.List;


public final class StudentController implements DefenderController {
	private Game currentGame = null;
	private Attacker devastator = null;
	private List<Defender> defendersList = null;
			//Creates names to differentiate between each defender
	private Defender redHunter;
	private Defender pinkChaser;
	private Defender orangePursuer;
	private Defender blueGoalie;
			//These are the classes for types of defenders, whose methods will create the unique behavior
	private attackingDefender redHunterClass;
	private attackingDefender pinkChaserClass;
	private attackingDefender orangePursuerClass;
	private blockingDefender blueGoalieClass;

	List<Node> powerPillList;
			//This class contains methods that will assist the defenders methods
	private helperClass helpers;

	public void init(Game game)
	{
	}

	public int[] update(Game game, long timeDue) {

		int[] actions = new int[4];
		StudentController.this.currentGame = game;
		StudentController.this.devastator = game.getAttacker();
		StudentController.this.defendersList = currentGame.getDefenders();
		StudentController.this.powerPillList = currentGame.getPowerPillList();
				//Assigns defender names to the actual defender objects
		redHunter = defendersList.get(0);
		pinkChaser = defendersList.get(1);
		orangePursuer = defendersList.get(2);
		blueGoalie = defendersList.get(3);

				//Initializes the needed classes for the defenders, and assigns their type
		redHunterClass = new AttackDefender1(redHunter);
		pinkChaserClass =  new AttackDefender1(pinkChaser);
		orangePursuerClass  = new AttackDefender1(orangePursuer);
		blueGoalieClass  = new BlockingDefender1(blueGoalie);

				//initializes the helper class
		helpers = new helperClass(game,devastator,redHunter,pinkChaser,orangePursuer,blueGoalie);
				//Returns actions if needed
		actions[0] = redHunterClass.updateDefender();
		actions[1] = pinkChaserClass.updateDefender();
		actions[2] = orangePursuerClass.updateDefender();
		actions[3] = blueGoalieClass.updateDefender();

		return actions;
	}

	public class AttackDefender1 implements attackingDefender{
		private Defender thisDefender;
		AttackDefender1(Defender _thisDefender)
		{
			thisDefender = _thisDefender;
		}
		public int updateDefender()
		{
			Actor closestDefender = devastator.getTargetActor(defendersList, true);
			int closestDefenderDistance = closestDefender.getLocation().getPathDistance(devastator.getLocation());
			if (thisDefender.getLocation().isJunction()) {
				if (powerPillList.size() > 0) {
					/*
					//This method will update the defender based on current game conditions
					Factors to take into account
					* Defender location
					* Number of pills remaining
					* distance from defender to pill
					* how spread apart devastator is from other pills
					*
					 */
					int devastatorToPill = helpers.devastatorToPillDistance();
					if (devastatorToPill < 5 && closestDefender == thisDefender) //if devastator is close tot the pill and hes closest defender, self sacrifice so the others can run
						return sacrifice(thisDefender);

					else if (devastatorToPill < 5) //otherwiise if defender is close and youre not closest defender, run away
					{
						return flee(devastator.getLocation());
					}
					else //If devastator is further from pill,then follow this behabior:
					{
						defenderStatus defenderVulnerability = helpers.vulnerableStatus(thisDefender);
						if (defenderVulnerability == defenderStatus.vulnerable) //if the defender is vulnerable, run awat
							return flee(devastator.getLocation());
						else if (defenderVulnerability == defenderStatus.blinking) //if blinking, it should
							return distract(thisDefender);
						else
							return chaseObject(devastator.getLocation());
					}
				}
				else
					return chaseObject(devastator.getLocation());
			}
			else
				return 0;

		}
		public int chaseObject(Node target)
		{
			int action = 0;
			if (target != null)
			{
				//action = thisDefender.getNextDir(target, true);
				try {List<Node> paths = thisDefender.getPathTo(target);
				action = thisDefender.getNextDir(paths.get(1), true);}
				catch (Exception e)
				{
					action = thisDefender.getNextDir(devastator.getLocation(), true);
				}
			}
			else
			{
				action = thisDefender.getNextDir(devastator.getLocation(), true);
			}

			return action;
		}
		public int shadowObject(Actor actor)
		{
			int actions = 0;
			int actorDirection = actor.getDirection();
			return actorDirection;
		}
		public int flee(Node target)
		{
			int action = 0;
			if (target != null)
			{
				action = thisDefender.getNextDir(target, false);
			}
			else
			{
				action = thisDefender.getNextDir(devastator.getLocation(), false);
			}
			return action;
		}
		public int distract(Defender distractor)
		{
			Node target = helpers.getNearestEmptyNode(distractor);
			if (target == null)
				return distractor.getNextDir(devastator.getLocation(), false);
			return distractor.getNextDir(target, true);
		}
		public int sacrifice(Defender martyr)
		{
			return martyr.getNextDir(devastator.getLocation(), true);
		}
		public int defend(Defender defender)
		{
			List<Node> powerPillList = currentGame.getPowerPillList();
			if (powerPillList != null) //if there are no power pills, attack or flee depending on lair Time
			{
				if (defender.isVulnerable())
					return defender.getNextDir(devastator.getLocation(), false);
				else
					return defender.getNextDir(devastator.getLocation(), true);
			}
			else
			{
				Node pillClosestToDevastator = devastator.getTargetNode(powerPillList, true);
				powerPillList.remove(pillClosestToDevastator);
				Node secondClosestPill = devastator.getTargetNode(powerPillList, true);
				if (devastator.getLocation().getPathDistance(pillClosestToDevastator) < 10)
					return defender.getNextDir(secondClosestPill, true);
				else
					return defender.getNextDir(pillClosestToDevastator, true);
			}
		}
		public int flock(Actor defender)
		{
			Actor closestDefender = devastator.getTargetActor(defendersList, true); // determines the closest defender
			List<Node> pathTo = defender.getPathTo(closestDefender.getLocation());
			if(pathTo.size() > 1)
				return defender.getNextDir(pathTo.get(1), true);
			else
				return defender.getNextDir(devastator.getLocation(), true);
		}
		public int flock(Defender defender, Node target)
		{
			List<Node> pathTo = defender.getPathTo(target);
			if(pathTo.size() > 1)
				return defender.getNextDir(pathTo.get(1), true);
			else
				return defender.getNextDir(devastator.getLocation(), true);
		}
	}

	public class BlockingDefender1 implements blockingDefender
	{
		Defender thisDefender;
		//Constuctor to initialize the defender to which the methods will reference
		BlockingDefender1(Defender _thisDefender)
		{
			thisDefender = _thisDefender;
		}
				//Create some helpful variables
		private Node closestPillDefender = null;
		private Node scndClosestPillDefender = null;
		List <Defender> otherDefenders = currentGame.getDefenders();
		private int devastatorToPillDistance;

				//based on the current game conditions, it will decide what the defender should do
		public int updateDefender()
		{
			int remainingPills = powerPillList.size();
			defenderStatus defenderState = helpers.vulnerableStatus(thisDefender); 				//This will tell if defender is vulnerable, blinking, or normal
					//Determine how far each defender is from the BlueGoalie
			int defenderADistance = thisDefender.getLocation().getPathDistance(redHunter.getLocation());
			int defenderBDistance = thisDefender.getLocation().getPathDistance(orangePursuer.getLocation());
			int defenderCDistance = thisDefender.getLocation().getPathDistance(pinkChaser.getLocation());
					//This takes the average distance, which will be used when the defenders are too close to one another
					//This prevents defenders from looping endlessly when devastator chases
			int averageDefenderDistance = (defenderADistance + defenderBDistance + defenderCDistance) / 3;
					//Removes the Blue Goalie from the defender list so it consists only of the other three defenders
			otherDefenders.remove(thisDefender);
			int defenderToDevastatorDist = thisDefender.getLocation().getPathDistance(devastator.getLocation());
			Actor closestToDevastator = devastator.getTargetActor(defendersList, true);

			if (powerPillList.size() > 0)		//This is just a check to avoid out of bounds exceptions
			{
				devastatorToPillDistance = helpers.devastatorToPillDistance(); //distance from devastator to closest power pill
			}

			if (remainingPills == 0)
			{
				//Description:
				//If no pills are remaining it should check averageDefenderDistance to ensure the others aren't too close
				//and avoid a infinite loop.
				if (averageDefenderDistance < 5)
				{
					thisDefender.getTargetActor(defendersList, true);
					return thisDefender.getNextDir(closestDefender().getLocation(), false);
				}
				//Next, if devastator is too far, BlueGoalie should move towards him to try and block
				else if (defenderToDevastatorDist > 15)
					chaseMode(devastator.getLocation());
				else
					//draw devastator towards an empty node
				{
					try {return goTowardsEmptyNode();}
					catch(Exception e) {return 0;}
				}
			}
					//Now if there is a pill remaining Blue Goalie will protect it if it is normal or blinking
					//or go towards an empty node if Blue Goalie is vulnerale as a distraction
			else if (remainingPills == 1)
				if (defenderState == defenderStatus.blinking || defenderState == defenderStatus.normal)
					return paceNodeMode(powerPillList.get(0));
				else if (defenderState == defenderStatus.vulnerable)
				{
					return goTowardsEmptyNode();
				}
					//If there are 2-4 pills; check the status of BLue Goalie and respond accordingly.
			else if (remainingPills > 1)
			{
				scndClosestPillDefender = secondClosestPillToDefender();
				closestPillDefender = closestPillToDefender();
				int defenderToClosestPillDist = closestPillDefender.getPathDistance(thisDefender.getLocation());
				if (defenderState == defenderStatus.normal)
				{
							//if devastator is closer to pill than goalie,goalie goes to 2nd closest pill
					if (devastatorToPillDistance < defenderToClosestPillDist)
						return chaseMode(scndClosestPillDefender);
					else
						return chaseMode(closestPillDefender);
				}
				else if (defenderState == defenderStatus.vulnerable)
				{
					if (averageDefenderDistance < 5) // the defenders are too close, scatter from them
					{
						return thisDefender.getNextDir(closestDefender().getLocation(), false);
					}
					return goTowardsEmptyNode();
				}
				else if (defenderState == defenderStatus.blinking)
					return chaseMode(closestPillDefender);
			}
		return 0;
		}

		//defender will pace around the node to defend it
		public int paceNodeMode(Node target)
		{
			try{return thisDefender.getNextDir(thisDefender.getPathTo(target).get(1), true);}
			catch(Exception e){return thisDefender.getNextDir(devastator.getLocation(), true);
			}
		}
		//simply runs away from devastator
		public int fleeMode()
		{
			return thisDefender.getNextDir(devastator.getLocation(), false);
		}
		//depending on devastator location and the location of the other ghosts, it will draw devastator towards empty nodes
		public int distractMode()
		{
			return 0;
		}
		private int chaseMode(Node target)
		{
			return thisDefender.getNextDir(target, true);
		}
		private Node closestPillToDefender()
		{
			return thisDefender.getTargetNode(powerPillList, true);
		}
		private Node secondClosestPillToDefender()
		{
			if (closestPillDefender != null)
				closestPillDefender = closestPillToDefender();

				List <Node> powerPillCopy = currentGame.getPowerPillList();
				powerPillCopy.remove(closestPillDefender);
				return thisDefender.getTargetNode(powerPillCopy, true);
		}
		private Defender closestDefender()
		{
			int min = thisDefender.getLocation().getPathDistance(otherDefenders.get(1).getLocation());
			Defender closest = otherDefenders.get(0);

			for (Defender defender: otherDefenders)
			{
				int distance = thisDefender.getLocation().getPathDistance(defender.getLocation());
				if ( distance < min)
				{
					min = distance;
					closest = defender;
				}
			}return closest;

		}
		private int goTowardsEmptyNode()
		{
			Node target;
			List<Node> neighbors = thisDefender.getLocation().getNeighbors();
			for (Node neighbor: neighbors) {
				if (neighbor != null && !(neighbor.isPill() || neighbor.isPowerPill()))
					return thisDefender.getNextDir(neighbor, true);
			}
			List<Node> neighborsDev = devastator.getLocation().getNeighbors();
			for (Node neighbor: neighborsDev) {
				if (neighbor != null && !(neighbor.isPill() || neighbor.isPowerPill()))
					return thisDefender.getNextDir(neighbor, true);
			}
			return 0; //if no neighbors or neighbors aren't empty, go up; this seems to work wel
		}
	}
	public enum defenderMode {
		chaseMode,
		fleeMode,
		distractMode,
		defendMode,
		flockMode;

		private defenderMode() {}
	}
	public enum defenderStatus {
		normal,
		vulnerable,
		blinking;
	}
	public void shutdown(Game game)
	{
	}

	public static class helperClass implements helperMethods {
		private Game currentGame;
		private Attacker devastator;
		private Defender redHunter;
		private Defender pinkChaser;
		private Defender orangePursuer;
		private Defender blueGoalie;

		helperClass(Game _currentGame, Attacker _devastator, Defender _redHunter, Defender _pinkChaser, Defender _orangePursuer, Defender _blueGoalie) {
			currentGame = _currentGame;
			devastator = _devastator;
			redHunter = _redHunter;
			pinkChaser = _pinkChaser;
			orangePursuer = _orangePursuer;
			blueGoalie = _blueGoalie;
		}

		public Node closestPowerPillToDevastator()
		{
			List<Node> powerPillList = currentGame.getPowerPillList();
			if (powerPillList.size() > 0)
				return devastator.getTargetNode(powerPillList, true);
			else
				return null;
		}
		public int devastatorToPillDistance()
		{
			Node closestPowerPill = closestPowerPillToDevastator();
			if (closestPowerPill == null)
				return -1;
			else return devastator.getLocation().getPathDistance(closestPowerPill);
		}
		public defenderStatus vulnerableStatus(Defender defender) {
			int lairTime = defender.getVulnerableTime();
			if (lairTime == 0)
				return defenderStatus.normal;
			else if (lairTime > 50)
				return defenderStatus.vulnerable;
			else
				return defenderStatus.blinking;
		}
		public Node getNearestEmptyNode(Actor personOfInterest)
		{
			Node to = personOfInterest.getLocation();
			List<Node> neighbors = to.getNeighbors();
			try {
				if (neighbors.size() > 0)
				{
					for (Node node : neighbors)
					{
						if (node != null && !node.isPill())
						{
							return node;
						}
					}
					return personOfInterest.getPathTo(currentGame.getCurMaze().getInitialAttackerPosition()).get(1);
				}
				return personOfInterest.getPathTo(currentGame.getCurMaze().getInitialAttackerPosition()).get(1);

			}
			catch (Exception e)
			{
				System.out.println("Oh Crap");
				return devastator.getLocation();
			}
		}
	}
		public interface helperMethods
		{
		Node closestPowerPillToDevastator();
		int devastatorToPillDistance();
		defenderStatus vulnerableStatus(Defender defender);
		Node getNearestEmptyNode(Actor personOfInterest);
		}
	public interface attackingDefender
	{
		int updateDefender();
		int chaseObject(Node target);
		int shadowObject(Actor actor);
		int flee(Node target);
		int distract(Defender distractor);
		int sacrifice(Defender martyr);
		int defend(Defender defender);
		int flock(Defender defender, Node target);
		int flock(Actor defender);
	}
	public interface blockingDefender{
		//based on the current game conditions it will decide what the defender should do

		int updateDefender();
		//defender will pace around the node to defend it
		int paceNodeMode(Node target);
		//simply runs away from devastator
		int fleeMode();
		//depending on devastator location and the location of the other ghosts, it will draw devastator towards empty nodes
		int distractMode();
	}
}
