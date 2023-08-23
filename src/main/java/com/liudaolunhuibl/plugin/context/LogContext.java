package com.liudaolunhuibl.plugin.context;

import lombok.experimental.UtilityClass;
import org.apache.maven.plugin.logging.Log;

/**
 * @author yunfanzhang@kuainiugroup.com
 * @version 1.0
 * @ClassName: LogContext
 * @Description: 日志上下文
 * @date 2023/8/23
 */
@UtilityClass
public class LogContext {

    private static final ThreadLocal<Log> LOG_LOCAL = new ThreadLocal<>();

    public void saveLog(Log log) {
        LOG_LOCAL.set(log);
    }

    public Log getLog() {
        return LOG_LOCAL.get();
    }

    public void clear() {
        LOG_LOCAL.remove();
    }
}
