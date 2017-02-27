package game_engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.interactivemesh.jfx.importer.obj.ObjImportOption;
import com.interactivemesh.jfx.importer.obj.ObjModelImporter;

import entities.EntityManager;
import entities.PastSelf;
import entities.Player;
import entities.Zombie;
import graphing.GraphNode;
import graphing.TileGraph;
import gui.Main;
import javafx.animation.AnimationTimer;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import levels.ProceduralMap;
import levels.TextureMaps;
import levels.Tile;
import levels.Tile.TileType;
import sound.SoundManager;

/**
 * @author Atle Olson
 * @author Jeffrey McCall
 * @author Sarah Salmonson, Hector Carrillo, Nick Schrandt
 * This class will create a 3d representation of our game
 *
 *new: Added textures for the walls, ceiling and floor in the different regions.
 */
public class ZombieHouse3d
{
  private PerspectiveCamera camera;
  public PointLight light = new PointLight();
  public PointLight exitLight = new PointLight();
  public AnimationTimer gameLoop;
  boolean isWall;

  boolean paused = false;

  public int boardWidth;
  public int boardHeight;
  public Tile[][] gameBoard;
  private Box[][] ceilingAndWallDrawingBoard;
  private Box[][] floorDrawingBoard;
  private Node[][][] obstacleBoard;

  public ArrayList<Box> exits = new ArrayList<>();

  public Group root;

  // The list of walls used for collision detection and for location-based lighting.
  public List<Box> walls = new ArrayList<>();
  //@Sarah: list of all other tiles to be used for location-based lighting
  private List<Box> notWalls = new ArrayList<>();
  private List<Box> obstacles = new ArrayList<>();

  //private static int zombieCounter = 0;
  public int numZombies = 0;

  public int difficulty;
  public Scene scene;

  private EntityManager entityManager;
  SoundManager soundManager;
  Main main;
  Scenes scenes;

  private static final ObjModelImporter importer = new ObjModelImporter();

  private static final int TOTAL_FRAMES = 11;
  private static final int TOTAL_SELF_FRAMES = 13;
  private static final int LARGEST_ZOMBIE_FRAME = 20;
  private static final int LARGEST_PASTSELF_FRAME = 12;
  private static Random random = new Random();

  private static final String Zombie = "Resources/Meshes/Zombie/Zombie";
  private static final String PastSelf = "Resources/Meshes/Past_Self/simpleMan";

  static final String BONEPILE = "Resources/Meshes/bones/bonePile.obj";
  private static final String GUILLOTINE = "Resources/Meshes/Guillotine/guillotine.obj";
  private static final String DUNGEONPILLAR = "Resources/Meshes/DungeonPillar/dungeonPillar.obj";

  private static final String BOOKPILE = "Resources/Meshes/Books/bookPile.obj";
  private static final String CHAIR = "Resources/Meshes/chair/chair.obj";
  private static final String HOROSCOPETABLE = "Resources/Meshes/HoroscopeTable/htable.obj";

  private static final String KNIGHT2 = "Resources/Meshes/knight/knight2.obj";
  private static final String KNIGHT1 = "Resources/Meshes/knight/knight1.obj";
  private static final String TOMBSTONE = "Resources/Meshes/tombstone/Tombstone.obj";

  private static final String[] obstacleStrings = {BONEPILE, GUILLOTINE, DUNGEONPILLAR, CHAIR, BOOKPILE, HOROSCOPETABLE, KNIGHT1, KNIGHT2};

  private static final int[] r1 = {0,0,1,1,1,1,1,2,2,2};
  private static final int[] r2 = {3,3,3,3,3,4,4,4,5,5};
  private static final int[] r3 = {6,6,6,7,7,7,5};

  private static final int[][] regions = {r1,r2,r3};

  // this is to prevent obstacle meshes from being randomized after first game
  private boolean firstGame = true;
  private ArrayList<Integer> obstacleList = new ArrayList<>();
  private ArrayList<Integer> orientation = new ArrayList<>();
  private int currentObstacleIndex = 0;
  private int maxObstacleIndex = 0;

