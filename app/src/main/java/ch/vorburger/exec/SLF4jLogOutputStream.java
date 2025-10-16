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

import org.apache.commons.exec.LogOutputStream;
import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * OutputStream which logs to SLF4j.
 *
 * <p>With many thanks to <a
 * href="https://stackoverflow.com/questions/5499042/writing-output-error-to-log-files-using">PumpStreamHandler</a>
 *
 * @author Michael Vorburger
 */
// intentionally package local
@SuppressWarnings("MemberName") // https://errorprone.info/bugpattern/IdentifierName
class SLF4jLogOutputStream extends LogOutputStream {

    private final OutputStreamLogDispatcher dispatcher;
    private final Logger logger;
    private final OutputStreamType type;
    private final String pid;

    protected SLF4jLogOutputStream(
            Logger logger,
            String pid,
            OutputStreamType type,
            OutputStreamLogDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        this.logger = logger;
        this.type = type;
        this.pid = pid;
    }

    @Override
    protected void processLine(String line, @SuppressWarnings("unused") int level) {
        Level logLevel = dispatcher.dispatch(type, line);
        if (logLevel == null) {
            return;
        }
        switch (logLevel) {
            case TRACE -> logger.trace("{}: {}", pid, line);
            case DEBUG -> logger.debug("{}: {}", pid, line);
            case INFO -> logger.info("{}: {}", pid, line);
            case WARN -> logger.warn("{}: {}", pid, line);
            case ERROR -> logger.error("{}: {}", pid, line);
        }
    }
}
