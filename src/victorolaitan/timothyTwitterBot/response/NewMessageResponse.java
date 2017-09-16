package victorolaitan.timothyTwitterBot.response;

import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.trigger.Trigger;
import victorolaitan.timothyTwitterBot.util.EasyJSON;

/**
 * Initial commit by Victor Olaitan on 26/03/2017.
 */
public class NewMessageResponse implements Response {
    Trigger trigger;
    String message;

    public NewMessageResponse(Trigger trigger) {
        this.trigger = trigger;
    }

    @Override
    public Trigger getTrigger() {
        return trigger;
    }

    @Override
    public ResponseDataType requiredDataType() {
        return ResponseDataType.USER_ID;
    }

    @Override
    public void create(EasyJSON.JSONElement data) {
        message = data.valueOf("message");
    }

    @Override
    public EasyJSON exportResponse() {
        EasyJSON json = EasyJSON.create();
        json.putGeneric("class", this.getClass().getName());
        json.putGeneric("message", message);
        return json;
    }

    @Override
    public void run(Object data) {
        Main.twitter.sendMessage(Main.twitter.users().getUser((long) data).getScreenName(), message);
    }

    @Override
    public Object getSavedData() {
        return message;
    }

    @Override
    public void updateSavedData(Object newData) {
        message = (String) newData;
    }
}