  /**
   * Constructor for ZombieHouse3d object
   * @param difficulty
   * The difficulty setting
   * @param //soundManager
   * Sound manager
   * @param main
   * Copy of Main
   * @param scenes
   * Scenes object
   *
   */
  public ZombieHouse3d(int difficulty, SoundManager soundManager, Main main, Scenes scenes)
  {
    this.difficulty = difficulty;
    this.soundManager = soundManager;
    this.main = main;
    this.scenes = scenes;
  }

  /**
   * @param input
   * The filepath to the mesh (.obj)
   * @return mesh
   * The Node[] that contains the model
   */
  public static Node[] loadMeshViews(String input)
  {
    importer.setOptions(ObjImportOption.NONE);
    importer.read(input);
    Node[] mesh = importer.getImport();
    for(int i = 0;i<mesh.length;i++)
    {
      mesh[i].setTranslateY(1);
      mesh[i].setScaleX(1);
      mesh[i].setScaleY(1);
      mesh[i].setScaleZ(1);
      mesh[i].setCache(true);
      mesh[i].setCacheHint(CacheHint.SPEED);
    }
    for (int i = 0; i < mesh.length; i++)
    {
      mesh[i].setRotationAxis(Rotate.Y_AXIS);
    }
    importer.clear();
    return mesh;
  }

