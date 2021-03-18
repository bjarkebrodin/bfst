package dankmap.controller;

import dankmap.model.DataModel;
import dankmap.model.PointOfInterest;
import dankmap.view.ViewModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;

import java.io.IOException;

public class PointOfInterestController extends ListCell<PointOfInterest> {
    private DataModel dataModel;
    private ViewModel viewModel;

    @FXML
    private Label addressLabel;
    @FXML
    private Label locationLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Button deleteButton;
    @FXML
    private Button goToButton;

    public PointOfInterestController() {
        loadFXML();
        deleteButton.setOnAction(event -> deleteItem());
        goToButton.setOnAction(event -> goTo());
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/poi_list_cell.fxml"));
            loader.setRoot(this);
            loader.setController(this);
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setDataModel(DataModel dataModel, ViewModel viewModel) {
        this.dataModel = dataModel;
        this.viewModel = viewModel;
    }

    private void deleteItem() {
        if (dataModel == null) return;
        dataModel.removePointOfInterest(getItem());
    }

    private void goTo() {
        viewModel.animToPoint(getItem());
    }

    @Override
    protected void updateItem(PointOfInterest item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        } else {
            addressLabel.setText(item.getFormattedAddress());
            locationLabel.setText(item.toGeo().toDMS());
            dateLabel.setText(item.getFormattedTimeStamp());
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }
    }
}
