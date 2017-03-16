package victorolaitan.timothyTwitterBot.controller;

import javafx.fxml.FXML;
import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.util.Util;

/**
 * Initial commit by Victor Olaitan on 15/03/2017.
 */
public class NoInternetController {

    @FXML
    public void onRetryClick() {
        Main.initTimothy(Util.bufferTextFile("acs"), Main.currentStage);
    }
}
