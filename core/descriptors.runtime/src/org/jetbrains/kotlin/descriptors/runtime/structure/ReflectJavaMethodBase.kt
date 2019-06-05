/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.runtime.structure

import org.jetbrains.kotlin.load.java.structure.JavaMethodBase
import org.jetbrains.kotlin.name.ClassId
import java.lang.reflect.Member

abstract class ReflectJavaMethodBase<M : Member>(override val member: M) : ReflectJavaMember(), JavaMethodBase {
    override val thrownExceptions: List<ClassId>
        get() {
            // @Throws annotations that are constructed from this data are useless in reflection (Throws has SOURCE retention),
            // so we don't bother extracting the list of thrown exception types here
            return emptyList()
        }
}
