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

import static java.util.Objects.requireNonNull;

import java.lang.invoke.MethodHandles;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
