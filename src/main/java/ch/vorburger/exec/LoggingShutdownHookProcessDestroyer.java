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

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.slf4j.Logger;

public class LoggingShutdownHookProcessDestroyer extends ShutdownHookProcessDestroyer {
    private static final Logger LOG = getLogger(LoggingShutdownHookProcessDestroyer.class);

    @Override
    public void run() {
        LOG.info(
                "Shutdown Hook: JVM is about to exit! Going to kill destroyOnShutdown"
                        + " processes...");
        super.run();
    }
}
