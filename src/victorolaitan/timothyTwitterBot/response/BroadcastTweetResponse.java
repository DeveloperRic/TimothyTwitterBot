package victorolaitan.timothyTwitterBot.response;

import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.util.EasyJSON;

/**
 * Initial commit by Victor Olaitan on 16/03/2017.
 */
public class BroadcastTweetResponse implements Response {
    public String message;

    @Override
    public ResponseDataType requiredDataType() {
        return null;
    }

    @Override
    public void create(EasyJSON.JSONElement data) {
        message = data.valueOf("message");
    }

    @Override
    public EasyJSON exportResponse() {
        EasyJSON json = EasyJSON.create();
        json.putGeneric("class", BroadcastTweetResponse.class.getName());
        json.putGeneric("message", message);
        return json;
    }

    @Override
    public void run(Object data) {
        Main.twitter.updateStatus(message);
    }
}
