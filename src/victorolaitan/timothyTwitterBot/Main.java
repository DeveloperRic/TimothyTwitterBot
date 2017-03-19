package victorolaitan.timothyTwitterBot;

import javafx.application.Application;
import javafx.stage.Stage;
import victorolaitan.timothyTwitterBot.controller.DashboardController;
import victorolaitan.timothyTwitterBot.trigger.Trigger;
import victorolaitan.timothyTwitterBot.util.Util;
import winterwell.jtwitter.OAuthSignpostClient;
import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterException;

import java.util.ArrayList;

public class Main extends Application {
    public static Main instance;
    public static Stage currentStage;
    private static final String consumerKey = "LAGO9eiNrLAJj4CCu60jrY5s6";
    private static final String consumerSecret = "6uH1L0enBWaGqC5gZtEZBZ2Im4ZPBvw9bYnCRrZNx2fbjmQTkU";
    public static Twitter twitter;
    public static String username;

    @Override
    public void start(Stage primaryStage) throws Exception {
        instance = this;
        currentStage = primaryStage;
        ArrayList<String> acs = Util.bufferTextFile("acs");
        if (acs.isEmpty()) {
            Util.switchScene(primaryStage, "timothy");
        } else {
            initTimothy(acs, primaryStage);
        }
    }

    public static void initTimothy(ArrayList<String> acs, Stage stage) {
        try {
            OAuthSignpostClient client = new OAuthSignpostClient(consumerKey, consumerSecret,
                    acs.get(0), acs.get(1));
            username = acs.get(2);
            twitter = new Twitter(username, client);
        } catch (Exception e) {
            e.printStackTrace();
            Util.switchScene(stage, "no-internet");
            currentStage.show();
            return;
        }
        DashboardController controller = Util.switchScene(stage, "dashboard");
        currentStage.show();
        controller.init();
        Trigger.init();
    }

    @Override
    public void stop() {
        Trigger.saveTriggers();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static OAuthSignpostClient oauthClient;

    public static void requestTokens() {
        oauthClient = new OAuthSignpostClient(consumerKey, consumerSecret, "oob");
        try {
            instance.getHostServices().showDocument(oauthClient.authorizeUrl().toString());
        } catch (Exception e) {
            e.printStackTrace();
            //failure (no key)
            return;
        }
        Util.switchScene(currentStage, "grant-access");
    }

    public static void verifyAccount(String code) {
        try {
            oauthClient.setAuthorizationCode(code);
        } catch (TwitterException e) {
            e.printStackTrace();
            //fail
            return;
        }
        String[] codes = oauthClient.getAccessToken();
        String[] toSave = new String[3];
        toSave[0] = codes[0];
        toSave[1] = codes[1];
        toSave[2] = username;
        Util.writeToTextFile("acs", false, toSave);
        twitter = new Twitter(username, oauthClient);
        DashboardController controller = Util.switchScene(Main.currentStage, "dashboard");
        controller.init();
    }
}
