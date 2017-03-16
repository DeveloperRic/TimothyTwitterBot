package victorolaitan.timothyTwitterBot.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.response.BroadcastTweetResponse;
import victorolaitan.timothyTwitterBot.response.Response;
import victorolaitan.timothyTwitterBot.trigger.FollowTrigger;
import victorolaitan.timothyTwitterBot.trigger.Trigger;
import victorolaitan.timothyTwitterBot.util.Util;

public class AutoResponseController {
    @FXML
    ChoiceBox comboAddWhen;
    @FXML
    ChoiceBox comboAddSend;
    @FXML
    TextField txtAddArgs;

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
        Trigger trigger = null;
        switch (comboAddWhen.getSelectionModel().getSelectedItem().toString()) {
            case "Someone follows me":
                trigger = Trigger.locateTrigger(FollowTrigger.class);
                if (trigger == null) {
                    trigger = new FollowTrigger();
                }
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
                new Alert(Alert.AlertType.WARNING, "The selected trigger is not yet available!").showAndWait();
                return;
        }
        Response response = null;
        switch (comboAddSend.getSelectionModel().getSelectedItem().toString()) {
            case "Send tweet":
                response = new BroadcastTweetResponse(txtAddArgs.getText());
                break;
            default:
                new Alert(Alert.AlertType.WARNING, "The selected response has not been implemented yet!").showAndWait();
                return;
        }
        trigger.addResponses(response);
        new Alert(Alert.AlertType.INFORMATION, txtAddArgs.getText()).showAndWait();
    }
}
