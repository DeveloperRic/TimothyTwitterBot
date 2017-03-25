package victorolaitan.timothyTwitterBot.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.response.*;
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
        lblEditTrigger.setText("");
        lblEditResponse.setText("");
    }

    public class ListViewCell extends ListCell<Trigger> {
        @Override
        public void updateItem(Trigger trigger, boolean empty) {
            super.updateItem(trigger, empty);
            if (trigger != null) {
                TriggerListViewItemHandler triggerListViewItemHandler = new TriggerListViewItemHandler();
                triggerListViewItemHandler.setInfo(trigger);
                triggerListViewItemHandler.getGridPane().setPrefWidth(autoResponsesList.getPrefWidth());
                GridPane gridPane = triggerListViewItemHandler.getGridPane();
                gridPane.setPrefWidth(autoResponsesList.getPrefWidth());
                setGraphic(gridPane);
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
        Response response;
        switch (comboAddSend.getSelectionModel().getSelectedItem().toString()) {
            case "New tweet":
                BroadcastTweetResponse broadcastTweetResponse = new BroadcastTweetResponse(trigger);
                broadcastTweetResponse.message = txtAddArgs.getText();
                response = broadcastTweetResponse;
                break;
            case "Follow back":
                response = new FollowResponse(trigger);
                break;
            case "Un-follow the person":
                response = new UnFollowResponse(trigger);
                break;
            case "Reply tweet":
                ReplyTweetResponse replyTweetResponse = new ReplyTweetResponse(trigger);
                replyTweetResponse.message = txtAddArgs.getText();
                response = replyTweetResponse;
                break;
            case "Reply direct message":
                ReplyMessageResponse replyMessageResponse = new ReplyMessageResponse(trigger);
                replyMessageResponse.message = txtAddArgs.getText();
                response = replyMessageResponse;
                break;
            default:
                new Alert(Alert.AlertType.WARNING, "The selected response has not been implemented yet!").showAndWait();
                return;
        }
        if (!Util.checkDataTypesCompatible(trigger.suppliedDataType(), response.requiredDataType())) {
            new Alert(Alert.AlertType.WARNING, "The selected trigger and response are not compatible!").showAndWait();
            return;
        }
        if (trigger.duplicateResponse(response)) {
            new Alert(Alert.AlertType.WARNING, "Automated response has already been added!").showAndWait();
            return;
        }
        trigger.addResponses(response);
        trigger.activate();
        refreshList();
        new Alert(Alert.AlertType.INFORMATION, trigger.getClass().getSimpleName() + "\n" + trigger.responses.get(0).getClass().getSimpleName() + "\n" + txtAddArgs.getText()).showAndWait();
    }

    @FXML
    public void autoResponsesListClick() {
        selectedResponse = null;
        lblEditTrigger.setText("");
        lblEditResponse.setText("");
        txtEditArgs.setText("");
    }

    static Response selectedResponse;

    @FXML
    public void btnArRemoveClick() {
        Object selectedItem = autoResponsesList.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Trigger selectedTrigger = (Trigger) observableList.get(autoResponsesList.getSelectionModel().getSelectedIndex());
            Trigger.triggers.remove(selectedTrigger);
            autoResponsesList.getSelectionModel().clearSelection();
            refreshList();
        } else {
            if (selectedResponse != null) {
                selectedResponse.getTrigger().responses.remove(selectedResponse);
                refreshList();
            }
        }
    }

    @FXML
    Label lblEditTrigger;
    @FXML
    Label lblEditResponse;
    @FXML
    TextField txtEditArgs;

    @FXML
    public void btnArEditClick() {
        if (selectedResponse != null) {
            Object savedData = selectedResponse.getSavedData();
            if (savedData == null) {
                new Alert(Alert.AlertType.INFORMATION, "The response has no saved data that can be modified!").showAndWait();
            } else {
                lblEditTrigger.setText(selectedResponse.getTrigger().getClass().getSimpleName());
                lblEditResponse.setText(selectedResponse.getClass().getSimpleName());
                txtEditArgs.setText(String.valueOf(savedData));
                editingResponse = selectedResponse;
            }
        }
    }

    private void refreshList() {
        observableList.clear();
        observableList.setAll(Trigger.triggers);
    }

    Response editingResponse;

    @FXML
    public void onEditResponseClick() {
        if (editingResponse != null) {
            if (!editingResponse.getTrigger().responses.contains(editingResponse)) {
                new Alert(Alert.AlertType.ERROR, "The response has since been removed from its trigger!").showAndWait();
            } else {
                editingResponse.updateSavedData(txtEditArgs.getText());
                lblEditTrigger.setText("");
                lblEditResponse.setText("");
                txtEditArgs.setText("");
                editingResponse = null;
            }
        }
    }
}
