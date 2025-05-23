/*
 * #%L
 * ch.vorburger.exec
 * %%
 * Copyright (C) 2012 - 2023 Michael Vorburger
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ch.vorburger.exec;

import static ch.vorburger.exec.OutputStreamType.STDERR;
import static ch.vorburger.exec.OutputStreamType.STDOUT;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Var;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.ProcessDestroyer;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import javax.annotation.Nullable;

/**
 * Managed OS Process (Executable, Program, Command). Created by {@link
 * ManagedProcessBuilder#build()}.
 *
 * <p>Intended for controlling external "tools", often "daemons", which produce some text-based
 * control output. In this form not yet suitable for programs returning binary data via stdout (but
 * could be extended).
 *
 * <p>Does reasonably extensive logging about what it's doing (contrary to Apache Commons Exec),
 * including logging the processes stdout &amp; stderr, into SLF4J (not the System.out.Console).
 *
 * @see Executor Internally based on http://commons.apache.org/exec/ but intentionally not exposing
 *     this; could be switched later, if there is any need.
 * @author Michael Vorburger
 * @author Neelesh Shastry
 * @author William Dutton
 */
public class ManagedProcess implements ManagedProcessState {

    private static final Logger logger =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final int EXITVALUE_DESTROYED = Executor.INVALID_EXITVALUE - 1;
    public static final int EXITVALUE_STILL_RUNNING = Executor.INVALID_EXITVALUE - 2;
    private static final int SLEEP_TIME_MS = 50;

    private final CommandLine commandLine;
    private final ExtendedDefaultExecutor executor = new ExtendedDefaultExecutor();
    private final StopCheckExecuteWatchdog watchDog =
            new StopCheckExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
    private final ProcessDestroyer shutdownHookProcessDestroyer =
            new LoggingShutdownHookProcessDestroyer();
    private final Map<String, String> environment;
    private final CompletableFuture<Integer> asyncResult;
    @Nullable private final InputStream input;
    private final boolean destroyOnShutdown;
    private final int consoleBufferMaxLines;
    private final OutputStreamLogDispatcher outputStreamLogDispatcher;
    private final MultiOutputStream stdout;
    private final MultiOutputStream stderr;

    private volatile boolean isAlive = false;
    private String procShortName;
    private RollingLogOutputStream console;

    /**
     * Package local constructor.
     *
     * <p>Keep ch.vorburger.exec's API separate from Apache Commons Exec, so it COULD be replaced.
     *
     * @see ManagedProcessBuilder#build()
     * @param commandLine Apache Commons Exec CommandLine
     * @param directory Working directory, or null
     * @param environment Environment Variable.
     * @param input Input stream to the console
     * @param destroyOnShutdown Ensure we get the handler for cleanup of the started processes if
     *     the main process is going to terminate.
     * @param consoleBufferMaxLines int of what we should keep in memory for the console
     * @param outputStreamLogDispatcher <tt>OutputStreamLogDispatcher</tt>
     * @param stdOuts StandardOut from the console
     * @param stdErrs StandardError from the console
     * @param listener A <tt>ManagedProcessListener</tt> which is notified when process completes or
     *     fails
     */
    @SuppressWarnings(
            "deprecation") // TODO https://github.com/vorburger/ch.vorburger.exec/issues/189
    ManagedProcess(
            CommandLine commandLine,
            File directory,
            Map<String, String> environment,
            InputStream input,
            boolean destroyOnShutdown,
            int consoleBufferMaxLines,
            OutputStreamLogDispatcher outputStreamLogDispatcher,
            List<OutputStream> stdOuts,
            List<OutputStream> stdErrs,
            ManagedProcessListener listener,
            Function<Integer, Boolean> exitValueChecker) {
        this.commandLine = commandLine;
        this.environment = environment;
        if (input != null) {
            this.input = buffer(input);
        } else {
            this.input = null; // this is safe/OK/expected; PumpStreamHandler constructor handles
            // this as
            // expected
        }
        if (directory != null) {
            executor.setWorkingDirectory(directory);
        }
        executor.setWatchdog(watchDog);
        executor.setIsSuccessExitValueChecker(exitValueChecker);
        this.destroyOnShutdown = destroyOnShutdown;
        this.consoleBufferMaxLines = consoleBufferMaxLines;
        this.outputStreamLogDispatcher = outputStreamLogDispatcher;
        this.asyncResult = new CompletableFuture<>();
        CompletableFuture<Void> unused =
                this.asyncResult.<Void>handle(
                        (result, e) -> {
                            if (e == null) {
                                logger.info(
                                        this.getProcLongName()
                                                + " just exited, with value "
                                                + result);
                                listener.onProcessComplete(result);
                            } else {
                                logger.error(this.getProcLongName() + " failed unexpectedly", e);
                                if (e instanceof ExecuteException ee) {
                                    listener.onProcessFailed(ee.getExitValue(), ee);
                                } // TODO handle non-ExecuteException cases gracefully
                            }
                            if (e != null && !(e instanceof CancellationException)) {
                                this.notifyProcessHalted();
                            }
                            return null;
                        });
        this.stdout = new MultiOutputStream();
        this.stderr = new MultiOutputStream();
        for (OutputStream stdOut : stdOuts) {
            stdout.addOutputStream(stdOut);
        }

        for (OutputStream stdErr : stdErrs) {
            stderr.addOutputStream(stdErr);
        }
    }

