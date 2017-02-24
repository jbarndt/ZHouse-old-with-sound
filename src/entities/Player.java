package entities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import game_engine.Attributes;
import graphing.GraphNode;
import graphing.TileGraph;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import levels.Tile;
//import sound.Sound;
import utilities.ZombieBoardRenderer;

import static game_engine.ZombieHouse3d.loadMeshViews;

/**
 * @author Atle Olson 
 *         Jeffrey McCall
 *
 *         Nick Schrandt
 * Player object for the game. All methods having
 * to do with the player object are in this class.
 *
 * new: Attack functionality and player weapon
 * 
 */
public class Player extends Creature
{
  public static final double SPRINTSPEED = Tile.tileSize/11d;
  public static final double WALKINGSPEED = Tile.tileSize/16d;

  private List<Double> walkBehaviorsX = new ArrayList<>();
  private List<Double> walkBehaviorsZ = new ArrayList<>();
  private List<Double> actionBehaviors = new ArrayList<>();
  
  //entitymanager
  EntityManager entityManager;
  
  //camera:
  public PerspectiveCamera camera;
  public PointLight light;
  public boolean lightOn = true;

  //
  public double strafeVelocity;
  private int turnCounter = 0;

  private double radius = .25;
  
  //atomic booleans:
  public AtomicBoolean shiftPressed = new AtomicBoolean(false);
  public AtomicBoolean wDown = new AtomicBoolean(false);
  public AtomicBoolean dDown = new AtomicBoolean(false);
  public AtomicBoolean aDown = new AtomicBoolean(false);
  public AtomicBoolean sDown = new AtomicBoolean(false);
  public AtomicBoolean gameIsRunning = new AtomicBoolean(true);
  private AtomicBoolean staminaOut = new AtomicBoolean(false);
  
  //other player fields:
  public Cylinder boundingCircle = null;
  public AtomicBoolean isDead = new AtomicBoolean(false);
  public AtomicBoolean foundExit = new AtomicBoolean(false);
  
  //Player Movement
  public boolean turnLeft = false;
  public boolean turnRight = false;

  private double stamina=5;
  private double regen=.2;
  private double deltaTime=0;

  //Player weapon.
  private Node[] weaponMesh = null;
  private Group weaponMeshGroup = new Group();
  private final double WEAPON_UPWARD_ANGLE = 30;
  private final double WEAPON_Y_TRANSLATE = -.75;
  public Group swordGroup = new Group();
  private boolean attacking = false;
  private boolean pushing = false;
  private int attackAnimationTimer = 0;
  private double weaponRotationRadius = .4;
  private Rotate xRotate = new Rotate();
  private Rotate yRotate = new Rotate();
  

