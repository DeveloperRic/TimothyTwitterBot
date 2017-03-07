package victorolaitan.timothyTwitterBot.util;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Initial commit by Victor Olaitan on 07/03/2017.
 */
public class JSONUtil {
    public static class EasyJSON {
        public static EasyJSON create() {
            return new EasyJSON();
        }

        public static EasyJSON open(String filePath) throws IOException, ParseException {
            return new EasyJSON(filePath);
        }

        JSONObject obj;
        JSONElement root;

        private EasyJSON() {
            root = new JSONElement(null, JSONElementType.ROOT, "", null);
            obj = new JSONObject();
        }

        private EasyJSON(String filePath) throws IOException, ParseException {
            root = new JSONElement(null, JSONElementType.ROOT, "", null);
            if (filePath.equals("")) {
                obj = new JSONObject();
            } else {
                obj = (JSONObject) (new JSONParser()).parse(new FileReader(filePath));
                for (Object key : obj.keySet()) {
                    if (!(key instanceof String)) {
                        throw new ParseException(ParseException.ERROR_UNEXPECTED_TOKEN, "EasyJSON can't handle non-string keys yet!");
                    }
                    Object value = obj.get(key);
                    JSONElementType type = JSONElementType.UNKNOWN;
                    if (value instanceof String) {
                        type = JSONElementType.STRING;
                    } else if (value instanceof JSONArray) {
                        type = JSONElementType.ARRAY;
                    } else if (value instanceof JSONObject) {
                        type = JSONElementType.STRUCTURE;
                    }
                    JSONElement rootElement = new JSONElement(null, type, key.toString(), value);
                    iterateElement(rootElement);
                    root.children.add(rootElement);
                }
            }
        }

        private void iterateElement(JSONElement targetItem) throws ParseException {
            if (targetItem.type == JSONElementType.ARRAY) {
                JSONArray array = (JSONArray) targetItem.value;
                for (Object arrayItem : array) {
                    JSONElementType type = JSONElementType.UNKNOWN;
                    if (arrayItem instanceof String) {
                        type = JSONElementType.STRING;
                    } else if (arrayItem instanceof JSONArray) {
                        type = JSONElementType.ARRAY;
                    } else if (arrayItem instanceof JSONObject) {
                        type = JSONElementType.STRUCTURE;
                    }
                    JSONElement newItem = new JSONElement(targetItem, type, "", arrayItem);
                    iterateElement(newItem);
                    targetItem.children.add(newItem);
                }
            } else if (targetItem.type == JSONElementType.STRUCTURE) {
                JSONObject structure = (JSONObject) targetItem.value;
                for (Object key : structure.keySet()) {
                    if (!(key instanceof String)) {
                        throw new ParseException(ParseException.ERROR_UNEXPECTED_TOKEN, "EasyJSON can't handle non-string keys yet!");
                    }
                    Object value = obj.get(key);
                    JSONElementType type = JSONElementType.UNKNOWN;
                    if (value instanceof String) {
                        type = JSONElementType.STRING;
                    } else if (value instanceof JSONArray) {
                        type = JSONElementType.ARRAY;
                    } else if (value instanceof JSONObject) {
                        type = JSONElementType.STRUCTURE;
                    }
                    JSONElement newItem = new JSONElement(targetItem, type, key.toString(), value);
                    iterateElement(newItem);
                    targetItem.children.add(newItem);
                }
            }
        }

        private enum JSONElementType {
            STRING,
            ARRAY,
            STRUCTURE,
            UNKNOWN,
            ROOT
        }

        private class JSONElement {
            JSONElementType type;
            ArrayList<JSONElement> children = new ArrayList<>();
            String key;
            Object value;
            JSONElement parent;

            JSONElement(JSONElement parent, JSONElementType type, String key, Object value) {
                this.parent = parent;
                this.type = type;
                this.key = key;
                this.value = value;
            }

            public void put(String key, Object value) {
                children.add(new JSONElement(root, JSONElementType.STRING, key, value));
            }

            public void putArray(String key, Object... items) {
                ArrayList<Object> array = new ArrayList<>();
                for (Object item : items) {
                    array.add(item);
                }
                children.add(new JSONElement(root, JSONElementType.ARRAY, key, array));
            }
        }

        public void put(String key, Object value) {
            root.put(key, value);
        }

        public void putArray(String key, Object... items) {
            root.putArray(key, items);
        }

        public JSONElement search(String... location) {
            return deepSearch(root, location, 0);
        }

        private JSONElement deepSearch(JSONElement element, String[] location, int locPosition) {
            for (int i = 0; locPosition < location.length && i < element.children.size(); i++) {
                JSONElement child = element.children.get(i);
                if (element.key.equals(location[locPosition])) {
                    if (locPosition == location.length - 1) {
                        return child;
                    } else {
                        return deepSearch(child, location, locPosition + 1);
                    }
                }
            }
            return null;
        }
    }
}