  /**
   * @return group
   * the Group that is used by zombieHouse3d to initialize content
   */
  public Parent createContent() throws Exception
  {
    root = new Group();
    root.setCache(true);
    root.setCacheHint(CacheHint.SPEED);

    // initialize entity manager if not already initialized
    if(entityManager == null)
    {
      entityManager = new EntityManager(soundManager, main, scenes);
    }
    else
    {
      entityManager.resetEM();
    }
    entityManager.setZombieHouse3d(this);
    entityManager.createZombies(gameBoard, boardHeight, boardWidth);
    numZombies = entityManager.zombies.size();

    // Initialize camera
    camera = new PerspectiveCamera(true);
    camera.getTransforms().addAll(new Rotate(0, Rotate.Y_AXIS),
        new Rotate(0, Rotate.X_AXIS), new Translate(0, -.5, 0));
    camera.setFieldOfView(70);
    camera.setFarClip(7);
    camera.setRotationAxis(Rotate.Y_AXIS);

    // Initialize player
    entityManager.player = new Player(3, 0, 3, camera, entityManager, light);
    entityManager.player.camera = camera;
    root.getChildren().add(entityManager.player.getWeapon());

    // Lighting
    root.getChildren().add(entityManager.player.light);

    obstacleBoard = new Node[boardHeight][boardWidth][];
    // Build the Scene Graph
    //modified by Sarah Salmonson to "create" a new instance of phongmaterial for each wall, floor, or ceiling tile,
    //rather than using the same shared copy and repeating it, which was a huge problem when implementing lighting
    for (int col = 0; col < boardHeight; col++)
    {
      for (int row = 0; row < boardWidth; row++)
      {
        ceilingAndWallDrawingBoard[col][row] = new Box(1, 0, 1);
        floorDrawingBoard[col][row] = new Box(1, 0, 1);
        switch (gameBoard[col][row].type)
        {
          case wall:
            if(!gameBoard[col][row].isObstacle)
            {
              Tile currentTile = gameBoard[col][row];
              ceilingAndWallDrawingBoard[col][row] = new Box(1, 2, 1);
              if(currentTile.isBorder || currentTile.getRegion() == 1)
              {
                ceilingAndWallDrawingBoard[col][row].setMaterial(TextureMaps.createStoneFloor());
              }
              if(!currentTile.isBorder && currentTile.getRegion() == 2)
              {
                ceilingAndWallDrawingBoard[col][row].setMaterial(TextureMaps.createBooks());
              }
              if(!currentTile.isBorder && currentTile.getRegion() == 3)
              {
                ceilingAndWallDrawingBoard[col][row].setMaterial(TextureMaps.createMarble());
              }
              if(!currentTile.isBorder && currentTile.getRegion() == 4)
              {
                ceilingAndWallDrawingBoard[col][row].setMaterial(TextureMaps.createHedge());
              }
            }
            else //is obstacle
            {
              makeObstacle(gameBoard[col][row], ceilingAndWallDrawingBoard[col][row], floorDrawingBoard[col][row],  col, row);
            }
            break;
          case region1:
            ceilingAndWallDrawingBoard[col][row].setMaterial(TextureMaps.createStoneFloor());
            floorDrawingBoard[col][row].setMaterial(TextureMaps.createStoneFloor());
            break;
          case region2:
            ceilingAndWallDrawingBoard[col][row].setMaterial(TextureMaps.createWoodCeiling());
            floorDrawingBoard[col][row].setMaterial(TextureMaps.createCarpet());
            break;
          case region3:
            ceilingAndWallDrawingBoard[col][row].setMaterial(TextureMaps.createParlorCeiling());
            floorDrawingBoard[col][row].setMaterial(TextureMaps.createTile());
            break;
          case region4:
            ceilingAndWallDrawingBoard[col][row].setMaterial(TextureMaps.createPurgolaCeiling());
            floorDrawingBoard[col][row].setMaterial(TextureMaps.createPavestone());
            break;
          case exit:
            ceilingAndWallDrawingBoard[col][row].setMaterial(TextureMaps.createIron());
            floorDrawingBoard[col][row].setMaterial(TextureMaps.createIron());
            Box box = new Box(1,2,1);
            box.setTranslateX(gameBoard[col][row].zPos);
            box.setTranslateZ(gameBoard[col][row].xPos);
            box.setMaterial(TextureMaps.createGlow());
            exits.add(box);

            break;
        }
        if (col == 0 || col == boardHeight - 1 || row == 0
                || row == boardWidth - 1)
        {
          ceilingAndWallDrawingBoard[col][row].setTranslateX(row + .5);
          ceilingAndWallDrawingBoard[col][row].setTranslateZ(col + .5);
          floorDrawingBoard[col][row].setTranslateX(row + .5);
          floorDrawingBoard[col][row].setTranslateZ(col + .5);
        } else
        {
          ceilingAndWallDrawingBoard[col][row]
                  .setTranslateX(gameBoard[col][row].xPos);
          ceilingAndWallDrawingBoard[col][row]
                  .setTranslateZ(gameBoard[col][row].zPos);
          floorDrawingBoard[col][row]
                  .setTranslateX(gameBoard[col][row].xPos);
          floorDrawingBoard[col][row]
                  .setTranslateZ(gameBoard[col][row].zPos);
        }
        if (!gameBoard[col][row].type.equals(TileType.wall))
        {
          ceilingAndWallDrawingBoard[col][row].setTranslateY(-1);
          floorDrawingBoard[col][row].setTranslateY(1);
        }

        root.getChildren().add(ceilingAndWallDrawingBoard[col][row]);
        root.getChildren().add(floorDrawingBoard[col][row]);
      }
    }
    // Spawn zombies on board and create list of wall tiles for
    // purposes of collision detection.
    for (int col = 0; col < boardHeight; col++)
    {
      for (int row = 0; row < boardWidth; row++)
      {
        if (gameBoard[col][row].getType().equals("wall") && !gameBoard[col][row].isObstacle)
        {
          walls.add(ceilingAndWallDrawingBoard[col][row]);
          entityManager.numTiles++;
          isWall = true;
        }
        //@Sarah: build tile collection of all other tile types
        else
        {
          notWalls.add(ceilingAndWallDrawingBoard[col][row]);
          notWalls.add(floorDrawingBoard[col][row]);
          isWall = false;
        }

        // The following code calls the appropriate methods to build the graph
        // to be used in zombie pathfinding.
        if (col == 0 && row == 0)
        {
          GraphNode newNode = new GraphNode(gameBoard[col + 1][row],
                  gameBoard[col][row + 1], gameBoard[col + 1][row + 1], row, col,
                  isWall, gameBoard[col][row]);
          TileGraph.createGraph(newNode);
        }
        if (col == 0 && row == boardWidth - 1)
        {
          GraphNode newNode = new GraphNode(gameBoard[col + 1][row],
                  gameBoard[col][row - 1], gameBoard[col + 1][row - 1], row, col,
                  isWall, gameBoard[col][row]);
          TileGraph.createGraph(newNode);
        }
        if (col == boardHeight - 1 && row == 0)
        {
          GraphNode newNode = new GraphNode(gameBoard[col - 1][row],
                  gameBoard[col][row + 1], gameBoard[col - 1][row + 1], row, col,
                  isWall, gameBoard[col][row]);
          TileGraph.createGraph(newNode);
        }
        if (col == boardHeight - 1 && row == boardWidth - 1)
        {
          GraphNode newNode = new GraphNode(gameBoard[col - 1][row],
                  gameBoard[col][row - 1], gameBoard[col - 1][row - 1], row, col,
                  isWall, gameBoard[col][row]);
          TileGraph.createGraph(newNode);
        }
        if (row == 0 && col != 0 && col != boardHeight - 1)
        {
          GraphNode newNode = new GraphNode(gameBoard[col + 1][row],
                  gameBoard[col - 1][row], gameBoard[col][row + 1],
                  gameBoard[col + 1][row + 1], gameBoard[col - 1][row + 1], row,
                  col, isWall, gameBoard[col][row]);
          TileGraph.createGraph(newNode);
        }
        if (row == boardWidth - 1 && col != 0 && col != boardHeight - 1)
        {
          GraphNode newNode = new GraphNode(gameBoard[col + 1][row],
                  gameBoard[col - 1][row], gameBoard[col][row - 1],
                  gameBoard[col + 1][row - 1], gameBoard[col - 1][row - 1], row,
                  col, isWall, gameBoard[col][row]);
          TileGraph.createGraph(newNode);
        }
        if (col == 0 && row != 0 && row != boardWidth - 1)
        {
          GraphNode newNode = new GraphNode(gameBoard[col + 1][row],
                  gameBoard[col][row + 1], gameBoard[col][row - 1],
                  gameBoard[col + 1][row + 1], gameBoard[col + 1][row - 1], row,
                  col, isWall, gameBoard[col][row]);
          TileGraph.createGraph(newNode);
        }
        if (col == boardHeight - 1 && row != 0 && row != boardWidth - 1)
        {
          GraphNode newNode = new GraphNode(gameBoard[col][row + 1],
                  gameBoard[col - 1][row], gameBoard[col][row - 1],
                  gameBoard[col - 1][row + 1], gameBoard[col - 1][row - 1], row,
                  col, isWall, gameBoard[col][row]);
          TileGraph.createGraph(newNode);
        }
        if (col >= 1 && col < boardHeight - 1 && row >= 1
                && row < boardWidth - 1)
        {
          GraphNode newNode = new GraphNode(gameBoard[col + 1][row],
                  gameBoard[col - 1][row], gameBoard[col][row + 1],
                  gameBoard[col][row - 1], gameBoard[col + 1][row + 1],
                  gameBoard[col + 1][row - 1], gameBoard[col - 1][row + 1],
                  gameBoard[col - 1][row - 1], row, col, isWall,
                  gameBoard[col][row]);
          TileGraph.createGraph(newNode);
        }
      }
    }
    firstGame = false;
    System.out.println("Number of Zombies: " + entityManager.zombies.size());

    makeCreatureMeshGroups();

    exitLight = new PointLight();
    exitLight.setTranslateX(exits.get(0).getTranslateX());
    exitLight.setTranslateZ(exits.get(0).getTranslateZ());
    root.getChildren().addAll(exits);
    //root.getChildren().add(exitLight);

    // Use a SubScene
    SubScene subScene = new SubScene(root, 1280, 800, true,
        SceneAntialiasing.BALANCED);
    //@author: Sarah set subScene color to BLACK to prevent the BLUE rectangles in the distance
    subScene.setFill(Color.BLACK);
    subScene.setCamera(camera);
    subScene.setCursor(Cursor.NONE);

    Group group = new Group();
    group.getChildren().add(subScene);
    group.addEventFilter(
        MouseEvent.MOUSE_MOVED,
        new MouseEventHandler(entityManager.player, this)
        );

    /*
    Nick Schrandt

    Added this so that when the mouse is clicked, the eventhandler is called.
     */
    group.addEventFilter(
        MouseEvent.MOUSE_CLICKED,
        new MouseEventHandler(entityManager.player, this)
        );

    return group;
  }