  /**
   * @author: original code, Nick Schrandt
   *
   * A constructor for a 3D player. takes in a camera object
   *
   * new: Now sets up the weapon as well as the camera.
   * 
   * @param x
   *        x coordinate of player
   * @param y
   *        y coordinate of player
   * @param z
   *        z coordinate of player
   * @param camera
   *        camera object used for player sight
   * @param entityManager
   *        entityManager object which updates many of the player fields as
   *        the game runs
   * @param light
   *        The light that emanates from the player
   */
  public Player(double x, double y, double z, PerspectiveCamera camera, EntityManager entityManager, PointLight light)
  {
    this.hitPoints = 100;
    stepDistance = 3;
    this.entityManager = entityManager;
    this.xPos = x;
    this.yPos = y;
    this.zPos = z;
    this.velocity = 0;
    this.angle = 0;
    this.strafeVelocity = 0;
    camera.setRotate(this.angle);
    this.camera = camera;
    camera.setTranslateX(x);
    camera.setTranslateZ(z);
    this.light = light;
    light.setRotationAxis(Rotate.Y_AXIS);
    boundingCircle = new Cylinder(radius, 1);
    PlayerStamina staminaCounter=new PlayerStamina();
    staminaCounter.start();
    boundingCircle.setTranslateX(camera.getTranslateX());
    boundingCircle.setTranslateZ(camera.getTranslateZ());

    //@Nick: This sets the initial position and rotation of the player cudgel. It also sets up the
    //  two Rotate objects to perform the animations.
    makeWeaponmesh();
    weaponMeshGroup.setTranslateX(camera.getTranslateX()+ weaponRotationRadius
            *Math.sin((angle * Math.PI/180) + (Math.PI/4)));
    weaponMeshGroup.setTranslateZ(camera.getTranslateZ()+ weaponRotationRadius
            *Math.cos((angle * Math.PI/180) + (Math.PI/4)));
    weaponMeshGroup.setTranslateY(WEAPON_Y_TRANSLATE);
    weaponMeshGroup.setRotationAxis(Rotate.X_AXIS);
    weaponMeshGroup.setRotate(WEAPON_UPWARD_ANGLE);
    swordGroup.getChildren().addAll(weaponMeshGroup);
    swordGroup.setRotationAxis(Rotate.Y_AXIS);
    weaponMeshGroup.getTransforms().addAll(xRotate, yRotate);
    yRotate.setAxis(Rotate.Y_AXIS);
    xRotate.setAxis(Rotate.X_AXIS);

    lastX = camera.getTranslateX();
    lastZ = camera.getTranslateZ();
  }
  /**
   * A constructor for a 2D player.
   * @param x
   *        x coordinate of the player
   * @param y
   *        y coordinate of the player
   */
  public Player(double x, double y)
  {
    this.xPos = x;
    this.yPos = y;
    this.velocity = 0;
    this.angle = 0;
  }

  /**@author: Nick Schrandt
   *
   * This method creates the mesh for the player weapon. Functions the same way the zombie mesh does.
   *
   */
  private void makeWeaponmesh()
  {
    weaponMesh = loadMeshViews("Resources/Meshes/Weapon/sword.obj");
    for(int i = 0; i < weaponMesh.length; i++)
    {
      weaponMesh[i].setVisible(true);
    }
    weaponMeshGroup.getChildren().addAll(weaponMesh);
    weaponMeshGroup.setScaleX(.1);
    weaponMeshGroup.setScaleY(.1);
    weaponMeshGroup.setScaleZ(.1);
    entityManager.zombieHouse.root.getChildren().add(weaponMeshGroup);
  }

  /**
   * @author Nick Schrandt
   *
   * This method is called from the tick and animates the cudgel whenever the player attacks. The player cannot
   * attack again until this animation cycle is complete.
   */
  private void animateAttack()
  {
    if(attackAnimationTimer >= 40)
    {
      attackAnimationTimer = 0;
      attacking = false;
      weaponMeshGroup.setRotate(WEAPON_UPWARD_ANGLE);
      yRotate.setAngle(0.0);
      xRotate.setAngle(0.0);
    }
    else if(attackAnimationTimer < 10)
    {
      xRotate.setAngle(xRotate.getAngle() - 2);
      yRotate.setAngle(yRotate.getAngle() - 10);
    }
    else if(attackAnimationTimer < 25)
    {
      yRotate.setAngle(yRotate.getAngle() + 10);
    }
    else if(attackAnimationTimer > 25)
    {
      yRotate.setAngle(yRotate.getAngle() - 2);
    }
    attackAnimationTimer++;
  }

  /**
   * @author Nick Schrandt
   *
   * This method is called from the tick and animates the cudgel whenever the player pushes. The player cannot push
   * again until this animation cycle completes.
   */
  private void animatePush(){
    if(attackAnimationTimer >= 50)
    {
      attackAnimationTimer = 0;
      pushing = false;
      weaponMeshGroup.setRotate(WEAPON_UPWARD_ANGLE);
      yRotate.setAngle(0.0);
      xRotate.setAngle(0.0);
    }
    else if(attackAnimationTimer < 10)
    {
      xRotate.setAngle(xRotate.getAngle() - 2);
      yRotate.setAngle(yRotate.getAngle() - 8);
    }
    else if(attackAnimationTimer < 30)
    {
      weaponRotationRadius += .01;
    }
    else if(attackAnimationTimer > 30)
    {
      weaponRotationRadius -= .01;
    }
    attackAnimationTimer++;
  }

