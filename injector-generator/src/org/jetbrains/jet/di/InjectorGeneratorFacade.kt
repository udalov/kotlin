/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.di

public fun generator(
        targetSourceRoot: String,
        injectorPackageName: String,
        injectorClassName: String,
        generatedBy: String,
        body: DependencyInjectorGenerator.() -> Unit
): DependencyInjectorGenerator {
    val generator = DependencyInjectorGenerator()
    generator.configure(targetSourceRoot, injectorPackageName, injectorClassName, generatedBy)
    generator.body()
    return generator
}

inline public fun <reified T> DependencyInjectorGenerator.field(
        name: String = defaultName(javaClass<T>()),
        init: Expression? = null,
        useAsContext: Boolean = false
) {
    addField(false, DiType(javaClass<T>()), name, init, useAsContext)
}

inline public fun <reified T> DependencyInjectorGenerator.publicField(
        name: String = defaultName(javaClass<T>()),
        init: Expression? = null,
        useAsContext: Boolean = false
) {
    addField(true, DiType(javaClass<T>()), name, init, useAsContext)
}

inline public fun <reified T> DependencyInjectorGenerator.parameter(
        name: String = defaultName(javaClass<T>()),
        useAsContext: Boolean = false
) {
    addParameter(false, DiType(javaClass<T>()), name, true, useAsContext)
}

inline public fun <reified T> DependencyInjectorGenerator.publicParameter(
        name: String = defaultName(javaClass<T>()),
        useAsContext: Boolean = false
) {
    addParameter(true, DiType(javaClass<T>()), name, true, useAsContext)
}

public fun defaultName(entityType: Class<*>): String = InjectorGeneratorUtil.`var`(DiType(entityType))