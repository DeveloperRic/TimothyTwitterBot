package victorolaitan.timothyTwitterBot.util;

import com.sun.istack.internal.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * EasyJSON is a class created to help simplify the JSON process.
 * No need to create new Objects for each item you want to add.
 * Simply use: foo.putGeneric(value) or foo.putGeneric(key, value) or foo.putArray(key, values...) or foo.putStructure("key").
 * You can also add items inline. For example:
 * <p>
 * EasyJSON json = EasyJSON.create("EasyJSON Example");
 * <p>
 * json.putStructure("pets").putArray("dogs").putGeneric("pug");
 * <p>
 * json.search("pets", "dogs").putGeneric("rottweiler");
 * <p>
 * json.search("pets").putArray("cats", "i'm not a cat guy");
 * <p>
 * will result in a structure like this:
 * <p>
 * "EasyJSON Example":{
 * <p>
 * "pets":{
 * <p>
 * "cats":["i'm not a cat guy"],
 * <p>
 * "dogs":["pug", "rottweiler"]
 * <p>
 * }
 * <p>
 * }
 * <p>
 * Initial commit by Victor Olaitan on 09/03/2017.
 */
public class EasyJSON {
    /**
     * Creates an empty EasyJSON instance.
     *
     * @return
     */
    public static EasyJSON create() {
        return new EasyJSON();
    }

    /**
     * Reads the specified file and attempts to parse it into an EasyJSON structure.
     *
     * @param filePath The path of the file relative to the Java instance (or full path ie. c: .... )
     * @return The parsed EasyJSON structure
     * @throws IOException    if the file cannot be read.
     * @throws ParseException if the file's JSON structure is incompatible with EasyJSON.
     */
    public static EasyJSON open(String filePath) throws IOException, ParseException {
        return new EasyJSON(filePath);
    }

    /**
     *
     */
    public static class ParseException extends Exception {
        public static final int UNEXPECTED_KEY = 0;
        public static final int UNEXPECTED_TOKEN = 1;
        public static final int INCOMPATIBLE_FILE = 2;
        public static final int READ_ERROR = 3;

        public ParseException(int index) {
            super(translateIndex(index));
        }

        public ParseException(int index, String message) {
            super(translateIndex(index) + message);
        }

        private static String translateIndex(int index) {
            switch (index) {
                case 0:
                    return "Unexpected key : ";
                case 1:
                    return "Unexpected token in EasyJSON structure : ";
                case 2:
                    return "The file specified is not properly defined!";
                case 3:
                    return "A read error occurred when attempting to load the file -> ";
                default:
                    return "Unexpected argument : ";
            }
        }
    }

    public JSONElement root;
    String filePath;

    private EasyJSON() {
        root = new JSONElement(null, JSONElementType.ROOT, "", "");
    }

    private EasyJSON(String filePath) throws ParseException {
        root = new JSONElement(null, JSONElementType.ROOT, "", "");
        try {
            if (!filePath.equals("")) {
                JSONObject obj = null;
                obj = (JSONObject) (new JSONParser()).parse(new FileReader(filePath));
                for (Object key : obj.keySet()) {
                    if (!(key instanceof String)) {
                        throw new ParseException(ParseException.UNEXPECTED_KEY, "EasyJSON can't handle non-string keys yet!");
                    }
                    Object value = obj.get(key);
                    JSONElementType type = JSONElementType.PRIMITIVE;
                    if (value instanceof String) {
                        type = JSONElementType.PRIMITIVE;
                    } else if (value instanceof JSONArray) {
                        type = JSONElementType.ARRAY;
                    } else if (value instanceof JSONObject) {
                        type = JSONElementType.STRUCTURE;
                    }
                    JSONElement rootElement = new JSONElement(null, type, key.toString(), value);
                    iterateElement(rootElement);
                    root.children.add(rootElement);
                }
            } else {
                throw new ParseException(ParseException.UNEXPECTED_TOKEN, "The file path specified is invalid!");
            }
        } catch (org.json.simple.parser.ParseException e) {
            throw new ParseException(ParseException.INCOMPATIBLE_FILE);
        } catch (IOException e) {
            throw new ParseException(ParseException.READ_ERROR, e.getMessage());
        }
    }