  /**
   * Updates the player values when called from an animation timer
   * Implemented in 2 dimensions
   */
  public void tick2d()
  {
    if (xPos + (velocity * Math.cos(angle)) > 0
        && yPos + (velocity * Math.sin(angle)) > 0
        && xPos
            + (velocity * Math.cos(angle)) < ZombieBoardRenderer.boardWidth
                * ZombieBoardRenderer.cellSize
        && yPos
            + (velocity * Math.sin(angle)) < ZombieBoardRenderer.boardWidth
                * ZombieBoardRenderer.cellSize)
    {
      xPos += (velocity * Math.cos(angle));
      yPos += (velocity * Math.sin(angle));
    }
  }
  
  /**
   * Updates the player values when called from an animation timer
   * Implemented in 3 dimensions
   *
   * new: Orients the cudgel weapon to always follow the camera in both rotation and position.
   *
   * @author: original code, Nick Schrandt
   *
   */
  public void tick()
  {
    Cylinder tempX = new Cylinder(boundingCircle.getRadius(), boundingCircle.getHeight());
    Cylinder tempZ = new Cylinder(boundingCircle.getRadius(), boundingCircle.getHeight());
    
    double movementX = boundingCircle.getTranslateX();
    double movementZ = boundingCircle.getTranslateZ();
    
    movementX += (velocity * Math.sin(angle * (Math.PI / 180)));
    movementX += (strafeVelocity * Math.sin(angle * (Math.PI / 180) - Math.PI / 2));
    movementZ += (velocity * Math.cos(angle * (Math.PI / 180)));
    movementZ += (strafeVelocity * Math.cos(angle * (Math.PI / 180) - Math.PI / 2));
    
    
    tempX.setTranslateX(movementX);
    tempX.setTranslateZ(boundingCircle.getTranslateZ());
    
    tempZ.setTranslateX(boundingCircle.getTranslateX());
    tempZ.setTranslateZ(movementZ);
    
    Box collisionX = entityManager.getWallCollision(tempX);
    Box collisionZ = entityManager.getWallCollision(tempZ);

    //@Nick: Calls the animation method every frame until the animation timer reaches a certain count.
    if(attacking)
    {
      animateAttack();
    }

    if(pushing)
    {
      animatePush();
    }
    
    if(turnLeft)
    {
      this.angle -= Attributes.Player_Rotate_sensitivity;
      this.camera.setRotate(this.angle);

      //@Nick: This sets the new rotation and position of the cudgel when turning left
      swordGroup.setRotate(this.angle);
      weaponMeshGroup.setTranslateX(camera.getTranslateX()+ (weaponRotationRadius
              *Math.sin((angle * Math.PI/180) + (Math.PI/4))));
      weaponMeshGroup.setTranslateZ(camera.getTranslateZ()+ (weaponRotationRadius
              *Math.cos((angle * Math.PI/180) + (Math.PI/4))));
    }
    if(turnRight)
    {
      this.angle += Attributes.Player_Rotate_sensitivity;
      this.camera.setRotate(this.angle);
      this.light.setRotate(this.angle);

      //@Nick: This sets the new rotation and position of the cudgel when turning right
      swordGroup.setRotate(this.angle);
      weaponMeshGroup.setTranslateX(camera.getTranslateX()+ (weaponRotationRadius
              *Math.sin((angle * Math.PI/180) + (Math.PI/4))));
      weaponMeshGroup.setTranslateZ(camera.getTranslateZ()+ (weaponRotationRadius
              *Math.cos((angle * Math.PI/180) + (Math.PI/4))));
    }
    
    lastX = camera.getTranslateX();
    lastZ = camera.getTranslateZ();

    //@Hector added player collision with zombies
    if (collisionX == null && !entityManager.playerCollidesWithZombie(tempX))
    {
      //@Sarah: this is where camera is moved on X axis
      camera.setTranslateX(movementX);

      //@Nick: This moves the cudgel with the camera
      weaponMeshGroup.setTranslateX(camera.getTranslateX()+ (weaponRotationRadius
              *Math.sin((angle * Math.PI/180) + (Math.PI/4))));
    } 
    if (collisionZ == null && !entityManager.playerCollidesWithZombie(tempZ))
    {
      //@Sarah: this is where camera is moved on Z axis
      camera.setTranslateZ(movementZ);

      //@Nick: This moves the cudgel with the camera
      weaponMeshGroup.setTranslateZ(camera.getTranslateZ()+ (weaponRotationRadius
              *Math.cos((angle * Math.PI/180) + (Math.PI/4))));
    }
    
    //@Sarah: this is moving the player's "circle" to the camera's location
    boundingCircle.setTranslateX(camera.getTranslateX());
    boundingCircle.setTranslateZ(camera.getTranslateZ());

    //@Sarah: this is logging the player's movement & actions on this tick
    walkBehaviorsX.add(turnCounter, boundingCircle.getTranslateX());
    walkBehaviorsZ.add(turnCounter, boundingCircle.getTranslateZ());
    actionBehaviors.add(turnCounter, angle);

    turnCounter++;
    
    //checking for exit collision
    for (Box box: entityManager.zombieHouse.exits){
      if (box.getBoundsInParent().intersects(boundingCircle.getBoundsInParent()))
      {
        foundExit.set(true);
        System.out.println("exit");
      }
    }
    
    if(shiftPressed.get() && !staminaOut.get())
    {
      if(wDown.get())velocity=SPRINTSPEED;
      if(sDown.get())velocity=-SPRINTSPEED;
      if(aDown.get())strafeVelocity=SPRINTSPEED;
      if(dDown.get())strafeVelocity=-SPRINTSPEED;
    }
    if(staminaOut.get())
    {
      if(wDown.get())velocity=WALKINGSPEED;
      if(sDown.get())velocity=-WALKINGSPEED;
      if(aDown.get())strafeVelocity=WALKINGSPEED;
      if(dDown.get())strafeVelocity=-WALKINGSPEED;
    }
    
    updateDistance();
    //@Sarah: sets light to camera location
    light.setVisible(true);
    light.setTranslateX(camera.getTranslateX());
    light.setTranslateZ(camera.getTranslateZ());
    //@Sarah: next line of code does literally nothing. Commenting out
    //light.setRotate(camera.getRotate() - 180);
    xPos = camera.getTranslateX();
    zPos = camera.getTranslateZ();
  }