  /*
 Hector Carrillo

 creates an abstacle using a mesh and a collision box, can easily be made to work with different regions
  */
  private void makeObstacle(Tile gameBoardTile, Box floorBox, Box roofBox, int col, int row)
  {
    Box collisionBox = new Box(1,2,1);
    collisionBox.setVisible(false);
    int randomRotateModifier;

    Node[] obstacleMesh;
    if(firstGame) {
      randomRotateModifier = random.nextInt(4);
      orientation.add(randomRotateModifier);
    } else {
      randomRotateModifier = orientation.get(currentObstacleIndex);
    }

    int region = gameBoardTile.getRegion();

    // floor tiles above and below mesh
    if(region == 1)
    {
      floorBox.setMaterial(TextureMaps.createStoneFloor());
      roofBox.setMaterial(TextureMaps.createStoneFloor());
    }
    if(region == 2)
    {
      floorBox.setMaterial(TextureMaps.createWoodCeiling());
      roofBox.setMaterial(TextureMaps.createCarpet());
    }
    if(region == 3)
    {
      floorBox.setMaterial(TextureMaps.createParlorCeiling());
      roofBox.setMaterial(TextureMaps.createTile());
    }
    if(region == 4)
    {
      floorBox.setMaterial(TextureMaps.createPurgolaCeiling());
      roofBox.setMaterial(TextureMaps.createPavestone());
    }

    if(firstGame) {
      if (region < 4) {
        int[] distribution = regions[region - 1];
        int dLength = distribution.length;
        int meshIndex = distribution[random.nextInt(dLength)];
        obstacleList.add(meshIndex);
        obstacleMesh = loadMeshViews(obstacleStrings[meshIndex]);
        ++maxObstacleIndex;
      }
      else obstacleMesh = loadMeshViews(TOMBSTONE);
    }
    else {
      if(region < 4) {
        int meshIndex = obstacleList.get(currentObstacleIndex);
        obstacleMesh = loadMeshViews(obstacleStrings[meshIndex]);
        currentObstacleIndex++;
      } else obstacleMesh = loadMeshViews(TOMBSTONE);
    }

    if(currentObstacleIndex >= maxObstacleIndex)
    {
      currentObstacleIndex = 0;
    }

    obstacleBoard[col][row] = obstacleMesh;
    root.getChildren().addAll(obstacleMesh);
    root.getChildren().add(collisionBox);

    for(int i = 0; i < obstacleMesh.length; i++)
    {
      obstacleMesh[i].setTranslateZ(gameBoard[col][row].zPos);
      obstacleMesh[i].setTranslateX(gameBoard[col][row].xPos);
      obstacleMesh[i].setRotationAxis(Rotate.Y_AXIS);
      obstacleMesh[i].setRotate(randomRotateModifier*90);
    }

    collisionBox.setTranslateZ(gameBoard[col][row].zPos);
    collisionBox.setTranslateX(gameBoard[col][row].xPos);
    obstacles.add(collisionBox);

    floorBox.setTranslateY(-1);
    roofBox.setTranslateY(1);

    notWalls.add(floorBox);
    notWalls.add(roofBox);
  }

