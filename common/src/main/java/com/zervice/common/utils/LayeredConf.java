package com.zervice.common.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A simple layered configuration file
 * <p>
 * It could be in JSON format (so each sub JSON object is a layer)
 * Or it could be a java property file, with key as dot-seperated levels like
 * a.b.c = foo
 * a.b.array = bar, baz, boz
 * euals to json
 * {
 * a: {
 * b: {
 * c: foo,
 * array: ["bar", "baz", "boz"]
 * }
 * }
 * }
 * <p>
 * TODO: Add a notification mecanism when reloading conf from file or JSON object to notify the
 * changing configurations
 */
public class LayeredConf {
    @Getter
    private static final LayeredConf _instance = new LayeredConf();

    private LayeredConf() {

    }

    /**
     * The root JSON configuration object
     */
    private JSONObject _root = new JSONObject();

    /**
     * Load configuration from a JSON file
     *
     * @param path
     */
    public void load(String path) throws IOException {
        if(path.endsWith(".json")) {
            load(path, true);
        }
        else {
            load(path, false);
        }
    }

    /**
     * Load from a file
     *
     * @param path
     * @param json - if the file is a JSON conf or a java Util props ...
     * @throws IOException
     */
    public void load(String path, boolean json) throws IOException {
        File confFile = _open(path);

        if (json) {
            load(JSONObject.parseObject(IOUtils.toString(new FileReader(confFile))));
        } else {
            Properties props = new Properties();
            props.load(new FileInputStream(confFile));
            load(props);
        }
    }

    public void load(Properties props) {
        JSONObject jsonProps = new JSONObject();

        Map<String, JSONObject> cache = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            String prefix = "";
            String confKey = key;
            int idx = key.lastIndexOf(".");
            if(idx > 0) {
                prefix = key.substring(0, idx);
                confKey = key.substring(idx + 1);
            }

            if(StringUtils.isEmpty(prefix)) {
                _populate(jsonProps, confKey, value);
                continue;
            }

            JSONObject jo = cache.get(prefix);
            if(jo == null) {
                String[] parts = StringUtils.split(prefix, ".");
                if (parts == null || parts.length == 0) {
                    continue;
                }

                jo = jsonProps;
                for (int i = 0; i < parts.length; i++) {
                    if (jo.containsKey(parts[i])) {
                        jo = jo.getJSONObject(parts[i]);
                    } else {
                        JSONObject tmp = new JSONObject();
                        jo.put(parts[i], tmp);
                        jo = tmp;
                    }
                }

                cache.put(prefix, jo);
            }

            _populate(jo, confKey, value);
        }

