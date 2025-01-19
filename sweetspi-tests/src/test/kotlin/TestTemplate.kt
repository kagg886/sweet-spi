/*
 * Copyright (c) 2024-2025 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.sweetspi.tests

import java.nio.file.*
import kotlin.io.path.*

class TestTemplate private constructor(path: String) {
    val templatePath: Path = templates.resolve(path)

    companion object {
        private val templates = Path("src/test/templates")

        val MULTIPLATFORM = TestTemplate("multiplatform")
        val MULTIPLATFORM_MULTIMODULE = TestTemplate("multiplatform-multimodule")
        val JVM = TestTemplate("jvm")
        val ANDROID = TestTemplate("android")
    }
}