  /**@aurhor Hector Carrillo and Nick Schrandt
   *
   * Nick wrote this method, but the logic is all adapted from Hector's meshZombie method.
   *
   * @param self version of PastSelf player that needs to be rendered.
   */
  public void makePastSelf(PastSelf self)
  {
    Node[] playerMesh;
    for(int currentFrame = 0; currentFrame<= LARGEST_PASTSELF_FRAME; currentFrame++)
    {
      if(currentFrame < 10)
      {
        playerMesh = loadMeshViews(PastSelf + "0" + currentFrame + ".obj");
        for(int i = 0; i < playerMesh.length; i++)
        {
          playerMesh[i].setVisible(false);
        }
        self.selfMeshes.getChildren().addAll(playerMesh);
        self.selfMeshes.setScaleX(.4);
        self.selfMeshes.setScaleY(.4);
        self.selfMeshes.setScaleZ(.4);
        self.selfMeshes.setTranslateY(-.5);
      }
      else
      {
        playerMesh = loadMeshViews(PastSelf + currentFrame + ".obj");

        for(int j = 0; j < playerMesh.length; j++)
        {
          playerMesh[j].setVisible(false);
        }

        self.selfMeshes.getChildren().addAll(playerMesh);
      }
    }
    self.currentFrame = 1 + random.nextInt(TOTAL_SELF_FRAMES -2);
    self.selfMeshes.getChildren().get(self.currentFrame).setVisible(true);
    self.selfMeshes.setRotationAxis(Rotate.Y_AXIS);
    root.getChildren().addAll(self.selfMeshes);
  }

