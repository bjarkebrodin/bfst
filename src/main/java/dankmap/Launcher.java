package dankmap;

import dankmap.controller.Controller;
import dankmap.drawing.DrawType;
import dankmap.model.DataModel;
import dankmap.osm.OSMParser;
import dankmap.view.ViewModel;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.Circle;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Launches the application
 */
public class Launcher extends Application {

    // THE PROGRAM WILL DEFAULT TO THIS FILE
    public static final InputStream DEFAULT_MAP = Launcher.class.getClassLoader().getResourceAsStream("data/small.osm");

    // DEFAULT WINDOW METRICS
    private static final double DEFAULT_WIDTH = 960;
    private static final double DEFAULT_HEIGHT = 640;
    private static double DEFAULT_WINDOW_X, DEFAULT_WINDOW_Y;

    // ACCEPTED FILE FORMATS
    private static final List<String> ACCEPTED_EXTENSIONS = new ArrayList<>();
    static {
        ACCEPTED_EXTENSIONS.add("*.osm");
        ACCEPTED_EXTENSIONS.add("*.zip");
    }

    // HOME FOLDER PATH NAMES
    public static final String BIN_PATH = System.getProperty("user.home");
    public static final String OSM_PATH = System.getProperty("user.home");

    // Hands control to the FX thread
    public static void main(String[] args) {
        launch(args);
    }

    // Everything starts here after FX thread initializes
    @Override
    public void start(Stage stage) {
        DEFAULT_WINDOW_X = screen().getMaxX() / 2.0 - DEFAULT_WIDTH / 2.0;
        DEFAULT_WINDOW_Y = screen().getMaxY() / 2.0 - DEFAULT_HEIGHT / 2.0;
        initialize(stage);
    }

    /**
     * Default stage initialization, this will open a map of the <code>DEFAULT_FILE</code>
     *
     * @param stage initialized content will be rendered using this stage
     */
    public static void initialize(Stage stage) {
        stage.setHeight(DEFAULT_HEIGHT);
        stage.setWidth(DEFAULT_WIDTH);
        stage.setResizable(true);
        stage.getIcons().add(new Image(Launcher.class.getClassLoader().getResourceAsStream("media/icons/globe.png")));
        openMapScene(new File("data/small.osm"), stage);
    }

    /**
     * Default stage initialization, this will open a map of the data in the specified file.
     *
     * @param stage initialized content will be rendered using this stage
     * @param file  *.osm | *.zip file containing map data
     */
    public static void initialize(Stage stage, File file) {
        stage.setHeight(DEFAULT_HEIGHT);
        stage.setWidth(DEFAULT_WIDTH);
        stage.setResizable(true);
        stage.getIcons().add(new Image(Launcher.class.getClassLoader().getResourceAsStream("media/icons/globe.png")));
        openMapScene(file, stage);
    }

