package victorolaitan.timothyTwitterBot.response;

import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.trigger.Trigger;
import victorolaitan.timothyTwitterBot.util.EasyJSON;

import java.math.BigInteger;

/**
 * Initial commit by Victor Olaitan on 26/03/2017.
 */
public class QuoteTweetResponse implements Response {
    Trigger trigger;
    String message;

    public QuoteTweetResponse(Trigger trigger) {
        this.trigger = trigger;
    }

    @Override
    public Trigger getTrigger() {
        return trigger;
    }

    @Override
    public ResponseDataType requiredDataType() {
        return ResponseDataType.STATUS_ID;
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
        Main.twitter.retweetWithComment(Main.twitter.getStatus((BigInteger) data), message);
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