    private void iterateElement(JSONElement targetItem) throws ParseException {
        if (targetItem.type == JSONElementType.ARRAY) {
            JSONArray array = (JSONArray) targetItem.value;
            for (Object arrayItem : array) {
                JSONElementType type = JSONElementType.PRIMITIVE;
                if (arrayItem instanceof String) {
                    type = JSONElementType.PRIMITIVE;
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
                    throw new ParseException(ParseException.UNEXPECTED_KEY, "EasyJSON can't handle non-string keys yet!");
                }
                Object value = structure.get(key);
                JSONElementType type = JSONElementType.PRIMITIVE;
                if (value instanceof JSONArray) {
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
        PRIMITIVE,
        ARRAY,
        STRUCTURE,
        ROOT
    }

    public class JSONElement {
        public JSONElementType type;
        public ArrayList<JSONElement> children = new ArrayList<>();
        public String key;
        public Object value;
        public JSONElement parent;

        JSONElement(JSONElement parent, JSONElementType type, String key, Object value) {
            this.parent = parent;
            this.type = type;
            this.key = key;
            this.value = value;
        }

        public JSONElement putGeneric(String key, Object value) {
            JSONElement element;
            if (value instanceof EasyJSON) {
                element = ((EasyJSON) value).root;
            } else if (value instanceof JSONElement) {
                element = (JSONElement) value;
            } else {
                element = new JSONElement(this, JSONElementType.PRIMITIVE, key, value);
            }
            children.add(element);
            return element;
        }

        public JSONElement putGeneric(Object value) {
            JSONElement element;
            if (value instanceof EasyJSON) {
                element = ((EasyJSON) value).root;
            } else if (value instanceof JSONElement) {
                element = (JSONElement) value;
            } else {
                element = new JSONElement(this, JSONElementType.PRIMITIVE, null, value);
            }
            children.add(element);
            return element;
        }

        public JSONElement putStructure(@Nullable String key) {
            JSONElement element;
            if (value instanceof EasyJSON) {
                element = ((EasyJSON) value).root;
            } else if (value instanceof JSONElement) {
                element = (JSONElement) value;
            } else {
                element = new JSONElement(this, JSONElementType.STRUCTURE, key, null);
            }
            children.add(element);
            return element;
        }

        public JSONElement putArray(String key, Object... items) {
            JSONElement element = new JSONElement(this, JSONElementType.ARRAY, key, "");
            for (Object item : items) {
                if (value instanceof EasyJSON) {
                    element.children.add(((EasyJSON) value).root);
                } else if (value instanceof JSONElement) {
                    element.children.add((JSONElement) value);
                } else {
                    element.children.add(new JSONElement(element, JSONElementType.PRIMITIVE, "", item));
                }
            }
            children.add(element);
            return element;
        }

        public JSONElement search(String... location) {
            return deepSearch(this, location, 0);
        }

        public <T> T valueOf(String... location) {
            return (T) search(location).value;
        }
    }

    public JSONElement putGeneric(String key, Object value) {
        return root.putGeneric(key, value);
    }

    public JSONElement putGeneric(Object value) {
        return root.putGeneric(value);
    }

    public JSONElement putStructure(@Nullable String key) {
        return root.putStructure(key);
    }

    public JSONElement putArray(String key, Object... items) {
        return root.putArray(key, items);
    }

    public JSONElement search(String... location) {
        return deepSearch(root, location, 0);
    }

    public <T> T valueOf(String... location) {
        return (T) search(location).value;
    }

    private JSONElement deepSearch(JSONElement element, String[] location, int locPosition) {
        for (int i = 0; locPosition < location.length && i < element.children.size(); i++) {
            JSONElement child = element.children.get(i);
            if (child.key.equals(location[locPosition])) {
                if (locPosition == location.length - 1) {
                    return child;
                } else {
                    return deepSearch(child, location, locPosition + 1);
                }
            }
        }
        return null;
    }

    public JSONObject exportToJSONOject() throws ParseException {
        return deepSave(new JSONObject(), root);
    }

    public void save() throws ParseException {
        if (filePath.equals("")) {
            throw new ParseException(ParseException.INCOMPATIBLE_FILE, "Error while saving EasyJSON instance! : Instance was not created with a file path. Use the save(String altPath) method to save to a compatible file.");
        }
        checkExists(filePath);
        JSONObject obj = new JSONObject();
        obj = deepSave(obj, root);
        try (FileWriter file = new FileWriter(filePath)) {
            file.write(obj.toJSONString());
            file.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkExists(String path) throws ParseException {
        File file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new ParseException(ParseException.INCOMPATIBLE_FILE, "Error while saving EasyJSON instance! : Failed to create a new file.");
            }
        }
    }

    public void save(String altPath) throws ParseException {
        JSONObject obj = new JSONObject();
        checkExists(altPath);
        obj = deepSave(obj, root);
        try (FileWriter file = new FileWriter(altPath)) {
            file.write(obj.toJSONString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //FIXME fix deepsave recursive return operation
    private <T> T deepSave(T currentJSONRef, JSONElement currentElement) throws ParseException {
        for (JSONElement child : currentElement.children) {
            Object objectToAdd;
            switch (child.type) {
                case ARRAY:
                    objectToAdd = deepSave(new JSONArray(), child);
                    break;
                case STRUCTURE:
                    objectToAdd = deepSave(new JSONObject(), child);
                    break;
                case ROOT:
                    objectToAdd = deepSave(new JSONObject(), child);
                    break;
                default:
                    objectToAdd = child.value;
            }
            if (objectToAdd != null) {
                if (currentJSONRef instanceof JSONObject) {
                    JSONObject object = (JSONObject) currentJSONRef;
                    object.put(child.key, objectToAdd);
                    currentJSONRef = (T) object;
                } else if (currentJSONRef instanceof JSONArray) {
                    JSONArray array = (JSONArray) currentJSONRef;
                    array.add(objectToAdd);
                    currentJSONRef = (T) array;
                } else {
                    throw new ParseException(ParseException.UNEXPECTED_TOKEN, "Error while saving EasyJSON instance! : Incompatible element design.");
                }
            } else {
                throw new ParseException(ParseException.UNEXPECTED_TOKEN, "Error while saving EasyJSON instance! : Incompatible element design.");
            }
        }
        return currentJSONRef;
    }
}
