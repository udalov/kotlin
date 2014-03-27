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

package org.jetbrains.jet.lang.resolve.objc;

import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.DelegatingType;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.storage.LockBasedStorageManager;
import org.jetbrains.jet.storage.NotNullLazyValue;
import org.jetbrains.jet.util.ReenteringLazyValueComputationException;

public class ObjCDeferredType extends DelegatingType {
    private final NotNullLazyValue<JetType> lazyValue;

    public ObjCDeferredType(@NotNull Function0<JetType> lazyType) {
        this.lazyValue = LockBasedStorageManager.NO_LOCKS.createLazyValue(lazyType);
    }

    @Override
    protected JetType getDelegate() {
        return lazyValue.invoke();
    }

    @Override
    public String toString() {
        try {
            if (lazyValue.isComputed()) {
                return getDelegate().toString();
            }
            else {
                return "<Not computed yet>";
            }
        }
        catch (ReenteringLazyValueComputationException e) {
            return "<Failed to compute this type>";
        }
    }
}
