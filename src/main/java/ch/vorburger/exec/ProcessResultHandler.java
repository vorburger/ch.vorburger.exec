package ch.vorburger.exec;

import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.ExecuteException;

class ProcessResultHandler extends DefaultExecuteResultHandler {
    private final ManagedProcessListener listener;

    /**
     * extends <CODE>DefaultExecuteResultHandler</CODE> used for asynchronous process handling.
     * @param listener
     */
    public ProcessResultHandler(ManagedProcessListener listener) {
        if (listener == null) {
            //set internal listener
            this.listener = new ManagedProcessListenerInternal();
        } else {
            this.listener = listener;
        }
    }

    @Override
    public void onProcessComplete(int exitValue) {
        super.onProcessComplete(exitValue);
        listener.onProcessComplete(exitValue);
    }

    @Override
    public void onProcessFailed(ExecuteException processFailedException) {
        super.onProcessFailed(processFailedException);
        listener.onProcessFailed(processFailedException.getExitValue(), processFailedException);
    }
}
