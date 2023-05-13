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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CompositeExecuteResultHandler extends AtomicExecuteResultHandler {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<? extends ExecuteResultHandler> handlers;
    private final ManagedProcessState managedProcessState;

    public CompositeExecuteResultHandler(ManagedProcessState managedProcessState,
            List<? extends ExecuteResultHandler> handlers) {
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
                logger.error(managedProcessState.getProcLongName() + " process handler failed on processComplete", e);
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
                logger.error(managedProcessState.getProcLongName() + " process handler failed on processComplete", e);
            }
        }
    }
}
