package com.liudaolunhuibl.plugin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhangyunfan@fiture.com
 * @version 1.0
 * @ClassName: TypescriptModeEnum
 * @Description: 最后生成的ts代码模式
 * @date 2025/2/19
 */
@Getter
@AllArgsConstructor
public enum TypescriptModeEnum {

    CLASS_MODEL("class"),

    INTERFACE_MODEL("interface");

    private final String mode;

    public static boolean isValidTypescriptMode(String mode) {
        for (TypescriptModeEnum type : TypescriptModeEnum.values()) {
            if (type.getMode().equals(mode)) {
                return true;
            }
        }
        return false;
    }
}