    // stolen from commons-io IOUtiles (@since v2.5)
    protected BufferedInputStream buffer(InputStream inputStream) {
        // reject null early on rather than waiting for IO operation to fail
        if (inputStream == null) { // not checked by BufferedInputStream
            throw new NullPointerException("inputStream == null");
        }
        return inputStream instanceof BufferedInputStream bufferedInputStream
                ? bufferedInputStream
                : new BufferedInputStream(inputStream);
    }

    /**
     * Starts the Process.
     *
     * <p>This method always immediately returns (i.e. launches the process asynchronously). Use the
     * different waitFor... methods if you want to "block" on the spawned process.
     *
     * @throws ManagedProcessException if the process could not be started
     */
    @CanIgnoreReturnValue
    public synchronized ManagedProcess start() throws ManagedProcessException {
        startPreparation();
        startExecute();
        return this;
    }

    protected synchronized void startPreparation() throws ManagedProcessException {
        if (isAlive()) {
            throw new ManagedProcessException(
                    getProcLongName()
                            + " is still running, use another ManagedProcess instance to launch"
                            + " another one");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Starting {}", getProcLongName());
        }

        PumpStreamHandler outputHandler = new PumpStreamHandler(stdout, stderr, input);
        executor.setStreamHandler(outputHandler);

        String pid = getProcShortName();
        stdout.addOutputStream(
                new SLF4jLogOutputStream(logger, pid, STDOUT, outputStreamLogDispatcher));
        stderr.addOutputStream(
                new SLF4jLogOutputStream(logger, pid, STDERR, outputStreamLogDispatcher));

        if (consoleBufferMaxLines > 0) {
            console = new RollingLogOutputStream(consoleBufferMaxLines);
            stdout.addOutputStream(console);
            stderr.addOutputStream(console);
        }

        if (destroyOnShutdown) {
            executor.setProcessDestroyer(shutdownHookProcessDestroyer);
        }
    }

    public File getExecutableFile() {
        return new File(commandLine.getExecutable());
    }

