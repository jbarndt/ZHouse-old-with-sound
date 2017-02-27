
package entities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import game_engine.Attributes;
import game_engine.Scenes;
import game_engine.ZombieHouse3d;
import gui.Main;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape3D;
import levels.Tile;
import sound.Sound;
import sound.SoundManager;
import utilities.ZombieBoardRenderer;

/**
 * @author Jeffrey McCall 
 *         Ben Matthews
 *         Atle Olson
 *
 *         Hector Carillo
 *         Sarah Salmonson
 *         Nick Schrandt
 * This class handles many different functions for all of the entities in the
 * game, which are the player and the zombies. Values are updated for the entities
 * every time the animation timer is called. Various other functions are performed
 * here such as calculating the sound balance as well as collision detection.
 *
 * new: Now handles the zombie and player attack/push functionality. Whenever a player
 * his space key, or the left-mouse, the entitymanager checks for zombies to hit. And
 * it checks each zombie in the tick to see if it's in range of a player.
 *
 */
public class EntityManager
{
  public Player player;
  public ArrayList<Zombie> zombies;
  public ArrayList<PastSelf> pastSelves = new ArrayList<>();
  public ArrayList<PastSelf> tempPastZombies = new ArrayList<>();
  public Set<Zombie> interactingZombies = new HashSet<>();
  int turnCounter = 0;
  private int timekeeper = 0;
  public SoundManager soundManager;
  public ZombieHouse3d zombieHouse;
  public Scenes scenes;
  public Main main;
  public AtomicBoolean gameIsRunning = new AtomicBoolean(true);
  Zombie masterZombie;
  
  private MasterZombieDecision masterDecision;
  private ZombieDecision zombieDecision;
  private int playerLives = 3;
  private int region1Counter = 0;
  private int region2Counter = 0;
  private int region3Counter = 0;
  private int region4Counter = 0;

  // The number of wall tiles on the map. Used to check for collisions.
  public int numTiles = 0;
  
  /**
   * Constructor for EntityManager.
   * @param //soundManager
   *        The SoundManager class being used to manage all of the sound
   *        of the game. Commented out
   * @param main
   *        The Main class that is running the program and is the entry point 
   *        for starting and playing the game.
   * @param scenes
   *        The various screens that are seen throughout playing the game, such as
   *        the main menu, the settings menu, the win screen, etc.
   */
  public EntityManager(SoundManager soundManager, Main main, Scenes scenes)
  {
    this.soundManager = soundManager;
    this.scenes = scenes;
    this.main = main;
    zombies = new ArrayList<>();
    zombieDecision = new ZombieDecision();
    zombieDecision.setDaemon(true);
    zombieDecision.start();
  }


