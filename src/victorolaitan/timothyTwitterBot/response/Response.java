package victorolaitan.timothyTwitterBot.response;

import victorolaitan.timothyTwitterBot.util.EasyJSON;

/**
 * Initial commit by Victor Olaitan on 16/03/2017.
 */
public abstract class Response {

    public abstract EasyJSON exportResponse();

    public abstract void run(Object... args);

}
