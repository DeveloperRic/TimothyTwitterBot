package victorolaitan.timothyTwitterBot.response;

import victorolaitan.timothyTwitterBot.util.EasyJSON;

/**
 * Initial commit by Victor Olaitan on 16/03/2017.
 */
public interface Response {

    ResponseDataType requiredDataType();

    void create(EasyJSON.JSONElement data);

    EasyJSON exportResponse();

    void run(Object data);

}
