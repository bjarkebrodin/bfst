package dankmap.controller;

import dankmap.Launcher;
import dankmap.drawing.DrawType;
import dankmap.model.Address;
import dankmap.model.DataModel;
import dankmap.model.Location;
import dankmap.model.PointOfInterest;
import dankmap.navigation.Vehicle;
import dankmap.util.StringUtil;
import dankmap.view.HighlightCanvas;
import dankmap.view.MapCanvas;
import dankmap.view.OverlayCanvas;
import dankmap.view.ViewModel;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controls the main view of the application :
 * - Holds handlers for UI events and bridges to file IO functionality
 */
public class Controller {

    public static boolean IS_DEBUG = false;

    // Data fields
    private DataModel dataModel;
    private ViewModel viewModel;

    // Mouse tracking variables
    private final long zoomTimeOut = 400;
    private long lastZoom = zoomTimeOut;
    private boolean isDragging;
    private Point2D lastMouse;

    // Temporarily holds route/marked addresses
    private boolean routeMenuIsShowing = false;
    private boolean profileMenuIsShowing = false;
    private Address routeFrom, routeTo;
    private Address currentAddress;

    // Misc utility components
    private final ContextMenu addressSuggestions = new ContextMenu();
    private final ObservableList<PointOfInterest> poiList = FXCollections.observableArrayList();

    // FXML component fields
    @FXML
    private StackPane stackPane;
    @FXML
    private MapCanvas mapCanvas;
    @FXML
    private HighlightCanvas highlightCanvas;
    @FXML
    private OverlayCanvas overlayCanvas;
    @FXML
    private ListView<PointOfInterest> poiListView;
    @FXML
    private TextField fromAddressField;
    @FXML
    private TextField toAddressField;
    @FXML
    private TextArea routeDescription;
    @FXML
    private BorderPane routeMenu;
    @FXML
    private ChoiceBox<String> routeTypeChoice;
    @FXML
    private ChoiceBox<String> styleChoice;
    @FXML
    public Button randButton;
    @FXML
    private ToggleButton POIButton;
    @FXML
    private TextField searchAddr;
    @FXML
    private ToggleGroup vehicleSelection;
    @FXML
    private ToggleButton carButton;
    @FXML
    private ToggleButton bikeButton;
    @FXML
    private ToggleButton walkButton;
    @FXML
    private ContextMenu contextMenu;
    @FXML
    private BorderPane addrBoxHolder;
    @FXML
    private BorderPane addrBox;
    @FXML
    private Label addrBoxAddress;
    @FXML
    private Label addrBoxLocation;
    @FXML
    private BorderPane profileMenu;

    public void initialize(DataModel dataModel, ViewModel viewModel) {
        getScene().addEventFilter(KeyEvent.ANY, this::handleArrowKeys);

        carButton.setUserData(Vehicle.MOTOR);
        bikeButton.setUserData(Vehicle.BIKE);
        walkButton.setUserData(Vehicle.PEDESTRIAN);


        initStyleChoice();

        this.dataModel = dataModel;
        this.viewModel = viewModel;
        stackPane.getScene().setOnKeyPressed(this::handleKeyPressed);
        mapCanvas.initialize(viewModel);
        highlightCanvas.initialize(viewModel);
        overlayCanvas.initialize(viewModel);
        bindViewModelSize();
        viewModel.addOnMapUpdateListener(this::onMapUpdate);
        viewModel.addOnHighlightUpdateListener(this::onHighlightUpdate);
        viewModel.addOnInputUpdateListener(this::onInputUpdate);
        dataModel.addOnDataUpdateListener(this::onDataModelUpdate);

        initPOIList();
    }

    private void setViewModelSize() {
        viewModel.setSize(stackPane.getWidth(), stackPane.getHeight());
    }

    private void bindViewModelSize() {
        setViewModelSize();
        stackPane.widthProperty().addListener((a, b, c) -> setViewModelSize());
        stackPane.heightProperty().addListener((a, b, c) -> setViewModelSize());
    }

