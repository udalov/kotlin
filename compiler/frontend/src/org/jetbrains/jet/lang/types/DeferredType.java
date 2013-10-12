/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.types;

import jet.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.storage.LockBasedStorageManager;
import org.jetbrains.jet.storage.NotNullLazyValue;
import org.jetbrains.jet.util.Box;

import static org.jetbrains.jet.lang.resolve.BindingContext.DEFERRED_TYPE;

public class DeferredType extends DeferredTypeBase {
    @NotNull
    public static DeferredType create(@NotNull BindingTrace trace, @NotNull NotNullLazyValue<JetType> lazyValue) {
        DeferredType deferredType = new DeferredType(lazyValue);
        trace.record(DEFERRED_TYPE, new Box<DeferredType>(deferredType));
        return deferredType;
    }

    @NotNull
    public static DeferredType create(@NotNull BindingTrace trace, @NotNull Function0<JetType> compute) {
        return create(trace, LockBasedStorageManager.NO_LOCKS.createLazyValue(compute));
    }

    private DeferredType(@NotNull NotNullLazyValue<JetType> lazyValue) {
        super(lazyValue);
    }
}
