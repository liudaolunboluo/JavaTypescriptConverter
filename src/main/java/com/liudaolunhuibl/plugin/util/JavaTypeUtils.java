package com.liudaolunhuibl.plugin.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yunfanzhang@kuainiugroup.com
 * @version 1.0
 * @ClassName: JavaTypeUtils
 * @Description: java类型工具类
 * @date 2023/6/23
 */
@UtilityClass
public class JavaTypeUtils {

    /**
     * map内部类型正则
     */
    private static final Pattern MAP_PATTERN = Pattern.compile("<(.+?),(.+?)>");

    /**
     * Map类型变量正则
     */
    private static final Pattern MAP_TYPE_PATTERN = Pattern.compile("(private)\\s+(Map<[^>]+>)\\s+(\\w+);");

    /**
     * 普通类型变量正则
     */
    private static final Pattern NORMAL_TYPE_PATTERN = Pattern.compile("(private)\\s+(\\w+)\\s+(\\w+);");

    /**
     * 集合类型变量正则
     */
    private static final Pattern LIST_TYPE_PATTERN = Pattern.compile("(private)\\s+(List<[^>]+>)\\s+(\\w+);");

    private static final Map<String, String> TYPE_MAP = new HashMap<>();

    static {
        TYPE_MAP.put("byte", "number");
        TYPE_MAP.put("short", "number");
        TYPE_MAP.put("int", "number");
        TYPE_MAP.put("long", "number");
        TYPE_MAP.put("float", "number");
        TYPE_MAP.put("double", "number");
        TYPE_MAP.put("char", "string");
        TYPE_MAP.put("boolean", "boolean");
        TYPE_MAP.put("String", "string");
        TYPE_MAP.put("Byte", "number");
        TYPE_MAP.put("Short", "number");
        TYPE_MAP.put("Integer", "number");
        TYPE_MAP.put("Long", "number");
        TYPE_MAP.put("Float", "number");
        TYPE_MAP.put("Double", "number");
        TYPE_MAP.put("Boolean", "boolean");
        TYPE_MAP.put("Object", "object");
        TYPE_MAP.put("Date", "Date");
        TYPE_MAP.put("BigDecimal", "number");
    }

    /**
     * java类型和ts类型转换
     *
     * @param propertyType:属性的java类型
     * @return ts类型
     * @author zhangyunfan
     * @date 2023/6/23
     */
    public String javaTypeConvertTypescriptType(String propertyType) {
        return TYPE_MAP.get(propertyType) == null ? propertyType : TYPE_MAP.get(propertyType);
    }

    /**
     * 获取Map的类型
     *
     * @param mapCode:java map的代码
     * @return Map中的java类型
     * @author zhangyunfan
     * @date 2023/6/23
     */
    public Pair<String, String> getMapType(String mapCode) {
        Matcher matcher = MAP_PATTERN.matcher(mapCode);
        if (matcher.find()) {
            return Pair.of(matcher.group(1).trim(), matcher.group(2).trim());
        }
        return null;
    }

    /**
     * 获取 List集合中的java类型
     *
     * @param collectionType：List代码
     * @return List集合中的java类型
     * @author zhangyunfan
     * @date 2023/6/23
     */
    public String getCollectionRealType(String collectionType) {
        return collectionType.replace("List<", "").replace(">", "");
    }

    /**
     * 获取pojo类中属性java代码中的变量名和类型
     *
     * @param codeLine:pojo类中属性java代码
     * @return java代码中的变量名和类型
     * @author zhangyunfan
     * @date 2023/6/23
     */
    public Pair<String, String> getTypeAndVariableName(String codeLine) {
        Pattern pattern;
        if (codeLine.contains("List<")) {
            pattern = LIST_TYPE_PATTERN;
        } else if (codeLine.contains("Map<")) {
            pattern = MAP_TYPE_PATTERN;
        } else {
            pattern = NORMAL_TYPE_PATTERN;
        }
        Matcher matcher = pattern.matcher(codeLine);
        if (matcher.find()) {
            return Pair.of(matcher.group(2), matcher.group(3));
        }
        return null;

    }

}