  /**@aurhor Hector Carrillo and Nick Schrandt
   *
   * Nick wrote this method, but the logic is all adapted from Hector's meshZombie method.
   *
   * @param zombie version of PastSelf zombie that needs to be rendered.
   */
  public void makePastZombie(PastSelf zombie)
  {
    Node[] zombieMesh;
    for(int currentFrame = 0; currentFrame<= LARGEST_ZOMBIE_FRAME; currentFrame++)
    {
      if(currentFrame < 10)
      {
        zombieMesh = loadMeshViews(Zombie + "0" + currentFrame + ".obj");
        for(int i = 0; i < zombieMesh.length; i++)
        {
          zombieMesh[i].setVisible(false);
        }
        zombie.selfMeshes.getChildren().addAll(zombieMesh);
      }
      else
      {
        zombieMesh = loadMeshViews(Zombie + currentFrame + ".obj");

        for(int j = 0; j < zombieMesh.length; j++)
        {
          zombieMesh[j].setVisible(false);
        }

        zombie.selfMeshes.getChildren().addAll(zombieMesh);
      }
    }
    zombie.currentFrame = 1 + random.nextInt(TOTAL_FRAMES -2);
    zombie.selfMeshes.getChildren().get(zombie.currentFrame).setVisible(true);
    zombie.selfMeshes.setRotationAxis(Rotate.Y_AXIS);
    root.getChildren().addAll(zombie.selfMeshes);
  }

  /**
   * @author Hector Carrillo
   * Addes all the meshes for an animation sequence to all the zombies & pastSelves
   */
  private void makeCreatureMeshGroups()
  {
    for (Zombie zombie: entityManager.zombies)
    {
      meshZombie(zombie);
    }
    for (PastSelf pastSelf : entityManager.pastSelves)
    {
      pastSelf.selfMeshes.setVisible(true);
      if(pastSelf.isZombie())
      {
        makePastZombie(pastSelf);
      }
      else
      {
        makePastSelf(pastSelf);
      }
    }
  }

