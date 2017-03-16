package victorolaitan.timothyTwitterBot.controller;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.util.Util;

import javax.imageio.ImageIO;
import java.io.IOException;


public class DashboardController {
    @FXML
    private Label Dash_lblUsername;
    @FXML
    private ImageView Dash_picUserImage;

    public void init() {
        Dash_lblUsername.setText("Logged in as: " + Main.username);
        try {
            Dash_picUserImage.setImage(SwingFXUtils.toFXImage(ImageIO.read(Main.twitter.getSelf().getProfileImageUrl().toURL()), null));
        } catch (IOException e) {
            e.printStackTrace();
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
