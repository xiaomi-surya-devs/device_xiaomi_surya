/*
 * Copyright (C) 2025 kenway214
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

package com.xiaomi.settings.gamebar

import java.io.File

internal object ThermalZones {

    private val zoneDirs: List<File> by lazy {
        File("/sys/class/thermal").listFiles { file ->
            file.isDirectory && file.name.matches(Regex("thermal_zone\\d+"))
        }?.sortedBy { it.name.removePrefix("thermal_zone").toIntOrNull() ?: Int.MAX_VALUE }
            ?: emptyList()
    }

    fun paths(predicate: (String) -> Boolean): List<String> =
        zoneDirs.mapNotNull { dir ->
            val type = runCatching { File(dir, "type").readText().trim() }.getOrNull()
            if (type != null && predicate(type)) File(dir, "temp").absolutePath else null
        }
}
