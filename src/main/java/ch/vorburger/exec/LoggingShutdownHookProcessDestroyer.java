package ch.vorburger.exec;

import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class LoggingShutdownHookProcessDestroyer extends ShutdownHookProcessDestroyer {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void run() {
        logger.info("Shutdown Hook: JVM is about to exit! Going to kill destroyOnShutdown processes...");
        super.run();
    }
}