    public static void loadOSM(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(OSM_PATH));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Open Street Maps", ACCEPTED_EXTENSIONS));
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) initialize(stage, file);
    }

    public static void loadBIN(Stage stage) {
        ArrayList<String> extensions = new ArrayList<>();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(BIN_PATH));
        extensions.add("*.bin");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Binaries", extensions));
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) initialize(stage, file);
    }

    private static DataModel loadFile(File file) throws IOException, XMLStreamException, InterruptedException, ClassNotFoundException {
        if (!file.getName().contains(".")) throw new IOException();
        String fileExt = file.getName().substring(file.getName().lastIndexOf('.'));
        switch (fileExt) {
            case ".bin":
                return readBinary(file);
            case ".osm":
            case ".zip":
                return readOSM(file);
            default:
                return null;
        }
    }

    private static DataModel readBinary(File file) throws IOException, ClassNotFoundException {
        return readBinary(new FileInputStream(file));
    }

    private static DataModel readBinary(InputStream file) throws IOException, ClassNotFoundException {
        var in = new ObjectInputStream(new BufferedInputStream(file));
        long startTime = System.nanoTime();
        DrawType.loadDrawTypeMap();
        DataModel dataModel = (DataModel) in.readObject();
        System.out.println(String.format("OSM load time: %.3f s", (System.nanoTime() - startTime) / 1e9));
        return dataModel;
    }

    private static DataModel readOSM(File file) throws IOException, XMLStreamException, InterruptedException {
        long startTime = System.nanoTime();
        DrawType.loadDrawTypeMap();
        OSMParser parser = new OSMParser(file);
        DataModel dataModel = parser.load();
        System.out.println(String.format("OSM load time: %.3f s", (System.nanoTime() - startTime) / 1e9));
        parser = null;
        return dataModel;
    }

    private static void openMapScene(File file, Stage stage) {
        stage.hide();
        Stage splash = makeSplash();

        Runnable parse = () -> {
            try {
                DataModel dataModel = loadFile(file);
                Platform.runLater(() -> showMap(dataModel, stage, splash));
            } catch (IOException | XMLStreamException | InterruptedException | ClassNotFoundException e) {
                showError("The specified file: " + file.getName() + ", is corrupted or invalid", stage);
                e.printStackTrace();
            }
        };

        Thread parser = new Thread(parse);
        showStage(splash);
        parser.start();
    }

    private static void openMapScene(InputStream file, Stage stage) {
        stage.hide();
        Stage splash = makeSplash();

        Runnable parse = () -> {
            try {
                DataModel dataModel = readBinary(file);
                Platform.runLater(() -> showMap(dataModel, stage, splash));
            } catch (IOException | ClassNotFoundException e) {
                showError("The specified file is corrupted or invalid", stage);
                e.printStackTrace();
            }
        };

        Thread parser = new Thread(parse);
        showStage(splash);
        parser.start();
    }

    private static void showMap(DataModel dataModel, Stage stage, Stage splash) {
        try {
            FXMLLoader loader = new FXMLLoader(Launcher.class.getClassLoader().getResource("fxml/view.fxml"));
            Font.loadFont(Objects.requireNonNull(Launcher.class.getClassLoader().getResource("media/fonts/francois_one/francois_one.ttf")).toExternalForm(), 26);
            Parent parent = null;
            parent = loader.<Parent>load();
            Scene mapScene = new Scene(parent, stage.getWidth(), stage.getHeight(), false, SceneAntialiasing.DISABLED);
            ViewModel viewModel = new ViewModel(dataModel, mapScene.getWidth(), mapScene.getHeight());
            Controller controller = loader.getController();
            controller.initialize(dataModel, viewModel);
            stage.setScene(mapScene);
            stage.sizeToScene();
            showStage(stage);
            splash.close();
        } catch (IOException e) {
            showError("Something unexpected has happened, we apologize the inconvenience", stage);
            e.printStackTrace();
        }
    }

    private static Stage makeSplash() {
        double w = DEFAULT_WIDTH;
        double h = DEFAULT_HEIGHT;
        double r = 100;
        double circleRadius = 3;
        double timingOffset = 60;

        Image image = new Image(Launcher.class.getClassLoader().getResourceAsStream("media/splash.png"));

        BackgroundImage bg = new BackgroundImage(image, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT);

        Stage stage = new Stage();
        BorderPane pane = new BorderPane();
        Scene scene = new Scene(pane, w, h);
        Pane container = new AnchorPane();

        ParallelTransition loader = new ParallelTransition();
        loader.setCycleCount(Animation.INDEFINITE);

        Path path = new Path();
        path.getElements().add(new MoveTo(w / 2.0 - r / 2.0, h / 2.0));
        path.getElements().add(new ArcTo(50, 50, 180, w / 2.0 + r / 2.0, h / 2.0, false, true));
        path.getElements().add(new ArcTo(50, 50, 180, w / 2.0 - r / 2.0, h / 2.0, false, true));

        for (int i = 0; i < 6; i++) {
            Circle circle = new Circle(circleRadius, Color.BLACK);
            container.getChildren().add(circle);
            PathTransition anim = new PathTransition();
            anim.setDuration(Duration.millis(1000));
            anim.setDelay(Duration.millis(i * timingOffset));
            anim.setInterpolator(Interpolator.EASE_BOTH);
            anim.setPath(path);
            anim.setNode(circle);
            loader.getChildren().add(anim);
        }

        stage.setOnShown((e) -> {
            loader.play();
        });

        pane.setCenter(container);
        stage.initStyle(StageStyle.UNDECORATED);
        scene.setFill(Color.valueOf("#fefefe"));
        pane.setBackground(new Background(bg));
        stage.setScene(scene);

        return stage;
    }

    private static void showStage(Stage stage) {
        double x = DEFAULT_WINDOW_X, y = DEFAULT_WINDOW_Y;

        if (stage.getStyle() != StageStyle.UNDECORATED) {
            // Titlebar compensation, hard to make exact
            x -= 12;
            y -= 7;
        }

        stage.setMinWidth(960);
        stage.setMinHeight(520);
        stage.setTitle("Dankmap");
        stage.setX(x);
        stage.setY(y);
        stage.show();
        stage.requestFocus();
        stage.toFront();
        stage.setAlwaysOnTop(true);
        stage.setAlwaysOnTop(false);
    }

    private static Rectangle2D screen() {
        return Screen.getPrimary().getBounds();
    }

    private static void showError(String msg, Stage stage) {
        Platform.runLater(() -> {
            Alert dialogue = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            dialogue.initStyle(StageStyle.DECORATED);
            dialogue.setOnCloseRequest((e) -> Platform.exit());
            dialogue.show();
        });
    }

}