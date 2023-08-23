package com.liudaolunhuibl.plugin.core;

import lombok.experimental.UtilityClass;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yunfanzhang@kuainiugroup.com
 * @version 1.0
 * @ClassName: JavaTypeHelper
 * @Description: java类型帮助
 * @date 2023/6/23
 */
@UtilityClass
public class JavaTypeMapping {

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

}