  /**@author Hector Carillo
   *
   * note: I pulled this method out of makeZombieMeshGroup so I could call it when making a bifurcated zombie. -Nick
   *
   * @param zombie to be meshed
   */
  public void meshZombie(Zombie zombie) {
    Node[] zombieMesh;
    for(int currentFrame = 0; currentFrame <= LARGEST_ZOMBIE_FRAME; currentFrame+=2)
    {
      if(currentFrame < 10)
      {
        zombieMesh = loadMeshViews(Zombie + "0" + currentFrame + ".obj");
        for(int j = 0; j < zombieMesh.length; j++)
        {
          zombieMesh[j].setVisible(false);
        }
        zombie.zombieMeshes.getChildren().addAll(zombieMesh);
      }
      else
      {
        zombieMesh = loadMeshViews(Zombie + currentFrame + ".obj");

        for(int j = 0; j < zombieMesh.length; j++)
        {
          zombieMesh[j].setVisible(false);
        }

        zombie.zombieMeshes.getChildren().addAll(zombieMesh);
      }
      // makes master zombie bigger
      if(zombie.isMasterZombie)
      {
        for(int i = 0; i< zombieMesh.length; i++)
        {
          zombieMesh[i].setScaleX(2);
          zombieMesh[i].setScaleY(1.1);
          zombieMesh[i].setScaleZ(1.1);
          zombieMesh[i].setTranslateY(1);
        }
      }
    }
    // Each zombie starts at a random frame so that they don't all look alike
    // Does not start at 0 frame because it causes a bug
    zombie.currentFrame = 1 + random.nextInt(TOTAL_FRAMES -2);
    zombie.zombieMeshes.getChildren().get(zombie.currentFrame).setVisible(true);
    zombie.zombieMeshes.setRotationAxis(Rotate.Y_AXIS);
    root.getChildren().addAll(zombie.zombieMeshes);
    root.getChildren().add(zombie.getHealthBar());
  }

  /**
   * @author Hector Carrillo
   * this bit of code is so that the engine only renders objects close to the player to prevent lagging
   */
  private void gameDistanceBasedRendering(int distanceSquared)
  {
    // used for distance calculation
    double distanceFromPlayerSquared;
    double playerZ = entityManager.player.boundingCircle.getTranslateZ();
    double playerX = entityManager.player.boundingCircle.getTranslateX();
    double zombieZ;
    double zombieX;
    double pastSelfZ;
    double pastSelfX;

    root.getChildren().clear();
    root.getChildren().add(entityManager.player.swordGroup);

    // calculates distances to static objects if the distance squared to the player is less than 60 it is rendered
    for(int col = 0; col < boardHeight; col++)
    {
      for(int row = 0; row < boardWidth; row++)
      {
        distanceFromPlayerSquared = ((col-playerZ) * (col-playerZ)) + ((row-playerX) * (row-playerX));

        if(distanceFromPlayerSquared < distanceSquared) {
          root.getChildren().add(floorDrawingBoard[col][row]);
          root.getChildren().add(ceilingAndWallDrawingBoard[col][row]);
          if(obstacleBoard[col][row] != null) {
            root.getChildren().addAll(obstacleBoard[col][row]);
          }
        }
      }
    }

    // calculates distance to zombies
    for(Zombie zombie : entityManager.zombies)
    {
      zombieZ = zombie.zombieCylinder.getTranslateZ();
      zombieX = zombie.zombieCylinder.getTranslateX();
      distanceFromPlayerSquared = ((zombieZ-playerZ) * (zombieZ-playerZ)) + ((zombieX-playerX) * (zombieX-playerX));
      if(distanceFromPlayerSquared < distanceSquared) {
        root.getChildren().addAll(zombie.zombieMeshes);
        root.getChildren().add(zombie.getHealthBar());
      }
    }

    // calculates distance to zombies
    for(PastSelf pastSelf : entityManager.pastSelves)
    {
      pastSelfZ = pastSelf.getBoundingCircle().getTranslateZ();
      pastSelfX = pastSelf.getBoundingCircle().getTranslateX();
      distanceFromPlayerSquared = ((pastSelfZ-playerZ) * (pastSelfZ-playerZ)) + ((pastSelfX-playerX) * (pastSelfX-playerX));
      if(distanceFromPlayerSquared < distanceSquared) {
        root.getChildren().addAll(pastSelf.selfMeshes);
      }
    }

    // exit distance calculation
    double exitX = exits.get(0).getTranslateX();
    double exitZ = exits.get(0).getTranslateZ();
    distanceFromPlayerSquared = ((exitZ-playerZ) * (exitZ-playerZ)) + ((exitX-playerX) * (exitX-playerX));
    if(distanceFromPlayerSquared < distanceSquared) {
      root.getChildren().addAll(exits);
    }
  }

