/*
 * Copyright 2022 The Android Open Source Project
 *
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
 */

package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.collection.MutableObjectIntMap
import androidx.collection.ObjectIntMap
import androidx.collection.emptyObjectIntMap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.getDefaultLazyLayoutKey

@OptIn(ExperimentalFoundationApi::class)
class KeyIndexMap(
    nearestRange: IntRange,
    intervalContent: LazyLayoutIntervalContent<*>,
) {
    private val map: ObjectIntMap<Any>
    private val keys: Array<Any?>
    private val keysStartIndex: Int

    constructor(
        index: Int,
        size: Int,
        intervalContent: LazyLayoutIntervalContent<*>,
    ) : this(
        (index - size).coerceAtLeast(0).let { start -> start until start + size },
        intervalContent,
    )

    init {
        // Traverses the interval [list] in order to create a mapping from the key to the index for
        // all the indexes in the passed [range].
        val list = intervalContent.intervals
        val first = nearestRange.first
        check(first >= 0) { "negative nearestRange.first" }
        val last = minOf(nearestRange.last, list.size - 1)
        if (last < first) {
            map = emptyObjectIntMap()
            keys = emptyArray()
            keysStartIndex = 0
        } else {
            val size = last - first + 1
            keys = arrayOfNulls<Any?>(size)
            keysStartIndex = first
            map = MutableObjectIntMap<Any>(size).also { map ->
                list.forEach(
                    fromIndex = first,
                    toIndex = last,
                ) {
                    val keyFactory = it.value.key
                    val start = maxOf(first, it.startIndex)
                    val end = minOf(last, it.startIndex + it.size - 1)
                    for (i in start..end) {
                        val key = keyFactory?.invoke(i - it.startIndex) ?: getDefaultLazyLayoutKey(i)
                        map[key] = i
                        keys[i - keysStartIndex] = key
                    }
                }
            }
        }
    }

    fun getKey(index: Int) = keys.getOrElse(index - keysStartIndex) { null }

    fun getIndex(key: Any) = map.getOrElse(key) { -1 }
}
