package llc.logic;

import java.io.Serializable;

/**
 * Player class.
 * @author MaxiHoeve14
 */
public class Player implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3L;
	private int minerals;
	
	public int playerID;
	
	/**
	 * Initializes the player with 0 minerals.
	 */
	public Player(int playerID) {
		this.minerals = 0;
		this.playerID = playerID;
	}
	
	/**
	 * Initializes the player with given minerals.
	 * @param startMinerals The amount of minerals that is given to the player at the beginning of a game.
	 */
	public Player(int playerID, int startMinerals) {
		this.playerID = playerID;
		this.minerals = startMinerals;
	}
	
	/**
	 * Gets the players minerals.
	 * @return Amount of minerals
	 */
	public int getMinerals() {
		return minerals;
	}

	/**
	 * Sets the players minerals.
	 * @param The amount of minerals.
	 */
	public void setMinerals(int minerals) {
		this.minerals = minerals;
	}

	/**
	 * Adds minerals to the player
	 * @param The amount of minerals to add.
	 */
	public void addMinerals(int minerals) {
		this.minerals += minerals;
	}

	/**
	 * Removes minerals from the player
	 * @param The amount of minerals to remove.
	 */
	public void removeMinerals(int minerals) {
		this.minerals -= minerals;
	}
}