        load(jsonProps);
    }

    public void load(JSONObject jo) {
        _root = jo;
    }

    private void _populate(JSONObject jo, String key, String value) {
        // we ignore empty value!!!
        value = StringUtils.trim(value);
        if (StringUtils.isEmpty(value)) {
            return;
        }

        // let's see what's the value
        // if it has a ',' it would be a array type ...
        // We support array type of the same type, like all strings, all integers or all booleans
        if (value.indexOf(',') >= 0) {
            // an array!!
            String[] parts = StringUtils.split(value, ",");
            JSONArray ja = new JSONArray();
            Arrays.stream(parts).forEach(v -> ja.add(_toValue(v)));
            jo.put(key, ja);
        } else {
            jo.put(key, _toValue(value));
        }
    }

    /**
     * Decide the possible value represented by a string, we guess using following order
     * 1. if surrounded by \" treat as string
     * 2. if one of true or false, treat as boolean
     * 3. if integer treat as integer
     * 4. if long treat as long
     * 5. if double ,treat as double
     * 6. treat as string
     *
     * @param value
     * @return
     */
    private Object _toValue(String value) {
        if (value.startsWith("\"") && value.endsWith("")) {
            return value.substring(1, value.length() - 1);
        }

        if ("true".equalsIgnoreCase(value)) {
            return true;
        } else if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ne) {
            // ignore
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ne) {
            // ignore
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ne) {
            // ignore
        }

        return value;
    }


    /**
     * Get a config object to wrap the leave level JSON object for retrieving configurations
     *
     * @param paths        - paths to get into level by level
     * @param includingKey - if the key is included in the path? if so we shall skip the last level
     * @return A config object to wrap the last level JSON object
     */
    private Config _getConfig(String[] paths, boolean includingKey) {
        JSONObject jo = _getConfigObject(paths, includingKey);
        return new Config(jo);
    }

    private JSONObject _getConfigObject(String[] paths, boolean includingKey) {
        JSONObject jo = _root;

        final int levels = includingKey ? paths.length - 1 : paths.length;

        for (int i = 0; i < levels; i++) {
            if (!jo.containsKey(paths[i])) {
                return new JSONObject();
            }

            jo = jo.getJSONObject(paths[i]);
            if (jo == null) {
                throw new IllegalStateException("Missing part at level " + i + " in configuration path " + paths[i]);
            }
        }

        return jo;
    }

    public static class Config {
        @Getter
        private final JSONObject _configObject;

        public Config() {
            _configObject = new JSONObject();
        }

        public Config(JSONObject obj) {
            if (obj == null) {
                throw new IllegalStateException("Create Config object with null JSON object!");
            }
            _configObject = obj;
        }

        public boolean hasSubConfig(String key) {
            if(!_configObject.containsKey(key)) {
                return false;
            }

            Object obj = _configObject.get(key);
            return obj instanceof JSONObject;
        }

        public boolean has(String key) {
            return _configObject.containsKey(key);
        }

        /**
         * We don't allow dotted path here!
         *
         * @param key
         * @return
         */
        public Config getSubConfig(String key) {
            if (_configObject.containsKey(key)) {
                return new Config(_configObject.getJSONObject(key));
            } else {
                return new Config();
            }
        }

        public int getInt(String key) {
            return _configObject.getInteger(key);
        }

        public int getInt(String key, int def) {
            if (_configObject.containsKey(key)) {
                return getInt(key);
            }

            return def;
        }

        public long getLong(String key) {
            return _configObject.getLong(key);
        }

        public long getLong(String key, long def) {
            if (_configObject.containsKey(key)) {
                return getLong(key);
            }

            return def;
        }

        public String getString(String key) {
            return _configObject.getString(key);
        }

        public String getString(String key, String def) {
            if (_configObject.containsKey(key)) {
                return getString(key);
            }

            return def;
        }

        public double getDouble(String key) {
            return _configObject.getDouble(key);
        }

        public double getDouble(String key, double def) {
            if (_configObject.containsKey(key)) {
                return getDouble(key);
            }

            return def;
        }

        public boolean getBoolean(String key) {
            return _configObject.getBoolean(key);
        }

        public boolean getBoolean(String key, boolean def) {
            if (_configObject.containsKey(key)) {
                return getBoolean(key);
            }

            return def;
        }

        /**
         * For complex array JSON sub configs ... Not supported in property files
         * @param key
         * @return
         */
        public Config[] getSubConfigArray(String key) {
            JSONArray ja = _configObject.getJSONArray(key);
            Config[] values = new Config[ja.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = new Config(ja.getJSONObject(i));
            }

            return values;
        }

        /**
         * We treat array with same type!
         */
        public int[] getIntArray(String key) {
            JSONArray ja = _configObject.getJSONArray(key);
            int[] values = new int[ja.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = ja.getInteger(i);
            }

            return values;
        }

        public long[] getLongArray(String key) {
            JSONArray ja = _configObject.getJSONArray(key);
            long[] values = new long[ja.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = ja.getLong(i);
            }

            return values;
        }

        public String[] getStringArray(String key) {
            Object o = _configObject.get(key);
            if (o == null) {
                return new String[0];
            }
            if (o instanceof JSONArray) {
                JSONArray ja = (JSONArray) o;
                String[] values = new String[ja.size()];
                for (int i = 0; i < values.length; i++) {
                    values[i] = ja.getString(i);
                }
                return values;
            } else {
                return new String[]{o.toString()};
            }
        }

        public double[] getDoubleArray(String key) {
            JSONArray ja = _configObject.getJSONArray(key);
            double[] values = new double[ja.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = ja.getDouble(i);
            }

            return values;
        }

        public boolean[] getBooleanArray(String key) {
            JSONArray ja = _configObject.getJSONArray(key);
            boolean[] values = new boolean[ja.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = ja.getBoolean(i);
            }

            return values;
        }

    }


    /**
     * Get a config object at given a.b.c path
     *
     * @param dotPath
     * @return
     */
    public static Config getConfig(String dotPath) {
        String[] path = StringUtils.split(dotPath, ".");
        if (path == null) {
            throw new IllegalStateException("Invalid dotted path - " + dotPath);
        }

        return getInstance()._getConfig(path, false);
    }

    public static Config getRootConfig() {
        final JSONObject root = _instance._root;

        if (root == null) {
            throw new IllegalStateException("Configuration not initialized!");
        }

        return new Config(root);
    }

    public static JSONObject getConfigObject(String path) {
        String[] paths = StringUtils.split(path, ".");
        if (paths == null) {
            throw new IllegalStateException("Invalid dotted path - " + path);
        }

        return getInstance()._getConfigObject(paths, false);
    }

    /**
     * Helper wrapper to easy of using the class
     */
    public static boolean hasSubConfig(String dotPath) {
        String[] paths = StringUtils.split(dotPath, ".");
        Config config = LayeredConf.getInstance()._getConfig(paths, true);

        return config.hasSubConfig(paths[paths.length - 1]);
    }

    public static Config[] getConfigArray(String dotPath) {
        String[] paths = StringUtils.split(dotPath, ".");
        Config config = LayeredConf.getInstance()._getConfig(paths, true);
        return config.getSubConfigArray(paths[paths.length - 1]);
    }

    public static boolean has(String dotPath) {
        String[] paths = StringUtils.split(dotPath, ".");
        Config config = LayeredConf.getInstance()._getConfig(paths, true);
        return config.has(paths[paths.length - 1]);
    }

    public static String getString(String dotPath, String deVal) {
        String[] paths = StringUtils.split(dotPath, ".");
        Config config = LayeredConf.getInstance()._getConfig(paths, true);
        return config.getString(paths[paths.length - 1], deVal);
    }

    public static String[] getStringArray(String dotPath) {
        String[] paths = StringUtils.split(dotPath, ".");
        Config config = LayeredConf.getInstance()._getConfig(paths, true);
        return config.getStringArray(paths[paths.length - 1]);
    }

    public static int getInt(String dotPath, int deVal) {
        String[] paths = StringUtils.split(dotPath, ".");
        Config config = LayeredConf.getInstance()._getConfig(paths, true);
        return config.getInt(paths[paths.length - 1], deVal);
    }

    public static int[] getIntArray(String dotPath) {
        String[] paths = StringUtils.split(dotPath, ".");
        Config config = LayeredConf.getInstance()._getConfig(paths, true);
        return config.getIntArray(paths[paths.length - 1]);
    }

    public static long getLong(String dotPath, long deVal) {
        String[] paths = StringUtils.split(dotPath, ".");
        Config config = LayeredConf.getInstance()._getConfig(paths, true);
        return config.getLong(paths[paths.length - 1], deVal);
    }

    public static long[] getLongArray(String dotPath) {
        String[] paths = StringUtils.split(dotPath, ".");
        Config config = LayeredConf.getInstance()._getConfig(paths, true);
        return config.getLongArray(paths[paths.length - 1]);
    }

    public static double getDouble(String dotPath, double deVal) {
        String[] paths = StringUtils.split(dotPath, ".");
        Config config = LayeredConf.getInstance()._getConfig(paths, true);
        return config.getDouble(paths[paths.length - 1], deVal);
    }

    public static double[] getDoubleArray(String dotPath) {
        String[] paths = StringUtils.split(dotPath, ".");
        Config config = LayeredConf.getInstance()._getConfig(paths, true);
        return config.getDoubleArray(paths[paths.length - 1]);
    }

    public static boolean getBoolean(String dotPath, boolean deVal) {
        String[] paths = StringUtils.split(dotPath, ".");
        Config config = LayeredConf.getInstance()._getConfig(paths, true);
        return config.getBoolean(paths[paths.length - 1], deVal);
    }

    public static boolean[] getBooleanArray(String dotPath) {
        String[] paths = StringUtils.split(dotPath, ".");
        Config config = LayeredConf.getInstance()._getConfig(paths, true);
        return config.getBooleanArray(paths[paths.length - 1]);
    }

    //
    // helpers
    private File _open(String filePath) {
        if(filePath.startsWith("~/")) {
            // replace it with absolute path
            String home = System.getProperty("user.home");
            return new File(home + filePath.substring(1));
        }
        else {
            return new File(filePath);
        }
    }

}
