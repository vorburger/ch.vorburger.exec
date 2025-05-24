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

import org.apache.commons.exec.DefaultExecutor;

import java.util.function.Function;

@SuppressWarnings("deprecation") // TODO https://github.com/vorburger/ch.vorburger.exec/issues/189
class ExtendedDefaultExecutor extends DefaultExecutor {

    private Function<Integer, Boolean> exitValueChecker;

    void setIsSuccessExitValueChecker(Function<Integer, Boolean> exitValueChecker) {
        this.exitValueChecker = exitValueChecker;
    }

    @Override
    public boolean isFailure(int exitValue) {
        if (exitValueChecker == null) {
            return super.isFailure(exitValue);
        } else {
            return !exitValueChecker.apply(exitValue);
        }
    }
}
