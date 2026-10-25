package org.comichao.netresult

import org.comichao.netresult.data.AppDatabase
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards against shipping a schema change without its exported schema (needed to write/test migrations). */
class SchemaTest {
    @Test fun schemaForCurrentVersionIsExported() {
        val f = File("schemas/org.comichao.netresult.data.AppDatabase/${AppDatabase.VERSION}.json")
        assertTrue("Missing ${f.path}: build once and keep the generated schema file", f.exists())
    }
}
