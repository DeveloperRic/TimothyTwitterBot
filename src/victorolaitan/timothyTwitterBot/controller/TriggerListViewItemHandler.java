package victorolaitan.timothyTwitterBot.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import victorolaitan.timothyTwitterBot.response.Response;
import victorolaitan.timothyTwitterBot.trigger.Trigger;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Initial commit by Victor Olaitan on 19/03/2017.
 */
public class TriggerListViewItemHandler {
    @FXML
    private Label triggerClass;
    @FXML
    private ListView triggerResponses;
    @FXML
    private GridPane gridPane;

    private ObservableList observableList = FXCollections.observableArrayList();

    public TriggerListViewItemHandler() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("victorolaitan/timothyTwitterBot/res/scene/trigger-listview-item.fxml"));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Trigger trigger;

    public void setInfo(Trigger trigger) {
        this.trigger = trigger;
        triggerClass.setText(trigger.getClass().getSimpleName());
        observableList.setAll(trigger.responses.stream().map(response -> response.getClass().getSimpleName()).collect(Collectors.toSet()));
        triggerResponses.setItems(observableList);
    }

    public GridPane getGridPane() {
        return gridPane;
    }

    @FXML
    public void responseListClick() {
        Object selectedResponse = triggerResponses.getSelectionModel().getSelectedItem();
        if (selectedResponse != null) {
            AutoResponseController.selectedResponse = (Response) observableList.get(triggerResponses.getSelectionModel().getSelectedIndex());
        }
    }
}