  /**
   * Checks if the zombie is colliding with anything.
   * 
   * @return False if no collision detected. True if there is a collision.
   */
  public boolean checkTwoD(Circle zombieCirc)
  {
    for (int i = 0; i < numTiles; i++)
    {
      if (zombieCirc.getLayoutBounds()
          .intersects(ZombieBoardRenderer.walls.get(i).getLayoutBounds()))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Collision detection for 3D zombie objects.
   * 
   * @param creature
   *          The shape that represents the creature.
   * @return Box with which the creature collided. Null if no collision
   */
  public Box getWallCollision(Shape3D creature)
  {
    for (int i = 0; i < numTiles; i++)
    {
      if (creature.getBoundsInParent()
          .intersects(zombieHouse.getWalls().get(i).getBoundsInParent()))
      {
        return zombieHouse.getWalls().get(i);
      }
    }
    for(int i = 0; i < zombieHouse.getObstacles().size(); i++)
    {
      if(creature.getBoundsInParent()
              .intersects(zombieHouse.getObstacles().get(i).getBoundsInParent()))
      {
        return zombieHouse.getObstacles().get(i);
      }
    }
    return null;
  }

  /**
   * Collision detection for 3D player objects.
   * 
   * @param player
   *          The shape that represents the player.
   * @return True if there is a collision. False if there isn't.
   */
  public boolean playerCollidesWithZombie(Shape3D player)
  {
    for (Zombie zombie : zombies)
    {
      if (player.getBoundsInParent()
          .intersects(zombie.zombieCylinder.getBoundsInParent()))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * @author Nick
   * @param zombie the zombie that is attacking the player
   * @return boolean True if attack deals damage.
   * This method is called every zombie tick to check if the player is in it's
   */
  public boolean zombieAttack(Zombie zombie)
  {
    if(distanceFromPlayer(zombie) < .65)
    {
      player.getHit(10);
      soundManager.playSoundClip(Sound.grunt);
      if(player.getHitPoints() <= 0)
      {
        player.setIsDead();
      }
      return true;
    }
    return false;
  }

    /**
     * This method is called from the MouseEventHandler when the left button
     * is pressed. It checks if there are any zombies in the player's hitbox (120 degrees)
     * and if so, it calls the hitZombie method.
     *
     * @param boundingCircle circle that represents the player characters
     *
     * @author Nick Schrandt
     * @author Sarah Salmonson
     */
  public void playerAttack(Shape3D boundingCircle)
  {
    ArrayList<Zombie> deleteList = new ArrayList<>();
    for(Zombie zombie : zombies)
    {
      if(isHit(zombie))
      {
        hitZombie(deleteList, zombie);
        interactingZombies.add(zombie);
      }
    }
    //run the attack check for each pastSelf in pastSelves
    for (PastSelf pastSelf : pastSelves)
    {
      if (boundingCircle.getBoundsInParent().intersects(pastSelf.getBoundingCircle().getBoundsInParent()))
      {
        if (pastSelf.isZombie())
        {
          //bifurcate the zombie !
          Zombie bifurcatedZombie = new Zombie(null, 0, 0,
                  pastSelf.getBoundingCircle().getTranslateX(), pastSelf.getBoundingCircle().getTranslateZ(), this);
          bifurcatedZombie.create3DZombie(Tile.tileSize);
          zombieHouse.meshZombie(bifurcatedZombie);
          initializeZombiePast(bifurcatedZombie);
          //begin life as a zombie!
          bifurcatedZombie.getWalkBehaviorsX().add(turnCounter, pastSelf.getBoundingCircle().getTranslateX());
          bifurcatedZombie.getWalkBehaviorsZ().add(turnCounter, pastSelf.getBoundingCircle().getTranslateZ());
          bifurcatedZombie.getActionBehaviors().add(turnCounter, 0.0);
          zombies.add(bifurcatedZombie);
          interactingZombies.add(bifurcatedZombie);    //TODO this is creating TWO zombies
        }
      }
    }
    for(Zombie zombie : deleteList)
    {
      zombies.remove(zombie);
      zombieHouse.root.getChildren().removeAll(zombie.zombieMeshes);
    }
  }

  public void freezeZombies()
  {
    for(Zombie zombie : zombies)
    {
      zombie.stopThreeDZombie();
    }
  }

  /**This method checks if a zombie is in the player's hitbox or not.
   *
   * @author: Nick Schrandt
   *
   * @param zombie current zombie that is being checked
   * @return whether or not the zombie is in the hitbox
   */
  private boolean isHit(Zombie zombie) {
    double angleToPlayer = findZombieAngleAbsolute(zombie);
    double playerAngle = findPlayerAngleAbsolute();
    double lowerAttackBound = findLowerAttackBound(playerAngle);
    double upperAttackBound = findUpperAttackBound(playerAngle);
    if (distanceFromPlayer(zombie) < .65 && (upperAttackBound < lowerAttackBound))
    {
      if(angleToPlayer > lowerAttackBound || angleToPlayer < upperAttackBound)
      {
        return true;
      }
    }
    else if(distanceFromPlayer(zombie) < .65 && upperAttackBound > lowerAttackBound)
    {
      if(angleToPlayer > lowerAttackBound && angleToPlayer < upperAttackBound)
      {
        return true;
      }
    }
    return false;
  }

  /**In the original code, the player angle was a value decreased as you turned left, and increased as you turned
   * right, and could become negative and go beyond 360 degrees in either direction. Since the attack hitbox is based
   * off of the relative angles, I created this method to convert the player angle into a value going from 0 to 360. It
   * cannot be negative, nor can it be greater than 360.
   *
   * @author: Nick Schrandt
   * @return double value of the absolute angle
   */
  private double findPlayerAngleAbsolute()
  {
    if(player.angle >= 0)
    {
      return player.angle%360;
    }
    else
    {
      return (360 + (player.angle%360));
    }
  }

  /**In the original code, the zombie angle was a value decreased as it turned left, and increased as it turned
   * right, and could become negative and go beyond 360 degrees in either direction. Since the attack hitbox is based
   * off of the relative angles, I created this method to convert the zombie angle into a value going from 0 to 360. It
   * cannot be negative, nor can it be greater than 360.
   *
   * @author: Nick Schrandt
   * @return double value of the absolute angle
   */
  private double findZombieAngleAbsolute(Zombie zombie)
  {
    if(zombie.getAngleToPlayer() >= 0)
    {
      return zombie.getAngleToPlayer();
    }
    else
    {
      return (360 + (zombie.getAngleToPlayer()%360));
    }
  }

  /**This method is called from the player attack method only when the conditions to hit a zombie are met.
   *
   * @author: Nick Schrandt, Sarah Salmonson
   *
   * @param deleteList list of zombies to be deleted in killed.
   * @param zombie current zombie to be hit by the player.
   */
  private void hitZombie(ArrayList<Zombie> deleteList, Zombie zombie) {
    zombie.getHit(10);
    zombie.updateHealthBar();
    //soundManager.playSoundClip(Sound.sword_hit);
    if(zombie.getHitPoints() <= 0)
    {
      deleteList.add(zombie);
      zombie.getActionBehaviors().add(zombie.getTurnCounter(), -1.0); //mark zombie as dead at this point in this timeline
      tempPastZombies.add(new PastSelf(zombie.getWalkBehaviorsX(), zombie.getWalkBehaviorsZ(), zombie.getActionBehaviors(), true, zombie.getTurnCounter()));
      interactingZombies.remove(zombie);
      zombieHouse.gameBoard[zombie.col][zombie.row].hasZombie = false;
    }
  }

  /**
   * This method is called from the KeyBoardEventHandler when 'Space'
   * is pressed. It checks if there are any zombies near the player, if so the
   * zombie is pushed aside.
   *
   * @author Hector Carrillo
   */
  public void playerPush()
  {
    for(Zombie zombie: zombies)
    {
      if (distanceFromPlayer(zombie) < .6 && !player.isPushing()) //&& isHit(player.boundingCircle, zombie))
      {
        //soundManager.playSoundClip(Sound.push);
        zombie.setPushed();
      }
    }
  }

  /**
   * calculate the distance between two entities
   *
   * @param zombie
   *        The zombie object that we are checking.
   * @return
   *        The distance between the zombie and the player.
   */
  public double distanceFromPlayer(Zombie zombie)
  {
    double xDist = player.xPos - zombie.xPos;
    double zDist = player.zPos - zombie.zPos;

    return Math.sqrt(xDist * xDist + zDist * zDist);
  }

  /**
   * calculate the sound balance based on the player angle and
   * the zombie position
   *
   * @param zombie
   * @return a number from -1 to 1 that represents the sound
   * balance
   */
  public double calculateSoundBalance(Zombie zombie)
  {
    double angle = player.boundingCircle.getRotate()*(180/Math.PI);

    double xDiff = player.xPos - zombie.xPos;
    double zDiff = player.zPos - zombie.zPos;
    double theta = Math.atan(xDiff / zDiff);

    angle -= theta;
    if (angle < -Math.PI) angle += 2*Math.PI;

    return angle/Math.PI;
  }

  /**
   * Creates list of all of the zombies that will spawn
   * on the board.
   *
   * @Nick This now loops through all of the tiles repeatedly until the minimum number of zombies has been
   * created. Since this is how it was handled in the past verison of the game, except during the tiles' creation
   * the distribution should be the same.
   */
  public void createZombies(Tile[][] gameBoard, int zHeight, int xWidth)
  {
    int zombieCounter = 0;
    while(zombieCounter <= Attributes.Min_Zombies && playerLives == 3)
    {
      for(int col = 0; col < zHeight; col++)
      {
        for(int row = 0; row < xWidth; row++)
        {
          if(gameBoard[col][row].spawnChance() && !gameBoard[col][row].hasZombie)
          {
            int tileRegion = gameBoard[col][row].getRegion();
            if(getRegionCounter(tileRegion) <= Attributes.Max_Zombies/4)
            {
              gameBoard[col][row].hasZombie = true;
              zombieCounter++;
              incrementRegionCounter(tileRegion);
            }
          }
        }
      }
    }
    int counter = 0;
    int randomZombieIndex = (int)(zombies.size()*Math.random());

    for (int col = 0; col < zHeight; col++)
    {
      for (int row = 0; row < xWidth; row++)
      {
        if (gameBoard[col][row].hasZombie && !gameBoard[col][row].isHallway)
        {
          counter++;
          Zombie newZombie = new Zombie(gameBoard[col][row], row, col,
              gameBoard[col][row].xPos, gameBoard[col][row].zPos, this);
          newZombie.create3DZombie(Tile.tileSize);
          zombies.add(newZombie);
          if (counter == Attributes.Max_Zombies)
            break;
        }
      }
      if (counter == Attributes.Max_Zombies)
        break;
    }
    int zombieListCounter = 0;

    for (int i = 0; i < zombies.size(); i++)
    {
      if (zombieListCounter == randomZombieIndex)
      {
        zombies.get(i).isMasterZombie = true;
        masterZombie=zombies.get(i);
        masterZombie.hitPoints = 30.0;
        masterDecision=new MasterZombieDecision();
        masterDecision.setDaemon(true);
        masterDecision.start();
      }
      zombieListCounter++;
    }

    for (Zombie zombie: zombies)
    {
      zombie.startZombie();
    }
  }

  /**
   * When a zombie detects the player, the master zombie also detects the player
   * and goes after the player.
   */
  public void startMasterZombie()
  {
    for (Zombie zombie : zombies)
    {
      if (zombie.isMasterZombie)
      {
        zombie.masterZombieChasePlayer.set(true);
      }
    }
  }

  /**
   * This Method updates all the values of all entities
   * @author: Jeff, Ben, Atle and Sarah Salmonson
   */
  public void tick(){
    ++timekeeper;

    player.tick();
    scenes.displayNewHP((int)player.getHitPoints());
    //fog the tiles as a function of distance from edge of far clip
    fadeToBlack(zombieHouse.getWalls());
    fadeToBlack(zombieHouse.getNotWalls());

    for (Zombie zombie: zombies)
    {
      zombie.tick();
      if(timekeeper%2 == 0) zombie.nextMesh();
      if (zombie.goingAfterPlayer.get()
          && !zombie.isMasterZombie)
      {
        startMasterZombie();
      }
    }
    //@Sarah call replayCreature method for all past players and zombies
    for(PastSelf pastSelf : pastSelves)
    {
      if(pastSelf != null)
      {
        pastSelf.replayCreature(turnCounter);
      }
      if(pastSelf.getTurnWorldEnds() == turnCounter && pastSelf.isZombie() && pastSelf.getActionBehaviors().get(pastSelf.getActionBehaviors().size()-1) != -1.0)
      {
        Zombie freeZombie = new Zombie(null, 0, 0,
                pastSelf.getBoundingCircle().getTranslateX(), pastSelf.getBoundingCircle().getTranslateZ(), this);
        freeZombie.create3DZombie(Tile.tileSize);
        zombieHouse.meshZombie(freeZombie);
        initializeZombiePast(freeZombie);
        zombies.add(freeZombie);
        pastSelf.selfMeshes.setVisible(false);
        pastSelf.getBoundingCircle().setTranslateX(0);
        pastSelf.getBoundingCircle().setTranslateZ(0);
      }
    }

    if (player.isDead.get())
    {
      playerLives--;
      //soundManager.stopTrack();
      //soundManager.playSoundClip(Sound.death);
      //log this player as a new PastSelf to be replayed upon level restart
      pastSelves.add(new PastSelf(player.getWalkBehaviorsX(), player.getWalkBehaviorsZ(), player.getActionBehaviors(), false, turnCounter));
      pastSelves.addAll(tempPastZombies);
      //remove zombies that player interacted with from zombies list and create as new PastSelf
      for(Zombie zombie : interactingZombies)
      {
        if(zombies.contains(zombie)) zombies.remove(zombie);
        pastSelves.add(new PastSelf(zombie.getWalkBehaviorsX(), zombie.getWalkBehaviorsZ(), zombie.getActionBehaviors(), true, turnCounter));
        zombieHouse.gameBoard[zombie.col][zombie.row].hasZombie = false;
      }

      interactingZombies.clear();
      tempPastZombies.clear();

      HBox hBox = new HBox();
      if(playerLives <= 0)
      {
        hBox.getChildren().addAll(scenes.returnButton);
        Label gameOverLabel = new Label("Game Over!");
        gameOverLabel.setStyle("-fx-font: 100px Tahoma; -fx-text-fill: red;");
        scenes.gameOverRoot.setCenter(gameOverLabel);
      }
      else
      {
        hBox.getChildren().addAll(scenes.returnButton, scenes.goTo3dGameDeath);
        Label livesRemaining = new Label("Lives Remaining: " + playerLives);
        livesRemaining.setStyle("-fx-font: 50px Tahoma; -fx-text-fill: red;");
        scenes.gameOverRoot.setCenter(livesRemaining);
      }
      scenes.gameOverRoot.setTop(hBox);
      main.assignStage(scenes.gameOver);
      zombieHouse.gameLoop.stop();
    }

    if (player!=null && player.foundExit.get())
    {
      //soundManager.stopTrack();
      //soundManager.playSoundClip(Sound.achieve);
      destroyZombieHouse();
      HBox hBox = new HBox();
      scenes.updateWinScreen();
      hBox.getChildren().addAll(scenes.returnButton);
      scenes.winRoot.setTop(hBox);
      main.assignStage(scenes.win);
    }
    turnCounter++;
  }

  /**@author Nick Schrandt
   *
   * Increments the counter for the region where a new zombie spawn tile was created.
   * This will create a more general distribution of zombies.
   *
   * @param region the region that a new zombie tile is in
   */
  private void incrementRegionCounter(int region)
  {
    if(region == 1)
    {
      region1Counter++;
    }
    if(region == 2)
    {
      region2Counter++;
    }
    if(region == 3)
    {
      region3Counter++;
    }
    if(region == 4)
    {
      region4Counter++;
    }
  }

  /**@author Nick Schrandt
   *
   * Returns the number of zombies in a specific region. Used to create a more general
   * distribution of zombies.
   *
   * @param region region where a zombie spawn tile is created
   * @return number of zombie spawn tiles in that region.
   */
  private int getRegionCounter(int region)
  {
    if(region == 1)
    {
      return region1Counter;
    }
    if(region == 2)
    {
      return region2Counter;
    }
    if(region == 3)
    {
      return region3Counter;
    }
    if(region == 4)
    {
      return region4Counter;
    }
    return 0;
  }

  /**@author Nick Schrandt
   *
   * @param playerAngle the angle that the player is facing
   * @return the lower bounding for the player's hitbox based from the angle they're facing
   */
  private double findLowerAttackBound(double playerAngle)
  {
    if(playerAngle - 60 > 0)
    {
      return playerAngle - 60;
    }
    else
    {
      return 360 + playerAngle - 60;
    }
  }

  /**@author Nick Schrandt
   *
   * @param playerAngle Angle that the player is facing
   * @return upper bounding for the player's hitbox
   */
  private double findUpperAttackBound(double playerAngle)
  {
    return (playerAngle + 60)%360;
  }

  /**
   *
   * @author Jeffrey McCall This is a class that extends Thread and is used to
   *         keep track of the decision rate of the zombies, which is 2 seconds.
   *
   */
  private class ZombieDecision extends Thread
  {
    /**
     * Every two seconds, if the zombie is a random walk zombie, a new angle for
     * the zombie to walk in is chosen. If the zombie has hit an obstacle, then
     * the angleAdjusted boolean flag will be on, to indicate that the angle was
     * adjusted when the zombie hit an obstacle. In this case, the
     * "makeDecision()" method is called to determine the new angle for the
     * zombie to travel in, and start it moving again. If the zombie is chasing after
     * the player, then the "findNewPath" boolean is set to on to indicate that a new
     * direction towards the player needs to be set.
     */
    @Override
    public void run()
    {
      while (gameIsRunning.get() == true)
      {
        try
        {
          sleep(2000);
        } catch (InterruptedException e)
        {
          e.printStackTrace();
        }
        for(Zombie zombie:zombies)
        {
          if(!zombie.isMasterZombie)
          {
            if (zombie.goingAfterPlayer.get())
            {
              zombie.findNewPath.set(true);
            }
            if (zombie.randomWalk && !zombie.goingAfterPlayer.get())
            {
              zombie.angle = zombie.rand.nextInt(360);
            }
            if (zombie.angleAdjusted.get())
            {
              zombie.makeDecision();
            }
          }
        }
      }
    }
  }

  /**
   *
   * @author Jeffrey McCall
   * Thread for the decision rate of the master zombie. It has a
   * faster decision rate than the regular zombies. The same operations
   * are performed on the master zombie that are performed on the
   * other zombies.
   *
   */
  private class MasterZombieDecision extends Thread
  {
    /**
     * While the game is running, perform the same operations on
     * the master zombie that would be performed on the regular zombies.
     */
    @Override
    public void run()
    {
      while (gameIsRunning.get() == true)
      {
        try
        {
          sleep(500);
        } catch (InterruptedException e)
        {
          e.printStackTrace();
        }
        if (masterZombie.masterZombieChasePlayer.get())
        {
          masterZombie.findNewPath.set(true);
        }
        if (masterZombie.randomWalk &&
            !masterZombie.masterZombieChasePlayer.get())
        {
          masterZombie.angle = masterZombie.rand.nextInt(360);
        }
        if (masterZombie.angleAdjusted.get())
        {
          masterZombie.makeDecision();
        }
      }
    }
  }

  /**
   * @param zombieHouse
   * ZombieHouse3d Object
   *
   * This Method sets the the current instance of zombieHouse3d with the parameter
   * zombieHouse
   */
  public void setZombieHouse3d(ZombieHouse3d zombieHouse){
    this.zombieHouse = zombieHouse;
  }

  /**
   * Clears game data
   * @author: original code, Sarah Salmonson
   */
  public void disposeCreatures()
  {
    gameIsRunning.set(false);

    player.dispose();
    player = null;

    for(Zombie zombie: zombies)
    {
      zombie.dispose();
    }
    zombies.clear();

    for(PastSelf pastSelf : pastSelves)
    {
      pastSelf.dispose();
    }
    pastSelves.clear();
  }

  /**
   * Darken tiles as a function of camera's visible distance, or FarClip, setting
   * @author Sarah Salmonson
   */
  private void fadeToBlack(List<Box> tiles)
  {
    double visibleDistance = zombieHouse.getCamera().getFarClip();
    //get camera's current location
    double playerXLocation = player.boundingCircle.getTranslateX();
    double playerZLocation = player.boundingCircle.getTranslateZ();
    for (Box tile : tiles)
    {
      //get the location of the tile we're examining
      double tileXLocation = tile.getTranslateX();
      double tileZLocation = tile.getTranslateZ();
      //get the distance between player and tile X and Y coordinates
      double distanceX = playerXLocation - tileXLocation;
      double distanceZ = playerZLocation - tileZLocation;
      //calculate linear distance using coordinates. Formula d = sqrt[(x1-x2)^2 + (y1-y2)^2]
      double linearDistance = Math.sqrt(distanceX * distanceX + distanceZ * distanceZ);
      //use this calculation to generate a distanceModifier that will impact tile texture color
      double darknessValue = 1.0 - linearDistance / visibleDistance;
      //if distance is in visible range, use the distance modifier to set the tile texture color
      //the further away, the darker the wall texture
      if (darknessValue < 0)
      {
        darknessValue = 0.0;
      }
      ((PhongMaterial) tile.getMaterial()).setDiffuseColor(Color.color(darknessValue, darknessValue, darknessValue));
      ((PhongMaterial) tile.getMaterial()).setSpecularColor(Color.color(darknessValue, darknessValue, darknessValue));
    }
  }

  /**
   * Disposes of the Zombie House elements.
   * @author: Sarah Salmonson
   */
  public void destroyZombieHouse()
  {
    zombieHouse.dispose();
    disposeCreatures();
  }

  /**
   * Resets the entity manager for a new playthrough of the same map
   * @author Sarah Salmonson
   */
  public void resetEM()
  {
    zombies.clear();
    turnCounter = 0;
  }

  /**
   * Make sure each zombie created mid-game has full lists of behaviors and an accuate turn coutner
   * @author Sarah Salmonson
   * @param zombie
   */
  private void initializeZombiePast(Zombie zombie)
  {
    zombie.setTurnCounter(turnCounter);
    for(int i = 0; i < turnCounter; i++)
    {
      zombie.getActionBehaviors().add(i, -1.0);
      zombie.getWalkBehaviorsX().add(i, -1.0);
      zombie.getWalkBehaviorsZ().add(i, -1.0);
    }
  }
}
