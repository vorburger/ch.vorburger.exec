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

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link ExecuteResultHandler} which uses a single
 * {@link java.util.concurrent.atomic.AtomicReference}
 * instead of three separate volatile fields like the
 * original {@link org.apache.commons.exec.DefaultExecuteResultHandler} did.
 *
 * @see <a href="https://github.com/vorburger/ch.vorburger.exec/issues/108">Issue #108</a>
 */
public class AtomicExecuteResultHandler implements ExecuteResultHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AtomicExecuteResultHandler.class);

    // When reading this code, do not confuse an
    // org.apache.commons.exec.ExecuteException and (similarly named)
    // an java.util.concurrent.ExecutionException

    private final CompletableFuture<Integer> holder = new CompletableFuture<>();

    private void logOnAlreadySet(String methodName, @Nullable ExecuteException e) {
        String errorString = methodName + "  will throw IllegalStateException, already set: " + holder;
        LOG.error(errorString);
        throw new IllegalStateException(errorString, e);
    }

    @Override
    public void onProcessComplete(int exitValue) {
        if (!holder.complete(exitValue)) {
            logOnAlreadySet("onProcessComplete(" + exitValue + ")", null);
        }
    }

    @Override
    public void onProcessFailed(ExecuteException e) {
        if (!holder.completeExceptionally(e)) {
            logOnAlreadySet("onProcessFailed(" + e + ")", e);
        }
    }

    public Optional<Integer> getExitValue() {
        try {
            return Optional.ofNullable(holder.getNow(null));
        } catch (CompletionException e) {
            // This is thrown when there is no exit value, yet; so:
            return Optional.empty();
        }
    }

    public Optional<Exception> getException() {
        try {
            holder.getNow(null);
            return Optional.empty();
        } catch (CompletionException e) {
            // This is thrown when there is no exit value, yet; so:
            Throwable inner = e.getCause();
            if (inner instanceof ExecuteException) {
                return Optional.of((ExecuteException) inner);
            } else {
                // This should never happen, because we only ever set ExecuteException
                throw new IllegalStateException("BUG", inner);
            }
        }
    }

    public void waitFor() throws InterruptedException {
        try {
            holder.get();
        } catch (InterruptedException e) {
            throw e;
        } catch (ExecutionException e) {
            // see below
        }
    }

    public void waitFor(Duration timeout) throws InterruptedException {
        long timeoutNanos = timeout.toNanos();
        try {
            holder.get(timeoutNanos, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw e;
        } catch (ExecutionException | TimeoutException e) {
            // Swallow any java.util.concurrent.ExecutionException
            // Caused by: org.apache.commons.exec.ExecuteException
            // (which we get here after an onProcessFailed()),
            // or any java.util.concurrent.TimeoutException; but
            // do NOT catch java.util.concurrent.CompletionException.
        }
    }
}
