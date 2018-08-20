package ch.vorburger.exec;

import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

class CompositeExecuteResultHandler extends DefaultExecuteResultHandler {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<? extends ExecuteResultHandler> handlers;
    private final ManagedProcessState managedProcessState;

    public CompositeExecuteResultHandler(ManagedProcessState managedProcessState, List<? extends ExecuteResultHandler> handlers) {
        super();
        this.managedProcessState = requireNonNull(managedProcessState, "managedProcessState can't be null");
        this.handlers = new ArrayList<>(handlers);
    }

    @Override
    public void onProcessComplete(int exitValue) {
        super.onProcessComplete(exitValue);
        for (ExecuteResultHandler handler : handlers) {
            try {
                handler.onProcessComplete(exitValue);
            } catch (Exception e) {
                logger.error(managedProcessState.getProcLongName() + " process handler failed on processComplete",e);
            }
        }
    }

    @Override
    public void onProcessFailed(ExecuteException processFailedException) {
        super.onProcessFailed(processFailedException);
        for (ExecuteResultHandler handler : handlers) {
            try {
                handler.onProcessFailed(processFailedException);
            } catch (Exception e) {
                logger.error(managedProcessState.getProcLongName() + " process handler failed on processComplete",e);
            }
        }
    }
}
