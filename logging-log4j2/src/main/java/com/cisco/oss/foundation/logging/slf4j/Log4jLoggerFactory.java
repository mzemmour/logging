/*
 * Copyright 2015 Cisco Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.cisco.oss.foundation.logging.slf4j;

import com.cisco.oss.foundation.logging.FoundationLoggerContextFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.FoundationLoggerContext;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Yair Ogen on 20/07/2014.
 */
public class Log4jLoggerFactory implements ILoggerFactory {

    private static final String FQCN = Log4jLoggerFactory.class.getName();
    private static final String PACKAGE = "org.slf4j";

    private final  ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap();
//    private final Map<LoggerContext, ConcurrentMap<String, Logger>> contextMap =
//            new WeakHashMap<LoggerContext, ConcurrentMap<String, Logger>>();

    public void clearLoggers(){
        loggers.clear();
    }

    @Override
    public Logger getLogger(final String name) {
//        final LoggerContext context = getContext();
//        final ConcurrentMap<String, Logger> loggers = getLoggersMap(context);

        if (loggers.containsKey(name)) {
            return loggers.get(name);
        }

        final String key = Logger.ROOT_LOGGER_NAME.equals(name) ? LogManager.ROOT_LOGGER_NAME : name;

        if(!Thread.currentThread().getName().equals("FirstTimeConfigInit")){
            FoundationLoggerContextFactory.CONTEXT.toString();
            try {
//                FoundationLoggerContext.POST_CONFIG_REFRESH_LATCH.await();
//                if (FoundationLoggerContext.POST_CONFIG_LATCH.getCount() > 0) {
//                    Thread.sleep(1000);
//                }

                FoundationLoggerContext.POST_CONFIG_LATCH.await();
            } catch (InterruptedException e) {
                System.err.println("POST_CONFIG_REFRESH_LATCH was interrupted: " + e);
            }
        }
        loggers.putIfAbsent(name, new Log4jLogger(FoundationLoggerContextFactory.CONTEXT.getLogger(key), name));
        return loggers.get(name);
    }

//    private ConcurrentMap<String, Logger> getLoggersMap(final LoggerContext context) {
//        synchronized (contextMap) {
//            ConcurrentMap<String, Logger> map = contextMap.get(context);
//            if (map == null) {
//                map = new ConcurrentHashMap<String, Logger>();
//                contextMap.put(context, map);
//            }
//            return map;
//        }
//    }

    private LoggerContext getContext() {
        final Throwable t = new Throwable();
        boolean next = false;
        boolean pkg = false;
        String fqcn = LoggerFactory.class.getName();
        for (final StackTraceElement element : t.getStackTrace()) {
            if (FQCN.equals(element.getClassName())) {
                next = true;
                continue;
            }
            if (next && element.getClassName().startsWith(PACKAGE)) {
                fqcn = element.getClassName();
                pkg = true;
                continue;
            }
            if (pkg) {
                break;
            }
        }
        return PrivateManager.getContext(fqcn);
    }

    /**
     * The real bridge between SLF4J and Log4j.
     */
    private static class PrivateManager extends LogManager {
        private static final String FQCN = LoggerFactory.class.getName();

        public static LoggerContext getContext() {
            return getContext(FQCN, false);
        }

        public static LoggerContext getContext(final String fqcn) {
            return getContext(fqcn, false);
        }

        public static ExtendedLogger getLogger(final String name) {
            return getContext(FQCN).getLogger(name);
        }
    }
}
