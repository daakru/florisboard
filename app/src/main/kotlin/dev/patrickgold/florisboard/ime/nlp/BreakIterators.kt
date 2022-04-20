/*
 * Copyright (C) 2022 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.nlp

import android.icu.text.BreakIterator
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.kotlin.GuardedByLock
import dev.patrickgold.florisboard.lib.kotlin.guardedByLock
import io.github.reactivecircus.cache4k.Cache

object BreakIterators {
    private val wordInstances = Cache.Builder().build<FlorisLocale, GuardedByLock<MutableList<CacheEntry>>>()

    suspend fun <R> withWordInstance(locale: FlorisLocale, action: (BreakIterator) -> R): R {
        val localeSpecificInstances = wordInstances.get(locale) { guardedByLock { mutableListOf() } }
        val cacheEntry: CacheEntry
        localeSpecificInstances.withLock { instances ->
            val entry = instances.firstOrNull { !it.isUsed }
            if (entry != null) {
                cacheEntry = entry.also { it.isUsed = true }
            } else {
                cacheEntry = CacheEntry(BreakIterator.getWordInstance(locale.base), isUsed = true)
                instances.add(cacheEntry)
            }
        }
        val ret = action(cacheEntry.instance)
        cacheEntry.isUsed = false
        return ret
    }

    class CacheEntry(val instance: BreakIterator, var isUsed: Boolean = false)
}
