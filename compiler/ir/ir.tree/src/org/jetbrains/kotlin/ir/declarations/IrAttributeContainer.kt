/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

@Suppress("unused") // T is unused
class IrAttributeKey<T : Any>(val name: String)

interface IrAttributeContainer {
    val attributes: Map<IrAttributeKey<*>, Any>?
}

interface IrMutableAttributeContainer : IrAttributeContainer {
    override var attributes: MutableMap<IrAttributeKey<*>, Any>?
}

fun <T : Any> IrAttributeContainer.getAttribute(key: IrAttributeKey<T>): T? =
    @Suppress("UNCHECKED_CAST")
    (attributes?.get(key) as T?)

fun <T : Any> IrMutableAttributeContainer.setAttribute(key: IrAttributeKey<T>, value: T) {
    val data = attributes ?: mutableMapOf<IrAttributeKey<*>, Any>().also { attributes = it }
    data[key] = value
}

fun IrMutableAttributeContainer.copyAttributesFrom(source: IrAttributeContainer) {
    for ((key, value) in source.attributes ?: return) {
        @Suppress("UNCHECKED_CAST")
        setAttribute(key as IrAttributeKey<Any>, value)
    }
}
