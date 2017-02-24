package game_engine;

import gui.Main;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
//import sound.Sound;
//import sound.SoundManager;
import utilities.MapViewerScene;

/**
 * @author Atle Olson
 * A place to stage Scenes and switch them out
 * 
 */
public class Scenes 
{
  //private SoundManager soundManager;
  private Main main;
  
  int winW = 1280;
  int winH = 800;
  
  public Button returnButton        = new Button();
  public Button returnButtonDeath   = new Button();
  public Button goTo3dGame          = new Button();
  public Button goTo3dGameDeath     = new Button();
  Button goToMapViewer = new Button();
  Button goToGameOver  = new Button();
  Button goToWin       = new Button();
  Button goToSettings  = new Button();

  //@Sarah: label for player's hp
  private Label playerHitPoints = new Label();
  
  Slider playerHearing      = new Slider(0, 50, 1);
  Slider playerWalkingSpeed = new Slider(0, 2, 0.16);
  Slider playerSprintSpeed  = new Slider(0, 2, 0.25);
  Slider playerStamina      = new Slider(0, 10, 5);
  Slider playerRegen        = new Slider(0, 2, 0.2);
  Slider zombieSmell        = new Slider(0, 30, 15);
  Slider maxZombies         = new Slider(0, 30, 20);
  Slider mapWidth           = new Slider(0, 100, 50);
  Slider mapHeight          = new Slider(0, 100, 50);
  Slider rotateSensitivity  = new Slider(0, 20, 5);
  
  public ZombieHouse3d threeDGameObject = new ZombieHouse3d(0, /*soundManager,*/ main, this);
  private MapViewerScene mapObject = new MapViewerScene();
  public BorderPane startRoot, twoDGameRoot, settingsRoot, gameOverRoot, loadRoot, winRoot;
  public StackPane threeDGameRoot;
  public Scene mainMenu, twoDGame, gameOver, loading, win, settings;


