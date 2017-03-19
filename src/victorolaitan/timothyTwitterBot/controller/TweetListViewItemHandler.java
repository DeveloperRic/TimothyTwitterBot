package victorolaitan.timothyTwitterBot.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import victorolaitan.timothyTwitterBot.util.Util;
import winterwell.jtwitter.Status;
import winterwell.jtwitter.User;

import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Initial commit by Victor Olaitan on 19/03/2017.
 */
public class TweetListViewItemHandler {
    private static final SimpleDateFormat tweetDateFormat = new SimpleDateFormat("k:m - d EEE MMM yyyy");
    @FXML
    private Label creatorName;
    @FXML
    private Label creatorUsername;
    @FXML
    private Label tweetText;
    @FXML
    private Label tweetDate;
    @FXML
    private ImageView creatorImage;
    @FXML
    private GridPane gridPane;

    public TweetListViewItemHandler() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("victorolaitan/timothyTwitterBot/res/scene/tweet-listview-item.fxml"));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setInfo(Status status) {
        User creator = status.getUser();
        String name = creator.getName();
        if (name == null) {
            creatorName.setText(name);
        } else {
            creatorName.setText("");
        }
        creatorUsername.setText("@" + creator.screenName);
        tweetText.setText(status.getText());
        tweetDate.setText(tweetDateFormat.format(status.getCreatedAt()));
        try {
            creatorImage.setImage(Util.downloadImage(creator.getProfileImageUrl().toURL()));
        } catch (IOException e) {
            creatorImage.setImage(Util.loadClassImage("twitter-default-avatar.jpg"));
        }
    }

    public GridPane getGridPane() {
        return gridPane;
    }
}