  /**
   * Get the current GraphNode object that represents the tile that the player
   * is standing on.
   * 
   * @return The GraphNode that represents the tile that the player is standing
   *         on.
   */
  public GraphNode getCurrentNode()
  {
    GraphNode currentNode = null;
    Tile currentTile = null;
    double currentX = boundingCircle.getTranslateX();
    double currentZ = boundingCircle.getTranslateZ();
    currentTile = entityManager.zombieHouse.gameBoard[(int) currentZ][(int) currentX];
    if (TileGraph.tileGraph.containsKey(currentTile))
    {
      currentNode = TileGraph.tileGraph.get(currentTile);
      return currentNode;
    }
    return currentNode;
  }

  /**
   * Get the current GraphNode object that represents the tile that the player
   * is standing on. This is the same as the previous method except that it is
   * called for the 2D board, not the 3D one.
   * 
   * @return The GraphNode that represents the tile that the player is standing
   *         on.
   */
  public GraphNode getCurrent2dNode()
  {
    GraphNode currentNode = null;
    Tile currentTile = null;
    double currentX = xPos / ZombieBoardRenderer.cellSize;
    double currentY = yPos / ZombieBoardRenderer.cellSize;
    currentTile = ZombieBoardRenderer.gameBoard[(int) currentY][(int) currentX];
    if (TileGraph.tileGraph.containsKey(currentTile))
    {
      currentNode = TileGraph.tileGraph.get(currentTile);
      return currentNode;
    }
    return currentNode;
  }
  /**
   * Plays player foot step sound
   * 
   */
  /*@Override
  public void stepSound()
  {
    //entityManager.soundManager.playSoundClip(Sound.footstep);
  }*/

