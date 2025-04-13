package com.zervice.kbase.utils;

import com.alibaba.fastjson.JSONObject;
import com.zervice.kbase.ai.convert.pojo.Python;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * convert python functions to json object or sting
 * - key:function name
 * - value: function body （entire function）
 */
public class PythonToJsonConverter {

    /**
     * convert yaml text to JSONObject
     *
     * @param pythonContent yml text
     * @return JSONObject
     * @throws IOException e
     */
    public static JSONObject convertPythonToJson(String pythonContent) throws IOException {
        return _parseFunctions(pythonContent);
    }

    /**
     * convert yaml text to JSONObject
     *
     * @param pythonContent python code text
     * @return Json string
     */
    public static String convertPythonToJsonStr(String pythonContent) {
        return JSONObject.toJSONString(_parseFunctions(pythonContent), true);
    }

    public static void main(String[] args) {
        String s = "import sqlite3\n" +
                   "\n" +
                   "def connect_db():\n" +
                   "    return sqlite3.connect('user_info.db')\n" +
                   "\n" +
                   "\n" +
                   "def validate_account_funds(amount_of_money):\n" +
                   "    conn = connect_db()\n" +
                   "    cursor = conn.cursor()\n" +
                   "\n" +
                   "    cursor.execute(\"SELECT account_balance FROM user_info WHERE user_name = ?\", ('user',)) \n" +
                   "    account_balance = cursor.fetchone()\n" +
                   "\n" +
                   "    if account_balance is None:\n" +
                   "        print(\"doesn't exist!\")\n" +
                   "        conn.close()\n" +
                   "        return False\n" +
                   "\n" +
                   "    if account_balance[0] >= amount_of_money:\n" +
                   "        print(\"suffient\")\n" +
                   "        conn.close()\n" +
                   "        return True\n" +
                   "    else:\n" +
                   "        print(\"insuffient\")\n" +
                   "        conn.close()\n" +
                   "        return False\n" +
                   "\n" +
                   "def submit_transaction(amount_of_money, recipient):\n" +
                   "    conn = connect_db()\n" +
                   "    cursor = conn.cursor()\n" +
                   "\n" +
                   "    cursor.execute('''\n" +
                   "    CREATE TABLE IF NOT EXISTS transactions (\n" +
                   "        transaction_id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                   "        amount_of_money REAL,\n" +
                   "        recipient TEXT,\n" +
                   "        transaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                   "    )\n" +
                   "    ''')\n" +
                   "\n" +
                   "    cursor.execute('''\n" +
                   "    INSERT INTO transactions (amount_of_money, recipient)\n" +
                   "    VALUES (?, ?)\n" +
                   "    ''', (amount_of_money, recipient))\n" +
                   "\n" +
                   "    conn.commit()\n" +
                   "    conn.close()\n" +
                   "\n" +
                   "    print(f\"Success. Money: {amount_of_money}, recipient: {recipient}\")\n";
        System.out.println(convertPythonToJsonStr(s));
    }

    /**
     * Parse functions and return a JSON object
     *
     * @param pythonCode
     * @return
     */
    private static JSONObject _parseFunctions(String pythonCode) {
        JSONObject functionsJson = new JSONObject();

        // 正则表达式匹配完整的函数定义，包括函数头和完整函数体
        Pattern pattern = Pattern.compile("(def\\s+\\w+\\s*\\([^)]*\\)\\s*:\\s*(\\n(?:[ \\t]+.*)?)+)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(pythonCode);

        while (matcher.find()) {
            String fullFunction = matcher.group(1);
            // 提取函数名
            String functionName = _extractFunctionName(fullFunction);

            // 内置的填充方法，无需处理
            if (Python.CONTEXT_VARIABLES_NAME.equals(functionName)) {
                continue;
            }

            // 将函数名和完整函数体存入 JSON
            functionsJson.put(functionName, fullFunction);
        }

        return functionsJson;
    }

    /**
     * 提取函数名
     */
    private static String _extractFunctionName(String fullFunction) {
        Pattern namePattern = Pattern.compile("def\\s+(\\w+)\\s*\\(");
        Matcher nameMatcher = namePattern.matcher(fullFunction);
        if (nameMatcher.find()) {
            return nameMatcher.group(1);
        }
        return "unknown_function";
    }
}