    private void initPOIList() {
        poiList.addAll(dataModel.getPointsOfInterest());
        poiListView.setItems(poiList);
        poiListView.setCellFactory(param -> {
            var cell = new PointOfInterestController();
            cell.setDataModel(dataModel, viewModel);
            cell.setOnMouseClicked(event -> {
                currentAddress = cell.getItem();
                viewModel.setPrimaryReleasedPos(new Point2D(currentAddress.getX(), currentAddress.getY()));
                showAddressBox();
            });
            return cell;
        });
    }

    private void initStyleChoice() {
        for (DrawType.Style style : DrawType.Style.values()) {
            styleChoice.getItems().add(style.getName());
        }
    }

    ////////////// Canvas Events //////////////////
    public void scroll(ScrollEvent e) {
        long now = System.currentTimeMillis();
        if (now - lastZoom >= zoomTimeOut) {
            if (e.getDeltaY() == 0) return;
            lastZoom = now;
            var dy = e.getDeltaY();
            if (dy > 1) {
                viewModel.zoomIn(new Point2D(e.getX(), e.getY()));
            } else if (0 < dy && dy < 1 || dy < 0) {
                viewModel.zoomOut(new Point2D(e.getX(), e.getY()));
            }
        }
    }

    public void pan(MouseEvent e) {
        var dx = e.getX() - lastMouse.getX();
        var dy = e.getY() - lastMouse.getY();
        viewModel.pan(dx, dy);
        lastMouse = new Point2D(e.getX(), e.getY());
    }

    ////////////// MouseEvents ////////////////
    public void handleMousePressed(MouseEvent e) {
        mapCanvas.requestFocus();
        lastMouse = new Point2D(e.getX(), e.getY());
        viewModel.setCurrentClickedPos(e.getX(), e.getY());
    }

    public void handleMouseReleased(MouseEvent e) {
        if (!isDragging && e.getButton() == MouseButton.PRIMARY) {
            viewModel.setPrimaryReleasedPos(e.getX(), e.getY());
            currentAddress = dataModel.getNearestAddress(new Location(viewModel.getPrimaryReleasedPos().getX(), viewModel.getPrimaryReleasedPos().getY()));
            if (currentAddress == null) {
                addrBox.setVisible(false);
            } else {
                showAddressBox();
            }
        } else if (e.isShiftDown()) {
            viewModel.fitToSelectedBounds();
        } else if (e.getButton() == MouseButton.SECONDARY) {
            viewModel.setSecondaryReleasedPos(e.getX(), e.getY());
        }
        viewModel.clearCurrentDragPos();
        isDragging = false;
    }

