package victorolaitan.timothyTwitterBot.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.util.Util;

public class AutoResponseController {
    @FXML
    ChoiceBox comboAddWhen;
    @FXML
    ChoiceBox comboAddSend;

    public void init() {
        comboAddWhen.getItems().clear();
        comboAddWhen.getItems().addAll(Util.bufferClassTextFile("AR_triggers"));
        comboAddSend.getItems().addAll(Util.bufferClassTextFile("AR_responses"));
    }

    @FXML
    public void btnReturnClick() {
        Util.switchScene(Main.currentStage, "dashboard");
    }

    @FXML
    public void btnAddAutoResponse() {
        if (comboAddWhen.getSelectionModel().getSelectedItem() == null || comboAddSend.getSelectionModel().getSelectedItem() == null) {
            new Alert(Alert.AlertType.ERROR, "You have not selected both a trigger and a response!").showAndWait();
            return;
        }

        switch (comboAddWhen.getSelectionModel().getSelectedItem().toString()) {
            case "Someone follows me":

                break;
            case "Someone un-follows me":
                break;
            case "Someone tweets me":
                break;
            case "Someone sends me a direct message":
                break;
            case "Someone likes my tweet":
                break;
            case "Someone re-tweets my tweet":
                break;
            case "Someone quotes my tweet":
                break;
            case "Someone mentions me":
                break;
            default:
                new Alert(Alert.AlertType.INFORMATION, "The selected trigger is not yet available!").showAndWait();
        }
    }
}
