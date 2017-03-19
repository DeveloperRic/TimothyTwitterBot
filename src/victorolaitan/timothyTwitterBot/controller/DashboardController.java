package victorolaitan.timothyTwitterBot.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.util.Util;
import winterwell.jtwitter.Status;

import java.io.IOException;


public class DashboardController {
    @FXML
    private Label Dash_lblUsername;
    @FXML
    private ImageView Dash_picUserImage;
    @FXML
    private ListView Dash_lstTimeline;

    public void init() {
        Dash_lblUsername.setText("Logged in as: " + Main.username);
        try {
            Dash_picUserImage.setImage(Util.downloadImage(Main.twitter.getSelf().getProfileImageUrl().toURL()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        ObservableList observableList = FXCollections.observableArrayList();
        observableList.addAll(Main.twitter.getHomeTimeline());
        Dash_lstTimeline.setItems(observableList);
        Dash_lstTimeline.setCellFactory(new Callback<ListView<Status>, ListCell<Status>>() {
            @Override
            public ListCell<Status> call(ListView<Status> listView) {
                return new ListViewCell();
            }
        });
    }

    public class ListViewCell extends ListCell<Status> {
        @Override
        public void updateItem(Status status, boolean empty) {
            super.updateItem(status, empty);
            if (status != null) {
                TweetListViewItemHandler tweetListViewItemHandler = new TweetListViewItemHandler();
                tweetListViewItemHandler.setInfo(status);
                tweetListViewItemHandler.getGridPane().setPrefWidth(Dash_lstTimeline.getPrefWidth());
                setGraphic(tweetListViewItemHandler.getGridPane());
            }
        }
    }

    @FXML
    public void onDashManageResponsesClick() {
        AutoResponseController arController = Util.switchScene(Main.currentStage, "auto-responses");
        arController.init();
    }

    @FXML
    public void onScheduledTweetsClick() {
    }
}
