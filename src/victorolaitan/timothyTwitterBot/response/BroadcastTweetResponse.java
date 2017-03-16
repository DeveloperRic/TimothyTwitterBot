package victorolaitan.timothyTwitterBot.response;

import victorolaitan.timothyTwitterBot.Main;
import victorolaitan.timothyTwitterBot.util.EasyJSON;

/**
 * Initial commit by Victor Olaitan on 16/03/2017.
 */
public class BroadcastTweetResponse extends Response {
    public String message;

    public BroadcastTweetResponse(String message) {
        this.message = message;
    }

    @Override
    public EasyJSON exportResponse() {
        EasyJSON json = EasyJSON.create();
        json.put("message", message, EasyJSON.JSONElementType.PRIMITIVE);
        return json;
    }

    @Override
    public void run(Object... args) {
        Main.twitter.setStatus(message);
    }
}
