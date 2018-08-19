package ch.vorburger.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Internal Listener that just provides debug info if ManagedProcessListener is not set externally
 */
class ManagedProcessListenerInternal implements ManagedProcessListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public void onProcessComplete(int exitValue) {
        logger.trace("Process completed exitValue: {}", exitValue);
    }

    public void onProcessFailed(int exitValue, Throwable throwable) {
        logger.trace("Process failed exitValue: {}", exitValue, throwable);
    }
}
