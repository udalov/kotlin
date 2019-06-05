/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.runtime.structure

import org.jetbrains.kotlin.load.java.structure.JavaMethodBase
import java.lang.reflect.Member

abstract class ReflectJavaMethodBase<M : Member>(override val member: M) : ReflectJavaMember(), JavaMethodBase