    public void handleMouseDragged(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            isDragging = true;
            if (e.isShiftDown()) {
                viewModel.setCurrentDragPos(e.getX(), e.getY());
            } else {
                pan(e);
            }
        }
    }

    public void handleMouseMoved(MouseEvent e) {
        viewModel.setCurrentMovedPos(e.getX(), e.getY());
    }

    //////////// KeyEvents /////////////////
    public void handleArrowKeys(KeyEvent keyEvent) {
        if (!mapCanvas.isFocused()) return;

        double distance = 20;
        double dx = 0;
        double dy = 0;
        KeyCode code = keyEvent.getCode();
        if (code == KeyCode.UP || code == KeyCode.DOWN || code == KeyCode.LEFT || code == KeyCode.RIGHT) {
            switch (code) {
                case UP:
                    dy = distance;
                    break;
                case DOWN:
                    dy = -distance;
                    break;
                case LEFT:
                    dx = distance;
                    break;
                case RIGHT:
                    dx = -distance;
                    break;
            }
            keyEvent.consume();
            viewModel.pan(dx, dy);
        }

    }

    public void handleKeyPressed(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case D: {
                if (keyEvent.isControlDown()) toggleDebug();
                break;
            }
            case ESCAPE: {
                escape();
                break;
            }
            case R: {
                styleChoice.getSelectionModel().select(DrawType.Style.RANDOM.getName());
                loadDrawType(DrawType.Style.RANDOM);
                break;
            }
            case ADD: {
                zoomInButtonClicked(null);
                break;
            }
            case SUBTRACT: {
                zoomOutButtonClicked(null);
                break;
            }
        }
    }

    ////////////// UpdateEvents /////////////
    private void onMapUpdate() {
        double startTime = System.nanoTime();
        mapCanvas.update();
        overlayCanvas.setUpdateTime((System.nanoTime() - startTime));
        highlightCanvas.update();
        overlayCanvas.update();


        startTime = System.nanoTime();
        mapCanvas.repaint();
        overlayCanvas.setRepaintTime((System.nanoTime() - startTime) / 1e6);
        highlightCanvas.repaint();
        overlayCanvas.repaint();
    }

    private void onHighlightUpdate() {
        highlightCanvas.update();
        overlayCanvas.update();
        highlightCanvas.repaint();
        overlayCanvas.repaint();
    }

    private void onInputUpdate() {
        overlayCanvas.update();
        overlayCanvas.repaint();
    }


    //////////// Other Events ////////////////

    @FXML
    private void randomizeColor(ActionEvent actionEvent) {
        loadDrawType(DrawType.Style.RANDOM);
    }

    @FXML
    private void handleStyleChange(ActionEvent actionEvent) {
        // TODO: 4/30/20 rewrite to get source and delete field
        String selected = styleChoice.getSelectionModel().getSelectedItem();
        var style = DrawType.Style.getStyle(selected);
        if (style != null) {
            randButton.setVisible(style.equals(DrawType.Style.RANDOM));
            loadDrawType(style);
        }
    }

    private void loadDrawType(DrawType.Style style) {
        DrawType.loadDrawTypeMap(style);
        viewModel.updateElements();
        onMapUpdate();
    }

    @FXML
    private void toggleRouteMenu(MouseEvent event) {
        if (!routeMenuIsShowing) {
            openRouteMenu();
        } else {
            closeRouteMenu();
        }
    }

    private void openRouteMenu() {
        TranslateTransition openDrawer = new TranslateTransition(new Duration(350), routeMenu);
        openDrawer.setToX(0);
        openDrawer.play();
        routeMenuIsShowing = true;
    }

    @FXML
    private void closeRouteMenu() {
        TranslateTransition closeDrawer = new TranslateTransition(new Duration(350), routeMenu);
        closeDrawer.setToX(routeMenu.getWidth());
        closeDrawer.play();
        routeMenuIsShowing = false;
    }

    public void toggleProfileMenu(MouseEvent mouseEvent) {
        if (profileMenuIsShowing) {
            closeProfileMenu();
        } else {
            openProfileMenu();
        }
    }

    @FXML
    private void closeProfileMenu() {
        TranslateTransition openDrawer = new TranslateTransition(new Duration(350), profileMenu);
        openDrawer.setToX(-profileMenu.getWidth());
        openDrawer.play();
        profileMenuIsShowing = false;
    }

    public void openProfileMenu() {
        TranslateTransition openDrawer = new TranslateTransition(new Duration(350), profileMenu);
        openDrawer.setToX(0);
        openDrawer.play();
        profileMenuIsShowing = true;
    }

    public void zoomOutButtonClicked(ActionEvent actionEvent) {
        viewModel.zoomOut(new Point2D(stackPane.getWidth() / 2, stackPane.getHeight() / 2));
    }

    public void zoomInButtonClicked(ActionEvent actionEvent) {
        viewModel.zoomIn(new Point2D(stackPane.getWidth() / 2, stackPane.getHeight() / 2));
    }

    public void showContextMenu(ContextMenuEvent e) {
        contextMenu.show(getWindow(), e.getScreenX(), e.getScreenY());
    }

    //////// Measure /////////////

    public void setMeasureFrom(ActionEvent e) {
        viewModel.setMeasureFrom();
    }

    public void setMeasureTo(ActionEvent e) {
        viewModel.setMeasureTo();
    }

    public void clearMeasure(ActionEvent e) {
        viewModel.clearMeasure();
    }

    /////////////// Route //////////////
    public void findRoute(ActionEvent actionEvent) {
        if (routeFrom == null && getFromAddress().length() >= 3) {
            routeFrom = dataModel.getAddressMatch(getFromAddress());
            if (routeFrom != null) {
                fromAddressField.setText(routeFrom.getFormattedAddress());
            }
        }
        if (routeTo == null && getToAddress().length() >= 3) {
            routeTo = dataModel.getAddressMatch(getToAddress());
            if (routeTo != null) {
                toAddressField.setText(routeTo.getFormattedAddress());
            }
        }
        if (routeTo == null || routeFrom == null || routeFrom == routeTo) return;

        Vehicle vehicle = Vehicle.MOTOR;
        if (vehicleSelection.getSelectedToggle() != null) {
            vehicle = (Vehicle) vehicleSelection.getSelectedToggle().getUserData();
        }
        viewModel.setCurrentRoute(dataModel.getRoute(vehicle, routeFrom, routeTo, routeTypeChoice.getValue().equals("Fastest route")));

        routeDescription.setText(viewModel.getCurrentRoute().getDescription() + routeTo.getFormattedAddress());
    }

    public void setFromRoute(ActionEvent actionEvent) {
        routeFrom = dataModel.getAddress(getFromAddress());
    }

    public void setToRoute(ActionEvent actionEvent) {
        routeTo = dataModel.getAddress(getToAddress());
    }

    public void setFromRouteToMousePos() {
        Point2D pos = viewModel.getSecondaryReleasedPos();
        if (pos != null) {
            Location loc = new Location(pos.getX(), pos.getY());
            routeFrom = dataModel.getNearestAddress(loc);
            if (routeFrom != null) {
                fromAddressField.setText(routeFrom.getFormattedAddress());
                openRouteMenu();
                findRoute(null);
            }
        }
    }

    public void setToRouteToMousePos() {
        Point2D pos = viewModel.getSecondaryReleasedPos();
        if (pos != null) {
            Location loc = new Location(pos.getX(), pos.getY());
            routeTo = dataModel.getNearestAddress(loc);
            if (routeTo != null) {
                toAddressField.setText(routeTo.getFormattedAddress());
                openRouteMenu();
                findRoute(null);
            }
        }
    }

    public void setFromRouteToCurrentAddress(ActionEvent actionEvent) {
        if (currentAddress != null) {
            routeFrom = currentAddress;
            fromAddressField.setText(routeFrom.getFormattedAddress());
            openRouteMenu();
        }
    }

    public void setToRouteToCurrentAddress(ActionEvent actionEvent) {
        if (currentAddress != null) {
            routeTo = currentAddress;
            toAddressField.setText(routeTo.getFormattedAddress());
            openRouteMenu();
        }
    }

    public void clearRoute(ActionEvent actionEvent) {
        fromAddressField.setText("");
        toAddressField.setText("");
        routeFrom = null;
        routeTo = null;
        routeDescription.clear();
        viewModel.setCurrentRoute(null);
        onHighlightUpdate();
    }


    ///////// Address /////////////

    public void searchForAddress(ActionEvent actionEvent) {
        searchAddr.requestFocus();
        if (getSearchAddress().length() >= 3)
            currentAddress = dataModel.getAddressMatch(getSearchAddress());
        if (currentAddress != null) {
            searchAddr.setText(currentAddress.getFormattedAddress());
            viewModel.jumpToPoint(currentAddress);
            showAddressBox();
            mapCanvas.requestFocus();
        }
    }

    private void showAddressBox() {
        if (currentAddress != null) {
            boolean isPoi = false;
            for (PointOfInterest POI : dataModel.getPointsOfInterest()) {
                if (POI.getAddress().equals(currentAddress.getAddress())) {
                    isPoi = true;
                    break;
                }
            }
            if (isPoi) {
                POIButton.setSelected(true);
            } else {
                POIButton.setSelected(false);
            }

            addrBoxAddress.setText(currentAddress.getFormattedAddress());

            addrBoxLocation.setText(currentAddress.toGeo().toDMS());
            addrBox.setVisible(true);
            addrBoxHolder.setMouseTransparent(false);
        }
    }

    @FXML
    private void onTypedAddressSearch(KeyEvent e) {
        TextField input = (TextField) e.getSource();
        if (input.getText().length() < 3) return;
        matchAddress(input);
    }

    private void matchAddress(TextField input) {
        Collection<String> matches = dataModel.getAddressMatches(input.getText().strip().toLowerCase());
        addressSuggestions.getItems().clear();
        if (matches.isEmpty()) return;

        // Prevent suggesting a full match
        if (matches.size() == 1) {
            for (String s : matches) {
                if (input.getText().toLowerCase().equals(s)) {
                    return;
                }
            }
        }

        // Shuffling matches, pick 10 and sort to get more variance in results.
        var list = matches.stream().collect(Collectors.collectingAndThen(Collectors.toList(), collected -> {
            Collections.shuffle(collected);
            return collected.stream();
        })).limit(10).sorted();

        list.forEach(match -> {
            Label menuLabel = new Label(StringUtil.capitalizeAllFirstLetters(match));
            menuLabel.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
                viewModel.jumpToPoint(dataModel.getAddress(menuLabel.getText()));
            });

            MenuItem suggestion = new MenuItem();
            suggestion.setGraphic(menuLabel);
            suggestion.setOnAction(event -> {
                input.setText(menuLabel.getText());
                input.positionCaret(menuLabel.getText().length() + 1);
                addressSuggestions.hide();
                addressSuggestions.getItems().clear();
                input.getOnAction().handle(new ActionEvent());
            });

            addressSuggestions.getItems().add(suggestion);
        });

        addressSuggestions.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.SPACE) {
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER && addressSuggestions.getItems().size() == 1) {
                addressSuggestions.getItems().get(0).fire();
            }

        });

        addressSuggestions.show(input, Side.BOTTOM, 0, 0);
    }

    public void closeAddrBox(ActionEvent actionEvent) {
        addrBox.setVisible(false);
        viewModel.clearPrimaryReleasePos();
        addrBoxHolder.setMouseTransparent(true);
    }

    private String getFromAddress() {
        return fromAddressField.getText();
    }

    private String getToAddress() {
        return toAddressField.getText();
    }

    private String getSearchAddress() {
        return searchAddr.getText();
    }

    ////////// Point of Interest ////////////
    public void handlePointOfInterest(ActionEvent e) {
        if (currentAddress != null) {
            if (!POIButton.isSelected()) {
                dataModel.removePointOfInterest(currentAddress);
            } else {
                PointOfInterest toAdd = new PointOfInterest(currentAddress, new Date());
                dataModel.addPointOfInterest(toAdd);
                viewModel.animToPoint(new Location(toAdd.getX(), toAdd.getY()));
                closeAddrBox(null);
                openProfileMenu();
            }
        }
    }


    public void clearPointsOfInterests(ActionEvent actionEvent) {
        dataModel.clearPointsOfInterest();
    }

    private void onDataModelUpdate() {
        poiList.clear();
        poiList.addAll(dataModel.getPointsOfInterest());
        if (POIButton.isSelected()) {
            boolean found = false;
            for (PointOfInterest POI : dataModel.getPointsOfInterest()) {
                if (POI.getAddress().equals(currentAddress.getAddress())) {
                    found = true;
                }
            }
            if (!found) POIButton.setSelected(false);
        }

    }

    /////////// Misc ////////////

    private void toggleDebug() {
        IS_DEBUG = !IS_DEBUG;
        overlayCanvas.repaint();
        highlightCanvas.repaint();
    }

    private Scene getScene() {
        return stackPane.getParent().getScene();
    }

    private Window getWindow() {
        return getScene().getWindow();
    }

    private void escape() {
        if (profileMenuIsShowing) {
            closeProfileMenu();
        } else if (routeMenuIsShowing) {
            closeRouteMenu();
        } else {
            if (getConfirmationResult()) Platform.exit();
        }
    }
    ///////////// Preloader ////////////

    @FXML
    private void reload() {
        Stage stage = (Stage) mapCanvas.getScene().getWindow();
        Launcher.initialize(stage);
    }

    @FXML
    private void loadOSM() {
        Launcher.loadOSM((Stage) getWindow());
    }

    @FXML
    private void loadBIN(ActionEvent actionEvent) {
        Launcher.loadBIN((Stage) getWindow());
    }

    private boolean getConfirmationResult() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText("Are you sure you want to close this map?");
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("media/icons/globe.png")));
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            return true;
        }
        return false;
    }

    @FXML
    private void saveToBinary() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(Launcher.BIN_PATH));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Binaries", "*.bin"));
        File out = (fileChooser).showSaveDialog(getScene().getWindow());
        if (out == null) return;
        try (var writer = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(out.toString())))) {
            writer.writeObject(dataModel);
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Alert(Alert.AlertType.CONFIRMATION, "saved to " + out.getName()).show();
    }
}