  /**
   * @param primaryStage
   * @param main
   * 
   * Constructor for a scenes object
   */
  public Scenes(Stage primaryStage, Main main)
  {
    this.main = main;
    
    returnButton.setText("Back to main menu.");
    returnButton.setOnAction(new EventHandler<ActionEvent>() 
    {
      @Override
      public void handle(ActionEvent event) 
      {
        playButtonSound();
        //soundManager.playTrack(0);

        try
        {
          main.assignStage(mainMenu);
        } 
        catch (Exception e) 
        {
          e.printStackTrace();
        }
      }
    });

    returnButtonDeath.setText("Back to main menu.");
    returnButtonDeath.setOnAction(new EventHandler<ActionEvent>() 
    {
      @Override
      public void handle(ActionEvent event) 
      {
        playButtonSound();
        //soundManager.playTrack(0);
        try
        {
          main.assignStage(mainMenu);
        } 
        catch (Exception e)
        {
          e.printStackTrace();
        }
        threeDGameObject.getEntityManager().destroyZombieHouse();

      }
    });

    goTo3dGame.setText("Play 3d ZombieHouse!");
    goTo3dGame.setOnAction(new EventHandler<ActionEvent>() 
    {
      @Override
      public void handle(ActionEvent event) 
      {
        playButtonSound();
        createNewGameBoard(2);
        threeDGameObject.build3DMap();
        setUpNewGame(primaryStage);
      }
    });

    goTo3dGameDeath.setText("Try Again?");
    goTo3dGameDeath.setOnAction(new EventHandler<ActionEvent>()
    {
      @Override
      public void handle(ActionEvent event) 
      {
        playButtonSound();
        setUpNewGame(primaryStage);
        //soundManager.playTrack(0);
      }
    });

    
    goToMapViewer.setText("Map Viewer");
    goToMapViewer.setOnAction(new EventHandler<ActionEvent>() 
    {
      @Override
      public void handle(ActionEvent event) 
      {
        playButtonSound();
        try 
        {
          main.assignStage(mapObject.mapViewerScene(primaryStage, threeDGameObject));
          MapViewerScene.root.getChildren().add(returnButton);
        }
        catch (Exception e) 
        {
          e.printStackTrace();
        }
        
      }
    });

    goToGameOver.setText("Game Over!");
    goToGameOver.setOnAction(new EventHandler<ActionEvent>() 
    {
      @Override
      public void handle(ActionEvent event) 
      {
        playButtonSound();
        try 
        {
          gameOverRoot.setTop(returnButton);
          main.assignStage(gameOver);
        } 
        catch (Exception e) 
        {
          e.printStackTrace();
        }
        
      }
    });
    
    goToWin.setText("Win Screen");
    goToWin.setOnAction(new EventHandler<ActionEvent>() 
    {
      @Override
      public void handle(ActionEvent event) 
      {
        playButtonSound();
        try 
        {
          winRoot.setTop(returnButton);
          main.assignStage(win);
        } 
        catch (Exception e) 
        {
          e.printStackTrace();
        }
        
      }
    });
    
    goToSettings.setText("Settings");
    goToSettings.setOnAction(new EventHandler<ActionEvent>() 
    {
      @Override
      public void handle(ActionEvent event) 
      {
        playButtonSound();
        try 
        {
          settingsRoot.setTop(returnButton);
          main.assignStage(settings);
        } 
        catch (Exception e) 
        {
          e.printStackTrace();
        }
        
      }
    });
    
    playerHearing.setShowTickMarks(true);
    playerHearing.setShowTickLabels(true);
    
    playerWalkingSpeed.setShowTickMarks(true);
    playerWalkingSpeed.setShowTickLabels(true);
    
    playerSprintSpeed.setShowTickMarks(true);
    playerSprintSpeed.setShowTickLabels(true);
    
    playerStamina.setShowTickMarks(true);
    playerStamina.setShowTickLabels(true);
    
    playerRegen.setShowTickMarks(true);
    playerRegen.setShowTickLabels(true);
    
    zombieSmell.setShowTickMarks(true);
    zombieSmell.setShowTickLabels(true);
    
    maxZombies.setShowTickMarks(true);
    maxZombies.setSnapToTicks(true);
    maxZombies.setShowTickLabels(true);
    maxZombies.setMajorTickUnit(5);
    
    mapWidth.setShowTickMarks(true);
    mapWidth.setSnapToTicks(true);
    mapWidth.setShowTickLabels(true);
    mapWidth.setMajorTickUnit(20);
    
    mapHeight.setShowTickMarks(true);
    mapHeight.setSnapToTicks(true);
    mapHeight.setShowTickLabels(true);
    mapHeight.setMajorTickUnit(20);
    
    rotateSensitivity.setShowTickMarks(true);
    rotateSensitivity.setSnapToTicks(true);
    rotateSensitivity.setShowTickLabels(true);
    rotateSensitivity.setMajorTickUnit(4);
    
    playerHearing.valueProperty().addListener(new ChangeListener<Number>()
    {
      public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) 
      {
        Attributes.Player_Hearing = playerHearing.getValue();
      }
    });
    playerWalkingSpeed.valueProperty().addListener(new ChangeListener<Number>()
    {
      public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) 
      {
        Attributes.Player_Walking_Speed = playerWalkingSpeed.getValue();
      }
    });
    playerSprintSpeed.valueProperty().addListener(new ChangeListener<Number>()
    {
      public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) 
      {
        Attributes.Player_Sprint_Speed = playerSprintSpeed.getValue();
      }
    });
    playerStamina.valueProperty().addListener(new ChangeListener<Number>()
    {
      public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) 
      {
        Attributes.Player_Stamina = playerStamina.getValue();
      }
    });
    playerRegen.valueProperty().addListener(new ChangeListener<Number>()
    {
      public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) 
      {
        Attributes.Player_Regen = playerRegen.getValue();
      }
    });
    zombieSmell.valueProperty().addListener(new ChangeListener<Number>()
    {
      public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) 
      {
        Attributes.Zombie_Smell = zombieSmell.getValue();
      }
    });
    maxZombies.valueProperty().addListener(new ChangeListener<Number>()
    {
      public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) 
      {
        Attributes.Max_Zombies = maxZombies.getValue();
      }
    });
    mapWidth.valueProperty().addListener(new ChangeListener<Number>()
    {
      public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) 
      {
        Attributes.Map_Width = (int) mapWidth.getValue();
      }
    });
    mapHeight.valueProperty().addListener(new ChangeListener<Number>()
    {
      public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) 
      {
        Attributes.Map_Height = (int) mapHeight.getValue();
      }
    });
    rotateSensitivity.valueProperty().addListener(new ChangeListener<Number>()
    {
      public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) 
      {
        Attributes.Player_Rotate_sensitivity = rotateSensitivity.getValue();
      }
    });
    
    //Main menu Scene
    startRoot = new BorderPane();
    startRoot.setStyle("-fx-background-image: url(\"/Images/background.png\");-fx-background-size: 1280, 800;-fx-background-repeat: no-repeat;");
    startRoot.setPrefSize(winW, winH);
    VBox buttonVBox = new VBox();
    buttonVBox.getChildren().addAll(
        goTo3dGame,
        goToMapViewer,
        goToGameOver, 
        goToWin,
        goToSettings
        );
    buttonVBox.setSpacing(5);
    buttonVBox.setPadding(new Insets(5, 5, 5, 5)); 
    startRoot.setCenter(buttonVBox);
    
    //2D Game Scene
    twoDGameRoot = new BorderPane();
    twoDGameRoot.setPrefSize(winW, winH);
   
    //Game over Scene
    gameOverRoot = new BorderPane();
    gameOverRoot.setStyle("-fx-background-image: url(\"/Images/zombie_1_by_pophipi.jpg\");-fx-background-size: 1280, 800;-fx-background-repeat: no-repeat;");
    gameOverRoot.setPrefSize(winW, winH);
    Label gameOverLabel = new Label("Game Over!");
    gameOverLabel.setStyle("-fx-font: 100px Tahoma;");
    gameOverLabel.setTextFill(Color.RED);
    gameOverRoot.setCenter(gameOverLabel);
    HBox hBoxGameOver = new HBox();
    hBoxGameOver.getChildren().addAll(returnButton,goTo3dGameDeath);
    gameOverRoot.setTop(hBoxGameOver);
    
    //loading Scene
    loadRoot = new BorderPane();
    loadRoot.setPrefSize(winW, winH);
    loadRoot.setCenter(new Label("Loading screen!"));
    
    //Win Scene
    winRoot = new BorderPane();
    winRoot.setPrefSize(winW, winH);
    winRoot.setCenter(new Label("You Won! " + this.threeDGameObject.difficulty));
    HBox hBoxWin = new HBox();
    hBoxWin.getChildren().addAll(returnButton);
    gameOverRoot.setTop(hBoxWin);
    
    //Settings Scene
    settingsRoot = new BorderPane();
    settingsRoot.setPrefSize(winW, winH);
    settingsRoot.setCenter(new Label("Settings!"));
    
    VBox sliders = new VBox();
    sliders.getChildren().addAll(
        new Label("Player Hearing:"),
        playerHearing, 
        new Label("Player Walking Speed"),
        playerWalkingSpeed,
        new Label("Player Sprint Speed"),
        playerSprintSpeed,
        new Label("Player Stamina"),
        playerStamina,
        new Label("Player Regen"),
        playerRegen,
        new Label("Zombie Smell"),
        zombieSmell,
        new Label("Max Zombies"),
        maxZombies,
        new Label("Map Width"),
        mapWidth,
        new Label("Map Height"),
        mapHeight,
        new Label("Rotate Sensitivity"),
        rotateSensitivity
        );
    sliders.setSpacing(5);
    sliders.setPadding(new Insets(10, 20, 10, 20)); 
    settingsRoot.setLeft(sliders);
    
    mainMenu   = new Scene(startRoot);
    twoDGame   = new Scene(twoDGameRoot);
    gameOver   = new Scene(gameOverRoot);
    loading    = new Scene(loadRoot);
    win        = new Scene(winRoot);
    settings   = new Scene(settingsRoot);

  }

  /**
   * Create the HUD for the in-game screen
   * @author Sarah Salmonson
   */
  private void createGameHUD()
  {
    playerHitPoints.setStyle("-fx-text-fill: white; -fx-font: 30px Tahoma;");
    HBox statsBox = new HBox();
    StackPane.setAlignment(statsBox, Pos.CENTER);
    statsBox.getChildren().add(playerHitPoints);
    //this prevents the mouse pane from being the mouse focus
    statsBox.setMouseTransparent(true);
    threeDGameRoot.setPadding(new Insets(10,10,10,10));
    threeDGameRoot.getChildren().add(statsBox);
  }

  /**
   * Plays a sound when you click buttons
   * 
   */  
  public void playButtonSound()
  {
    //soundManager.playSoundClip(Sound.button);
  }
  
  public void updateWinScreen()
  {
    this.winRoot.setCenter(new Label("YOU WIN!"));
  }
  
  /**
   * @param //soundManager
   * 
   * Setter for the sound manager
   */  
  public void setSoundManager(/*SoundManager soundManager*/)
  {
    //this.soundManager = soundManager;
  }
  
  /**
   * Creates a new game board
   * 
   */  
  private void createNewGameBoard(int difficulty)
  {
    threeDGameObject = new ZombieHouse3d(difficulty, /*soundManager,*/ main, this);
  }


  /**
   * This code was cobbled together from other sections of entire program in a probably futile attempt to improve code logic
   * Sets up a new game
   * @author Sarah Salmonson
   */
  private void setUpNewGame(Stage gameStage)
  {
    try
    {
      main.assignStage(threeDGameObject.zombieHouse3d(Scenes.this));
      // Initialize stage
      gameStage.setTitle("Zombie House 3D");
      gameStage.setResizable(false);
      gameStage.setOnCloseRequest(event ->
      {
        if(threeDGameObject.getEntityManager() != null)
        {
          threeDGameObject.getEntityManager().player.gameIsRunning.set(false);
          threeDGameObject.getEntityManager().gameIsRunning.set(false);
        }
      });
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    //add the in-game HUD
    createGameHUD();
  }

  /**
   * Update hitpoints display
   * @param hitpoints
   * @author Sarah Salmonson
   */
  public void displayNewHP(int hitpoints)
  {
    playerHitPoints.setText("HP: " + hitpoints);
  }
}
