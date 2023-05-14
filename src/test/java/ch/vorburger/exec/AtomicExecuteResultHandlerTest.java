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

import static java.time.Duration.ofMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.apache.commons.exec.ExecuteException;
import org.junit.Test;

public class AtomicExecuteResultHandlerTest {

    @Test
    public void unset() throws InterruptedException {
        AtomicExecuteResultHandler h = new AtomicExecuteResultHandler();
        h.waitFor(ofMillis(50));
        assertFalse(h.getExitValue().isPresent());
        assertFalse(h.getException().isPresent());
    }

    @Test
    public void completion() throws InterruptedException {
        AtomicExecuteResultHandler h = new AtomicExecuteResultHandler();
        h.onProcessComplete(42);
        h.waitFor(ofMillis(50));
        assertEquals(Integer.valueOf(42), h.getExitValue().get());
        assertFalse(h.getException().isPresent());
    }

    @Test
    public void failure() throws InterruptedException {
        AtomicExecuteResultHandler h = new AtomicExecuteResultHandler();
        h.onProcessFailed(new ExecuteException("shit happens", 123));
        h.waitFor(ofMillis(50));
        assertEquals("shit happens (Exit value: 123)", h.getException().get().getMessage());
        assertFalse(h.getExitValue().isPresent());
    }
}