    protected synchronized void startExecute() throws ManagedProcessException {
        try {
            executor.execute(
                    commandLine,
                    environment,
                    new CompletableFutureExecuteResultHandler(asyncResult));
        } catch (IOException e) {
            throw new ManagedProcessException("Launch failed: " + commandLine, e);
        }

        // We now must give the system a say 100ms chance to run the background
        // thread now, otherwise the asyncResult in checkResult() won't work.
        //
        // This is admittedly not ideal, but to do better would require significant
        // changes to DefaultExecutor, so that its execute() would "fail fast" and
        // throw an Exception immediately if process start-up fails by doing the
        // launch in the current thread, and then spawns a separate thread only
        // for the waitFor().
        //
        // As DefaultExecutor doesn't seem to have been written with extensibility
        // in mind, and rewriting it to start gain 100ms (at the start of every process..)
        // doesn't seem to be worth it for now, I'll leave it like this, for now.
        //
        try {
            this.wait(100); // better than Thread.sleep(100); -- thank you, FindBugs
        } catch (InterruptedException e) {
            throw handleInterruptedException(e);
        }
        checkResult();

        // watchDog.isWatching() blocks if the process never started or already finished,
        // so we have to do it only after checkResult() had a chance to throw
        // ManagedProcessException
        isAlive = watchDog.isWatching(); // check watchdog is watching as DefaultExecutor sets
        // watchDog if process started successfully
    }

    /**
     * Starts the Process and waits (blocks) until the process prints a certain message.
     *
     * <p>You should be sure that the process either prints this message at some point, or otherwise
     * exits on it's own. This method will otherwise be slow, but never block forever, as it will
     * "give up" and always return after max. maxWaitUntilReturning ms.
     *
     * @param messageInConsole text to wait for in the STDOUT/STDERR of the external process
     * @param maxWaitUntilReturning maximum time to wait, in milliseconds, until returning, if
     *     message wasn't seen returning due to max. wait timeout
     * @throws ManagedProcessException for problems such as if the process already exited (without
     *     the message ever appearing in the Console)
     */
    @Override
    public boolean startAndWaitForConsoleMessageMaxMs(
            String messageInConsole, long maxWaitUntilReturning) throws ManagedProcessException {
        startPreparation();

        CheckingConsoleOutputStream checkingConsoleOutputStream =
                new CheckingConsoleOutputStream(messageInConsole);
        if (stdout != null && stderr != null) {
            stdout.addOutputStream(checkingConsoleOutputStream);
            stderr.addOutputStream(checkingConsoleOutputStream);
        }

        @Var long timeAlreadyWaited = 0;
        logger.info(
                "Thread will wait for \"{}\" to appear in Console output of process {} for max. "
                        + maxWaitUntilReturning
                        + "ms",
                messageInConsole,
                getProcLongName());

        startExecute();

        try {
            while (!checkingConsoleOutputStream.hasSeenIt() && isAlive()) {
                try {
                    Thread.sleep(SLEEP_TIME_MS);
                } catch (InterruptedException e) {
                    throw handleInterruptedException(e);
                }
                timeAlreadyWaited += SLEEP_TIME_MS;
                if (timeAlreadyWaited > maxWaitUntilReturning) {
                    logger.warn(
                            "Timed out waiting for \"\"{}\"\" after {}ms (returning false)",
                            messageInConsole,
                            maxWaitUntilReturning);
                    return false;
                }
            }

            // If we got out of the while() loop due to !isAlive() instead of messageInConsole,
            // so this means that we finished very fast, then throw the same exception as above!
            if (!checkingConsoleOutputStream.hasSeenIt()) {
                throw new ManagedProcessException(getUnexpectedExitMsg(messageInConsole));
            } else {
                return true;
            }
        } finally {
            if (stdout != null && stderr != null) {
                stdout.removeOutputStream(checkingConsoleOutputStream);
                stderr.removeOutputStream(checkingConsoleOutputStream);
            }
        }
    }

    protected String getUnexpectedExitMsg(String messageInConsole) {
        return "Asked to wait for \""
                + messageInConsole
                + "\" from "
                + getProcLongName()
                + ", but it already exited! (without that message in console)"
                + getLastConsoleLines();
    }

    protected ManagedProcessException handleInterruptedException(InterruptedException e)
            throws ManagedProcessException {
        // TODO Not sure how to best handle this... opinions welcome (see also below)
        String message =
                "Huh?! InterruptedException should normally never happen here..."
                        + getProcLongName();
        logger.error(message, e);
        return new ManagedProcessException(message, e);
    }

