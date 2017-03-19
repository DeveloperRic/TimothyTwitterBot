package victorolaitan.timothyTwitterBot.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;
import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.response.BroadcastTweetResponse;
import victorolaitan.timothyTwitterBot.response.FollowResponse;
import victorolaitan.timothyTwitterBot.response.ReplyTweetResponse;
import victorolaitan.timothyTwitterBot.response.UnFollowResponse;
import victorolaitan.timothyTwitterBot.trigger.*;
import victorolaitan.timothyTwitterBot.util.Util;

public class AutoResponseController {
    @FXML
    ChoiceBox comboAddWhen;
    @FXML
    ChoiceBox comboAddSend;
    @FXML
    TextField txtAddArgs;
    @FXML
    ListView autoResponsesList;

    ObservableList observableList = FXCollections.observableArrayList();

    public void init() {
        comboAddWhen.getItems().clear();
        comboAddWhen.getItems().addAll(Util.bufferClassTextFile("AR_triggers"));
        comboAddSend.getItems().addAll(Util.bufferClassTextFile("AR_responses"));
        observableList.setAll(Trigger.triggers);
        autoResponsesList.setItems(observableList);
        autoResponsesList.setCellFactory(new Callback<ListView<Trigger>, ListCell<Trigger>>() {
            @Override
            public ListCell<Trigger> call(ListView<Trigger> listView) {
                return new ListViewCell();
            }
        });
    }

    public class ListViewCell extends ListCell<Trigger> {
        @Override
        public void updateItem(Trigger trigger, boolean empty) {
            super.updateItem(trigger, empty);
            if (trigger != null) {
                TriggerListViewItemHandler triggerListViewItemHandler = new TriggerListViewItemHandler();
                triggerListViewItemHandler.setInfo(trigger);
                triggerListViewItemHandler.getGridPane().setPrefWidth(autoResponsesList.getPrefWidth());
                setGraphic(triggerListViewItemHandler.getGridPane());
            }
        }
    }

    @FXML
    public void btnReturnClick() {
        DashboardController controller = Util.switchScene(Main.currentStage, "dashboard");
        controller.init();
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
                trigger = Trigger.locateTrigger(UnFollowTrigger.class);
                if (trigger == null) {
                    trigger = new UnFollowTrigger();
                }
                break;
            case "Someone mentions me":
                trigger = Trigger.locateTrigger(MentionTrigger.class);
                if (trigger == null) {
                    trigger = new MentionTrigger();
                }
                break;
            case "Someone sends me a direct message":
                trigger = Trigger.locateTrigger(MessageTrigger.class);
                if (trigger == null) {
                    trigger = new MessageTrigger();
                }
                break;
            case "Someone likes my tweet":
                break;
            case "Someone re-tweets my tweet":
                break;
            case "Someone quotes my tweet":
                break;
            default:
                new Alert(Alert.AlertType.WARNING, "The selected trigger is not yet available!").showAndWait();
                return;
        }
        switch (comboAddSend.getSelectionModel().getSelectedItem().toString()) {
            case "New tweet":
                BroadcastTweetResponse broadcastTweetResponse = new BroadcastTweetResponse();
                broadcastTweetResponse.message = txtAddArgs.getText();
                trigger.addResponses(broadcastTweetResponse);
                break;
            case "Follow back":
                trigger.addResponses(new FollowResponse());
                break;
            case "Un-follow the person":
                trigger.addResponses(new UnFollowResponse());
                break;
            case "Reply tweet":
                ReplyTweetResponse replyTweetResponse = new ReplyTweetResponse();
                replyTweetResponse.message = txtAddArgs.getText();
                trigger.addResponses(replyTweetResponse);
                break;
            default:
                new Alert(Alert.AlertType.WARNING, "The selected response has not been implemented yet!").showAndWait();
                return;
        }
        trigger.activate();
        new Alert(Alert.AlertType.INFORMATION, trigger.getClass().getSimpleName() + "\n" + trigger.responses.get(0).getClass().getSimpleName() + "\n" + txtAddArgs.getText()).showAndWait();
    }
}
