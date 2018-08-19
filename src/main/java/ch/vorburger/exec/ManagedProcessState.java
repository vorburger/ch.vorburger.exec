package ch.vorburger.exec;

public interface ManagedProcessState {

    boolean startAndWaitForConsoleMessageMaxMs(String messageInConsole,
            long maxWaitUntilReturning) throws ManagedProcessException;

    void destroy() throws ManagedProcessException;

    boolean isAlive();

    void notifyProcessHalted();

    int exitValue() throws ManagedProcessException;

    int waitForExit() throws ManagedProcessException;

    int waitForExitMaxMs(long maxWaitUntilReturning) throws ManagedProcessException;

    void waitForExitMaxMsOrDestroy(long maxWaitUntilDestroyTimeout)
            throws ManagedProcessException;

    String getConsole();

    String getLastConsoleLines();

    boolean watchDogKilledProcess();

    String getProcLongName();
}