    // TODO we could add this as a closure on the CompletableFuture instead of checking
    protected void checkResult() throws ManagedProcessException {
        if (asyncResult.isCompletedExceptionally()) {
            // We already terminated (or never started)
            try {
                asyncResult.get(); // just called to throw the exception
            } catch (InterruptedException e) {
                throw handleInterruptedException(e);
            } catch (ExecutionException e) {
                logger.error(getProcLongName() + " failed", e);
                throw new ManagedProcessException(
                        getProcLongName() + " failed with Exception: " + getLastConsoleLines(), e);
            }
        }
    }

    /**
     * Kills the Process. If you expect that the process may not be running anymore, use if ( {@link
     * #isAlive()}) around this. If you expect that the process should still be running at this
     * point, call as is - and it will tell if it had nothing to destroy.
     *
     * @throws ManagedProcessException if the Process is already stopped (either because destroy()
     *     already explicitly called, or it terminated by itself, or it was never started)
     */
    @Override
    public void destroy() throws ManagedProcessException {
        // Note: If destroy() is ever giving any trouble, the
        // org.openqa.selenium.os.ProcessUtils may be of interest.
        if (!isAlive) {
            asyncResult.cancel(false);
            throw new ManagedProcessException(
                    getProcLongName() + " was already stopped (or never started)");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Going to destroy {}", getProcLongName());
        }

        watchDog.destroyProcess();

        try {
            // Safer to get() after destroy()
            asyncResult.get();
        } catch (InterruptedException e) {
            throw handleInterruptedException(e);
        } catch (ExecutionException e) {
            // process failed, likely because it was destroyed
        }

        if (logger.isInfoEnabled()) {
            logger.info("Successfully destroyed {}", getProcLongName());
        }

        isAlive = false;
    }

    // Java Doc shamelessly copy/pasted from java.lang.Thread#isAlive() :
    /**
     * Tests if this process is alive. A process is alive if it has been started and has not yet
     * terminated.
     *
     * @return <code>true</code> if this process is alive; <code>false</code> otherwise.
     */
    @Override
    public boolean isAlive() {
        // NOPE: return !asyncResult.hasResult();
        return isAlive;
    }

    /**
     * Allows <code>LoggingExecuteResultHandler</code> to notify if process has halted (success or
     * failure).
     */
    @Override
    public void notifyProcessHalted() {
        if (watchDog.isWatching()) {
            logger.error(
                    "Have been notified that process is finished but watchdog belives its still"
                            + " watching it");
        }

        isAlive = false;
    }

    /**
     * Returns the exit value for the subprocess.
     *
     * @return the exit value of the subprocess represented by this <code>Process</code> object. by
     *     convention, the value <code>0</code> indicates normal termination.
     * @exception ManagedProcessException if the subprocess represented by this <code>ManagedProcess
     *     </code> object has not yet terminated, or has terminated without an exit value.
     */
    @Override
    public int exitValue() throws ManagedProcessException {
        try {
            return asyncResult.get();
        } catch (InterruptedException e) {
            throw handleInterruptedException(e);
        } catch (Exception e) {
            throw new ManagedProcessException(
                    "No Exit Value, but an exception, is available for " + getProcLongName(), e);
        }
    }

    /**
     * Waits for the process to terminate.
     *
     * <p>Returns immediately if the process is already stopped (either because destroy() was
     * already explicitly called, or it terminated by itself).
     *
     * <p>Note that if the process was attempted to be started but that start failed (may be because
     * the executable could not be found, or some underlying OS error) then it throws a
     * ManagedProcessException.
     *
     * <p>It also throws a ManagedProcessException if {@link #start()} was never even called.
     *
     * @return exit value (or {@link #EXITVALUE_DESTROYED} if {@link #destroy()} was used)
     * @throws ManagedProcessException see above
     */
    @Override
    public int waitForExit() throws ManagedProcessException {
        logger.info(
                "Thread is now going to wait for this process to terminate itself: {}",
                getProcLongName());
        return waitForExitMaxMsWithoutLog(-1);
    }

