package com.liudaolunhuibl.plugin.core.pojo.record;

public record NormalRecord(
        /**
         * 姓名
         */
        String name,

        /**
         * 地址
         */
        String address,

        /**
         * 号码
         */
        Integer number) {}
