package ch.vorburger.exec;

import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static java.util.Objects.requireNonNull;

/**
 * Extends Commons Exec's {@link DefaultExecuteResultHandler} with logging and notify state to initializing class.
 */
public class LoggingExecuteResultHandler extends DefaultExecuteResultHandler {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ManagedProcessState managedProcessState;

    public LoggingExecuteResultHandler(ManagedProcessState managedProcessState) {
        super();
        this.managedProcessState = requireNonNull(managedProcessState, "managedProcessState can't be null");
    }

    @Override
    public void onProcessComplete(int exitValue) {
        super.onProcessComplete(exitValue);
        logger.info(managedProcessState.getProcLongName() + " just exited, with value " + exitValue);
        managedProcessState.notifyProcessHalted();
    }

    @Override
    public void onProcessFailed(ExecuteException e) {
        super.onProcessFailed(e);
        if (!managedProcessState.watchDogKilledProcess()) {
            logger.error(managedProcessState.getProcLongName() + " failed unexpectedly", e);
        }
        managedProcessState.notifyProcessHalted();
    }
}
