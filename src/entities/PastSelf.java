package entities;

import javafx.scene.Group;
import javafx.scene.shape.Cylinder;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that tracks states of all past selves in the single-player co-op game
 * Works for both zombies and player creatures
 * @author Sarah Salmonsons
 */
public class PastSelf
{

  private List<Double> walkBehaviorsX = new ArrayList<>();
  private List<Double> walkBehaviorsZ = new ArrayList<>();
  private List<Double> actionBehaviors = new ArrayList<>();

  private Cylinder boundingCircle = new Cylinder();
  private boolean isZombie = false;
  private int turnWorldEnds;

  //for animation of a past player
  public Group selfMeshes = new Group();
  public int currentFrame = 0;
  private boolean direction = true;


  /**
   * Constructor takes three Lists, the first of X-axis walking behaviors, the second of Z-axis walking
   * behaviors, and the final of action behaviors so that these actions can be "replayed" where each index is
   * the equivalent turn counter in the game. It also takes a boolean value that is true if the new PastSelf
   * is a Zombie, and false if it is a Player
   * @param walkBehaviorsX
   * @param walkBehaviorsZ
   * @param actionBehaviors
   * @param zomBool set true if PastSelf is a Zombie
   * @author Sarah Salmonson
   */
  public PastSelf (List<Double> walkBehaviorsX, List<Double> walkBehaviorsZ, List<Double> actionBehaviors, boolean zomBool, int turnIndex)
  {
    this.walkBehaviorsX.addAll(walkBehaviorsX);
    this.walkBehaviorsZ.addAll(walkBehaviorsZ);
    this.actionBehaviors.addAll(actionBehaviors);
    this.isZombie = zomBool;
    this.turnWorldEnds = turnIndex;
  }

  /**
   * Moves the PastSelf and implements the actions it took on its original timeline, like a sad marionette
   * @param turnIndex the index in array of behavior to replay
   * @author Sarah Salmonson
   */
  public void replayCreature (int turnIndex)
  {

    if(this.walkBehaviorsX.size() > turnIndex)
    {
      this.boundingCircle.setTranslateX(this.walkBehaviorsX.get(turnIndex));
      this.selfMeshes.setTranslateX(this.walkBehaviorsX.get(turnIndex));
      this.boundingCircle.setTranslateZ(this.walkBehaviorsZ.get(turnIndex));
      this.selfMeshes.setTranslateZ(this.walkBehaviorsZ.get(turnIndex));
    }
    if(this.actionBehaviors.size() > turnIndex)
    {
      if(this.actionBehaviors.get(turnIndex) == -1.0)
      {
        this.selfMeshes.setVisible(false);
        this.boundingCircle.setTranslateX(0);
        this.boundingCircle.setTranslateZ(0);
      }
      else
      {
        this.selfMeshes.setVisible(true);
        this.boundingCircle.setRotate(this.actionBehaviors.get(turnIndex)+180);
        this.selfMeshes.setRotate(this.actionBehaviors.get(turnIndex)+180);
      }
    }
    if(turnIndex % 2 == 0)
    {
      this.nextMesh();
    }
  }

  /**
   * Public getter for the PastSelf bounding circle
   * @return Cylinder
   */
  public Cylinder getBoundingCircle()
  {
    return this.boundingCircle;
  }

  /**
   * Public getter for the isZombie boolean value
   * @return true if PastSelf is a zombie, false if a player
   */
  public boolean isZombie()
  {
    return isZombie;
  }

  /**
   * Dispose of PastSelf object and associated elements in memory
   */
  public void dispose()
  {
    walkBehaviorsX.clear();
    walkBehaviorsZ.clear();
    actionBehaviors.clear();
    boundingCircle = null;
    selfMeshes.setVisible(false);
  }

  /**
   * @author Hector Carrillo and Nick Schrandt
   *
   * Sets the next mesh in the animation sequence as visible and the current one as not visible
   *
   * note: Taken and adapted from the Zombie class. -Nick
   */
  public void nextMesh()
  {
    selfMeshes.getChildren().get(currentFrame).setVisible(false);
    int maxFrame;
    if(this.isZombie())
    {
      maxFrame = 10;
    }
    else
    {
      maxFrame = 12;
    }
    if(currentFrame == maxFrame || currentFrame == 0)
    {
      direction = !direction;
    }

    if(direction) currentFrame++;
    else currentFrame--;
    selfMeshes.getChildren().get(currentFrame).setVisible(true);
  }

  /**
   * Getter for the turn number this PastSelf's player dies
   * @return int turnWorldEnds
   */
  public int getTurnWorldEnds()
  {
    return turnWorldEnds;
  }

  /**
   * Getter for the arraylist of pastself's actionbehaviors
   * @return List actionBehaviors
   */
  public List<Double> getActionBehaviors()
  {
    return actionBehaviors;
  }

}
