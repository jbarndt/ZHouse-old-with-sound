package game_engine;

import entities.Player;
/**
 * @author Jeffrey McCall
 * This class handles all of the mouse input
 * into the game. When the mouse is moved, the camera
 * is rotated appropriately.
 */
import javafx.event.EventHandler;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
//import sound.Sound;

import java.awt.*;

/**
 * 
 * @author Atle Olson 
 *        Jeffrey McCall
 *        modified (and cleaned up) by Hector Carrillo using original code + ideas from Maxwell's code
 *        Nick Schrandt
 *          
 * Handles mouse events in the game. Moves the player camera to the 
 * left and right. 
 */
public class MouseEventHandler implements EventHandler<MouseEvent>
{
  // player whose camera is rotated
  private Player player;
  private ZombieHouse3d zombiehouse;

  // the center x coordinate of the current scene
  private double centerX = 0;
  private double rotationSpeed = 1.5*Math.PI;
  // the x coordinate of the mouse when it was moved
  private double currentX = 0;
  boolean robotMove = false;


  /**
   * Constructor for the program.
   *
   * @param player used to modify his angle and camera angle
   * @param zombiehouse uses scene from zombie house to change
   */
  public MouseEventHandler(Player player, ZombieHouse3d zombiehouse)
  {
    this.player = player;
    this.zombiehouse = zombiehouse;
  }

  private class myRobot extends Robot
  {
    myRobot() throws java.awt.AWTException
    {
      super();
    }

    @Override
    public synchronized void mouseMove(int x, int y) {
      super.mouseMove(x, y);
      robotMove = true;
    }
  }
  /**
   * Create a robot to reset the mouse to the middle of the screen.
   */
  private myRobot robot;
  {
    try
    {
      robot = new myRobot();
    } catch (Exception e)
    {
      System.out.println("ERROR");
      e.printStackTrace();
    }
  }

  /**
   * Handles all of the mouse movement events. Moves the camera based on if the mouse is left or right of the center.
   * This avoids moving the camera when the mouse is reset back since the mouse x coordinate will equal the center x
   * coordinate (which is neither left or right).
   *
   * @param event All mouse motion events are automatically passed into
   *              this method.
   * @authors: original code, Nick Schrandt
   */
  @Override
  public void handle(MouseEvent event)
  {
    /*
    Author: Nick Schrandt

    Added this to the handle so that the left mouse click will attack any zombies in the player's bounding circle.
    Also sets the player to attacking, preventing any further attacks until the animation is complete. Also activates
    the sword swing sound.
     */
    if(event.getEventType() == MouseEvent.MOUSE_CLICKED)
    {
      if (event.getButton() == MouseButton.PRIMARY && !player.isAttacking())
      {
        player.setAttacking();
        //zombiehouse.getEntityManager().soundManager.playSoundClip(Sound.swing);
        zombiehouse.getEntityManager().playerAttack(player.boundingCircle);
      }
    }

    // Do this if the mouse event is actually the robot moving the cursor back...
    if(robotMove)
    {
      // top corner coordinates of scene
      double topX = event.getScreenX() - event.getSceneX();

      centerX = topX + zombiehouse.scene.getWidth() / 2;

      currentX = topX + event.getSceneX();

      // cursor is to the right of the center so rotate right
      if (currentX-10 > centerX)
      {
        player.camera.setRotate(player.angle += rotationSpeed);
        player.turnRight = true;
      }
      // cursor is to the left of the center so rotate left
      else if (currentX+10 < centerX)
      {
        player.camera.setRotate(player.angle -= rotationSpeed);
        player.turnLeft = true;
      }
      robotMove = false;
    } else {
      // top corner coordinates of scene
      double topX = event.getScreenX() - event.getSceneX();
      double topY = event.getScreenY() - event.getSceneY();

      centerX = topX + zombiehouse.scene.getWidth() / 2;

      currentX = topX + event.getSceneX();

      // cursor is to the right of the center so rotate right
      if (currentX-2 > centerX) {
        player.camera.setRotate(player.angle += rotationSpeed);
        player.turnRight = true;
      }
      // cursor is to the left of the center so rotate left
      else if (currentX+2 < centerX) {
        player.camera.setRotate(player.angle -= rotationSpeed);
        player.turnLeft = true;
      }

      try {
        // Reset mouse to middle of screen
        robot.mouseMove((int) topX + (int) (zombiehouse.scene.getWidth() / 2), (int) topY +
                (int) (zombiehouse.scene.getHeight() / 2));

        currentX = topX + zombiehouse.scene.getWidth() / 2;

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}