    /**
     * Like {@link #waitForExit()}, but waits max. maxWaitUntilReturning, then returns (even if
     * still running, taking no action).
     *
     * @param maxWaitUntilReturning Time to wait
     * @return exit value, or {@link #EXITVALUE_STILL_RUNNING} if the timeout was reached, or {@link
     *     #EXITVALUE_DESTROYED} if {@link #destroy()} was used
     * @throws ManagedProcessException see above
     */
    @Override
    public int waitForExitMaxMs(long maxWaitUntilReturning) throws ManagedProcessException {
        logger.info(
                "Thread is now going to wait max. {}ms for process to terminate itself: {}",
                maxWaitUntilReturning,
                getProcLongName());
        return waitForExitMaxMsWithoutLog(maxWaitUntilReturning);
    }

    protected int waitForExitMaxMsWithoutLog(long maxWaitUntilReturningInMs)
            throws ManagedProcessException {
        assertWaitForIsValid();
        try {
            if (maxWaitUntilReturningInMs != -1) {
                return asyncResult.get(maxWaitUntilReturningInMs, TimeUnit.MILLISECONDS);
            } else {
                return asyncResult.get();
            }
        } catch (TimeoutException e) {
            if (isAlive()) {
                return EXITVALUE_STILL_RUNNING;
            } else {
                return EXITVALUE_DESTROYED;
            }
        } catch (InterruptedException e) {
            throw handleInterruptedException(e);
        } catch (Exception e) {
            logger.error(getProcLongName() + " failed", e);
            throw new ManagedProcessException(
                    getProcLongName() + " failed with Exception: " + getLastConsoleLines(), e);
        }
    }

    /**
     * Like {@link #waitForExit()}, but waits max. maxWaitUntilReturning, then destroys if still
     * running, and returns.
     *
     * @param maxWaitUntilDestroyTimeout Time to wait
     * @throws ManagedProcessException see above
     */
    @Override
    @CanIgnoreReturnValue
    public ManagedProcess waitForExitMaxMsOrDestroy(long maxWaitUntilDestroyTimeout)
            throws ManagedProcessException {
        waitForExitMaxMs(maxWaitUntilDestroyTimeout);
        if (isAlive()) {
            logger.info(
                    "Process didn't exit within max. {}ms, so going to destroy it now: {}",
                    maxWaitUntilDestroyTimeout,
                    getProcLongName());
            destroy();
        }
        return this;
    }

    protected void assertWaitForIsValid() throws ManagedProcessException {
        if (!watchDog.isStopped() && !isAlive() && !asyncResult.isDone()) {
            throw new ManagedProcessException(
                    "Asked to waitFor "
                            + getProcLongName()
                            + ", but it was never even start()'ed!");
        }
    }

    @Override
    public boolean watchDogKilledProcess() {
        return watchDog.killedProcess();
    }

    // ---

    @Override
    public String getConsole() {
        if (console != null) {
            return console.getRecentLines();
        } else {
            return "";
        }
    }

    @Override
    public String getLastConsoleLines() {
        return ", last " + consoleBufferMaxLines + " lines of console:\n" + getConsole();
    }

    // ---

    private String getProcShortName() {
        // could later be extended to some sort of fake numeric PID, e.g. "mysqld-1", from a static
        // Map<String execName, Integer id)
        if (procShortName == null) {
            File exec = getExecutableFile();
            procShortName = exec.getName();
        }
        return procShortName;
    }

    @Override
    public String getProcLongName() {
        return "Program "
                + commandLine.toString()
                + (executor.getWorkingDirectory() == null
                        ? ""
                        : " (in working directory "
                                + executor.getWorkingDirectory().getAbsolutePath()
                                + ")");
    }
}