  /**
   * Calculates Distance for camera
   * @return The distance between lastX/Z and Camera.getTranslateX/Z
   */
  @Override
  public double calculateDistance()
  {
    double xDist = camera.getTranslateX() - lastX;
    double zDist = camera.getTranslateZ() - lastZ;
    return Math.sqrt((xDist*xDist)+(zDist*zDist));
  }
  
  /**
   * Clears Data from previous Game
   * 
   */
  public void dispose()
  {
    camera = null;
    light = null;
    boundingCircle = null;
  }
  /**
   * 
   * @author Jeffrey McCall 
   * This class keeps track of player stamina. While the player
   * is running, the stamina is decremented until it reaches 0. At that time,
   * the player can't run until the stamina regenerates. This class takes care
   * of decrementing and regenerating stamina.
   *
   */
  private class PlayerStamina extends Thread
  {
    /**
     * Once every second, decrement stamina if shift is pressed.
     * If stamina reaches 0, regenerate stamina at a constant rate
     * once every second until stamina reaches max of 5. Exit thread if
     * program is closed. 
     */
    @Override
    public void run()
    {
      while (gameIsRunning.get() == true)
      {
        try
        {
          sleep(1000);
        } catch (InterruptedException e)
        {
          e.printStackTrace();
        }
        if(shiftPressed.get() && !staminaOut.get())
        {
          stamina--;
          if(stamina==0)
          {
            staminaOut.set(true);
          }
        }else if(!shiftPressed.get())
        {
          deltaTime++;
          if(((deltaTime*regen)+stamina)<=5)
          {
            stamina+=deltaTime*regen;
          }else
          {
            stamina=5;
            deltaTime=0;
            staminaOut.set(false);
          }
        }
      }
      System.exit(0);
    }
  }

  /**
   * @author Nick
   * @author Sarah Salmonson
   *
   * @param damage amount of damage done to the player
   *
   *This method is called when a zombie attacks a player. Not fully implemented yet.
   */
  public void getHit(int damage)
  {
    this.hitPoints -= damage;
  }

  /**
   * @author Nick Schrandt
   *
   * @return the boolean value of whether or not the attack animation is playing. Eventually this will be used to
   * stop the player from attacking while the animation is in progress.
   */
  public boolean isAttacking()
  {
    return attacking;
  }

  /**
   * @author Nick Schrandt
   *
   * @return the boolean value of whether or not the push animation is playing. Eventually this will be used to
   * stop the player from pushing or attacking while the animation is in progress.
   */
  public boolean isPushing()
  {
    return pushing;
  }

  /**
   * @author Nick Schrandt
   *
   * When the attack button is pressed, this sets attacking to true for the length of the animation.
   */
  public void setAttacking()
  {
    attacking = true;
  }

  /**
   * @author Nick Schrandt
   *
   * When the attack button is pressed, this sets attacking to true for the length of the animation.
   */
  public void setPushing()
  {
    pushing = true;
  }


  /**@author Nick Schrandt
   *
   * This method returns the cyclinder that represents the player weapon
   * @return Cylinder cudgel
   */
  public Group getWeapon()
   {
     return swordGroup;
   }

  /**
   * Public getter for Player hitpoints stat
   * @return hitPoints the double value of player's current HP
   * @author Sarah Salmonson
   */
  public double getHitPoints()
  {
    return this.hitPoints;
  }

  /**
   * Public setter for Player death status.
   * @author Nick Schrandt
   */
  public void setIsDead()
  {
    this.isDead.set(true);
    actionBehaviors.add(turnCounter, -1.0);
  }

  /**
   * Public getter for player's walk behaviors on X dimension
   * @return List of walk coordinates on X axis
   * @author Sarah Salmonson
   */
  public List<Double> getWalkBehaviorsX()
  {
    return walkBehaviorsX;
  }

  /**
   * Public getter for player's walk behaviors on Z dimension
   * @return List of walk coordinates on Z axis
   * @author Sarah Salmonson
   */
  public List<Double> getWalkBehaviorsZ()
  {
    return walkBehaviorsZ;
  }

  /**
   * Public getter for player's action behaviors
   * @return List of action behaviors
   * @author Sarah Salmonson
   */
  public List<Double> getActionBehaviors()
  {
    return actionBehaviors;
  }
}