  /**
   * The animation timer used in running the game.
   *
   */
  private class MainGameLoop extends AnimationTimer
  {
    int timekeeper = 30;
    /**
     * Call the appropriate method to update the attributes of the
     * entities in the game.
     */
    public void handle(long now)
    {
      // only update every second (player range is so limited this might not hurt)
      ++timekeeper;
      if(timekeeper == 30)
      {
        gameDistanceBasedRendering(110);
        timekeeper = 0;
      }
      if(!paused && !entityManager.player.isDead.get())
      {
        entityManager.tick();
      }
      else
      {
        entityManager.player.tick();
      }
    }
  }

  /**
   * @param scenes
   *        The scenes into which all of the attributes of the game
   *        are being placed and rendered.
   * @return scene
   *         Returns the scene that is our game
   */
  public Scene zombieHouse3d(Scenes scenes) throws Exception
  {
    //3D Game Scene... We're unable to do this in "Scenes" class due to the way legacy code is structured
    scenes.threeDGameRoot = new StackPane();
    scenes.threeDGameRoot.setPrefSize(scenes.winW, scenes.winH);
    //@Sarah: clear stage's scene's nodes in case we're starting from a previous level
    scenes.threeDGameRoot.getChildren().clear();
    //call createContent on cleared scene
    scenes.threeDGameRoot.getChildren().add(createContent());

    //adds root to scene
    scene = new Scene(scenes.threeDGameRoot);

    scene.addEventHandler(KeyEvent.KEY_PRESSED,
        new KeyboardEventHandler(camera, entityManager.player, this));
    scene.addEventHandler(KeyEvent.KEY_RELEASED,
        new KeyboardEventHandler(camera, entityManager.player, this));

    gameLoop = new MainGameLoop();
    gameLoop.start();
    return scene;
  }

  /**
   * Generates gameBoard and builds 3D wall, ceiling, and floor objects
   */
  void build3DMap ()
  {
    gameBoard = ProceduralMap.generateMap(Attributes.Map_Width, Attributes.Map_Height, difficulty);
    boardWidth = gameBoard[0].length;
    boardHeight = gameBoard.length;
    ceilingAndWallDrawingBoard = new Box[boardWidth][boardHeight];
    floorDrawingBoard = new Box[boardWidth][boardHeight];
  }

  /**
   * Delete game data after game has ended. Used when starting new game
   */
  public void dispose()
  {
    gameLoop.stop();
    entityManager = null;
    scene = null;
    camera = null;
    light = null;
    gameBoard = null;
    walls.clear();
    exits.clear();
    root.getChildren().clear();
  }


  /**
   * In the interests of beginning to encapsulate this messy legacy code, I set the camera to private and created
   * this getter so that other classes, like the EntityManager, can use player's field of view and far clip settings
   * to create lighting and other spooky effects
   * @author: Sarah Salmonson
   * @return PerspectiveCamera
   */
  public PerspectiveCamera getCamera()
  {
    return this.camera;
  }
  /**
   * Getter for the ArrayList notWalls which contains all Tile objects that are not wall tiles
   * @author: Sarah Salmonson
   * @return List of all Tile objects that are not wall tiles
   */
  public List<Box> getNotWalls()
  {
    return this.notWalls;
  }

  public List<Box> getWalls() { return this.walls; }

  public List<Box> getObstacles() { return this.obstacles; }
  /**
   * Getter for the game's EntityManager
   * @author Sarah Salmonson
   * @return EntityManager
   */
  public EntityManager getEntityManager()
  {
    return this.entityManager;
  }
}