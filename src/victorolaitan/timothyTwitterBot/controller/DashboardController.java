package victorolaitan.timothyTwitterBot.controller;

import javafx.fxml.FXML;
import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.util.Util;


public class DashboardController {

    @FXML
    public void onDashManageResponsesClick() {
        AutoResponseController arController = Util.switchScene(Main.currentStage, "auto-responses");
        arController.init();
    }

    @FXML
    public void onScheduledTweetsClick() {

    }
}
