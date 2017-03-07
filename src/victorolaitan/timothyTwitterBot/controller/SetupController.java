package victorolaitan.timothyTwitterBot.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import victorolaitan.timothyTwitterBot.Main;


public class SetupController {
    @FXML
    TextField Setup_twitterUsername;
    @FXML
    TextField Setup_twitterCode;

    @FXML
    public void onSetupBeginClick() {
        Main.username = Setup_twitterUsername.getText();
        Main.requestTokens();
    }

    @FXML
    public void onSetupVerifyClick() {
        Main.verifyAccount(Setup_twitterCode.getText());
    }
}
