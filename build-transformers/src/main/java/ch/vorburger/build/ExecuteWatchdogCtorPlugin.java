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
package ch.vorburger.build;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatchers;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.concurrent.ThreadFactory;

public final class ExecuteWatchdogCtorPlugin implements Plugin {

    @Override
    public boolean matches(TypeDescription target) {
        return target.getName().equals("org.apache.commons.exec.ExecuteWatchdog");
    }

    @Override
    public DynamicType.@NonNull Builder<?> apply(DynamicType.@NonNull Builder<?> builder,
                                        @NonNull TypeDescription type,
                                        @NonNull ClassFileLocator cfl) {
        try {
            TypeDescription threadFactory =
                    TypeDescription.ForLoadedType.of(ThreadFactory.class);      // JDK OK to load
            TypeDescription duration =
                    TypeDescription.ForLoadedType.of(Duration.class);          // JDK OK to load

            // Find existing private ctor: (ThreadFactory, Duration)
            MethodDescription.InDefinedShape superCtor =
                    type.getDeclaredMethods()
                            .filter(ElementMatchers.isConstructor()
                                    .and(ElementMatchers.takesArguments(threadFactory, duration)))
                            .getOnly();

            // Define new protected ctor: (Duration, ThreadFactory)
            // Re-map to super order: (ThreadFactory, Duration) => arguments 1, 0
            return builder
                    .defineConstructor(Visibility.PROTECTED)
                    .withParameter(Duration.class, "timeout")
                    .withParameter(ThreadFactory.class, "threadFactory")
                    .intercept(MethodCall.invoke(superCtor)
                            .withArgument(1)  // ThreadFactory
                            .withArgument(0)  // Duration
                    );

        } catch (Exception e) {
            throw new IllegalStateException("Failed to add protected constructor to ExecuteWatchdog", e);
        }
    }

    @Override
    public void close() { /* no-op */ }
}
