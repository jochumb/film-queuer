package me.jochum.filmqueuer.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DepartmentTest {

    @Test
    fun `fromString should return correct department for known values`() {
        assertEquals(Department.ACTING, Department.fromString("acting"))
        assertEquals(Department.ACTING, Department.fromString("Acting"))
        assertEquals(Department.ACTING, Department.fromString("ACTING"))
        
        assertEquals(Department.DIRECTING, Department.fromString("directing"))
        assertEquals(Department.DIRECTING, Department.fromString("Directing"))
        assertEquals(Department.DIRECTING, Department.fromString("DIRECTING"))
        
        assertEquals(Department.WRITING, Department.fromString("writing"))
        assertEquals(Department.WRITING, Department.fromString("Writing"))
        assertEquals(Department.WRITING, Department.fromString("WRITING"))
    }
    
    @Test
    fun `fromString should return OTHER for unknown values`() {
        assertEquals(Department.OTHER, Department.fromString("production"))
        assertEquals(Department.OTHER, Department.fromString("cinematography"))
        assertEquals(Department.OTHER, Department.fromString("sound"))
        assertEquals(Department.OTHER, Department.fromString("unknown"))
        assertEquals(Department.OTHER, Department.fromString(""))
        assertEquals(Department.OTHER, Department.fromString(null))
    }
    
    @Test
    fun `fromString should handle various casing`() {
        assertEquals(Department.ACTING, Department.fromString("AcTiNg"))
        assertEquals(Department.DIRECTING, Department.fromString("DiReCtInG"))
        assertEquals(Department.WRITING, Department.fromString("WrItInG"))
    }
}