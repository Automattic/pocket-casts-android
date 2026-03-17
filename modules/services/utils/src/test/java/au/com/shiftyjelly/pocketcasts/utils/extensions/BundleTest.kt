package au.com.shiftyjelly.pocketcasts.utils.extensions

import android.os.Bundle
import android.os.ParcelUuid
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class BundleTest {
    @Test
    fun `find boolean`() {
        val bundle = Bundle().apply {
            putBoolean("key1", true)
            putBoolean("key2", false)
        }

        assertEquals(true, bundle.findBoolean("key1"))
        assertEquals(false, bundle.findBoolean("key2"))
        assertEquals(null, bundle.findBoolean("key3"))
    }

    @Test
    fun `require boolean`() {
        val bundle = Bundle().apply {
            putBoolean("key1", true)
            putBoolean("key2", false)
        }

        assertEquals(true, bundle.requireBoolean("key1"))
        assertEquals(false, bundle.requireBoolean("key2"))
        assertThrows("Missing value for key \"key3\"", IllegalArgumentException::class.java) {
            bundle.requireBoolean("key3")
        }
    }

    @Test
    fun `find byte`() {
        val bundle = Bundle().apply {
            putByte("key1", 25.toByte())
        }

        assertEquals(25.toByte(), bundle.findByte("key1"))
        assertEquals(null, bundle.findByte("key2"))
    }

    @Test
    fun `require byte`() {
        val bundle = Bundle().apply {
            putByte("key1", 25.toByte())
        }

        assertEquals(25.toByte(), bundle.requireByte("key1"))
        assertThrows("Missing value for key \"key2\"", IllegalArgumentException::class.java) {
            bundle.requireByte("key2")
        }
    }

    @Test
    fun `find char`() {
        val bundle = Bundle().apply {
            putChar("key1", 'X')
        }

        assertEquals('X', bundle.findChar("key1"))
        assertEquals(null, bundle.findChar("key2"))
    }

    @Test
    fun `require char`() {
        val bundle = Bundle().apply {
            putChar("key1", 'X')
        }

        assertEquals('X', bundle.requireChar("key1"))
        assertThrows("Missing value for key \"key2\"", IllegalArgumentException::class.java) {
            bundle.requireChar("key2")
        }
    }

    @Test
    fun `find double`() {
        val bundle = Bundle().apply {
            putDouble("key1", 12.5)
        }

        assertEquals(12.5, bundle.findDouble("key1"))
        assertEquals(null, bundle.findDouble("key2"))
    }

    @Test
    fun `require double`() {
        val bundle = Bundle().apply {
            putDouble("key1", 12.5)
        }

        assertEquals(12.5, bundle.requireDouble("key1"), 0.0)
        assertThrows("Missing value for key \"key2\"", IllegalArgumentException::class.java) {
            bundle.requireDouble("key2")
        }
    }

    @Test
    fun `find float`() {
        val bundle = Bundle().apply {
            putFloat("key1", 55.33f)
        }

        assertEquals(55.33f, bundle.findFloat("key1"))
        assertEquals(null, bundle.findFloat("key2"))
    }

    @Test
    fun `require float`() {
        val bundle = Bundle().apply {
            putFloat("key1", 55.33f)
        }

        assertEquals(55.33f, bundle.requireFloat("key1"), 0.0f)
        assertThrows("Missing value for key \"key2\"", IllegalArgumentException::class.java) {
            bundle.requireFloat("key2")
        }
    }

    @Test
    fun `find int`() {
        val bundle = Bundle().apply {
            putInt("key1", 8)
        }

        assertEquals(8, bundle.findInt("key1"))
        assertEquals(null, bundle.findInt("key2"))
    }

    @Test
    fun `require int`() {
        val bundle = Bundle().apply {
            putInt("key1", 8)
        }

        assertEquals(8, bundle.requireInt("key1"))
        assertThrows("Missing value for key \"key2\"", IllegalArgumentException::class.java) {
            bundle.requireInt("key2")
        }
    }

    @Test
    fun `find long`() {
        val bundle = Bundle().apply {
            putLong("key1", 8L)
        }

        assertEquals(8L, bundle.findLong("key1"))
        assertEquals(null, bundle.findLong("key2"))
    }

    @Test
    fun `require long`() {
        val bundle = Bundle().apply {
            putLong("key1", 8L)
        }

        assertEquals(8L, bundle.requireLong("key1"))
        assertThrows("Missing value for key \"key2\"", IllegalArgumentException::class.java) {
            bundle.requireLong("key2")
        }
    }

    @Test
    fun `find short`() {
        val bundle = Bundle().apply {
            putShort("key1", 8.toShort())
        }

        assertEquals(8.toShort(), bundle.findShort("key1"))
        assertEquals(null, bundle.findShort("key2"))
    }

    @Test
    fun `require short`() {
        val bundle = Bundle().apply {
            putShort("key1", 8.toShort())
        }

        assertEquals(8.toShort(), bundle.requireShort("key1"))
        assertThrows("Missing value for key \"key2\"", IllegalArgumentException::class.java) {
            bundle.requireShort("key2")
        }
    }

    @Test
    fun `find string`() {
        val bundle = Bundle().apply {
            putString("key1", "abc")
        }

        assertEquals("abc", bundle.findString("key1"))
        assertEquals(null, bundle.findString("key2"))
    }

    @Test
    fun `require string`() {
        val bundle = Bundle().apply {
            putString("key1", "abc")
        }

        assertEquals("abc", bundle.requireString("key1"))
        assertThrows("Missing value for key \"key2\"", IllegalArgumentException::class.java) {
            bundle.requireString("key2")
        }
    }

    @Test
    fun `find bundle`() {
        val bundle = Bundle().apply {
            putBundle(
                "key1",
                Bundle().apply {
                    putString("a", "b")
                },
            )
        }

        assertBundleEquals(
            Bundle().apply {
                putString("a", "b")
            },
            bundle.findBundle("key1"),
        )
        assertEquals(null, bundle.findBundle("key2"))
    }

    @Test
    fun `require bundle`() {
        val bundle = Bundle().apply {
            putBundle(
                "key1",
                Bundle().apply {
                    putString("a", "b")
                },
            )
        }

        assertBundleEquals(
            Bundle().apply {
                putString("a", "b")
            },
            bundle.requireBundle("key1"),
        )
        assertThrows("Missing value for key \"key2\"", IllegalArgumentException::class.java) {
            bundle.requireBundle("key2")
        }
    }

    @Test
    fun `find serializable`() {
        val uuid = UUID.randomUUID()
        val bundle = Bundle().apply {
            putSerializable("key1", uuid)
        }

        assertEquals(uuid, bundle.findSerializable<UUID>("key1"))
        assertEquals(null, bundle.findSerializable<UUID>("key2"))
    }

    @Test
    fun `require serializable`() {
        val uuid = UUID.randomUUID()
        val bundle = Bundle().apply {
            putSerializable("key1", uuid)
        }

        assertEquals(uuid, bundle.requireSerializable<UUID>("key1"))
        assertThrows("Missing value for key \"key2\"", IllegalArgumentException::class.java) {
            bundle.requireSerializable<UUID>("key2")
        }
    }

    @Test
    fun `find parcelable`() {
        val uuid = ParcelUuid(UUID.randomUUID())
        val bundle = Bundle().apply {
            putParcelable("key1", uuid)
        }

        assertEquals(uuid, bundle.findParcelable<ParcelUuid>("key1"))
        assertEquals(null, bundle.findParcelable<ParcelUuid>("key2"))
    }

    @Test
    fun `require parcelable`() {
        val uuid = ParcelUuid(UUID.randomUUID())
        val bundle = Bundle().apply {
            putParcelable("key1", uuid)
        }

        assertEquals(uuid, bundle.requireParcelable<ParcelUuid>("key1"))
        assertThrows("Missing value for key \"key2\"", IllegalArgumentException::class.java) {
            bundle.requireParcelable<ParcelUuid>("key2")
        }
    }

    @Test
    fun `find parcelable list`() {
        val uuids = ArrayList<ParcelUuid>().apply {
            add(ParcelUuid(UUID.randomUUID()))
            add(ParcelUuid(UUID.randomUUID()))
        }
        val bundle = Bundle().apply {
            putParcelableArrayList("key1", uuids)
        }

        assertEquals(uuids, bundle.findParcelableList<ParcelUuid>("key1"))
        assertEquals(null, bundle.findParcelableList<ParcelUuid>("key2"))
    }

    @Test
    fun `require parcelable list`() {
        val uuids = ArrayList<ParcelUuid>().apply {
            add(ParcelUuid(UUID.randomUUID()))
            add(ParcelUuid(UUID.randomUUID()))
        }
        val bundle = Bundle().apply {
            putParcelableArrayList("key1", uuids)
        }

        assertEquals(uuids, bundle.requireParcelableList<ParcelUuid>("key1"))
        assertThrows("Missing value for key \"key2\"", IllegalArgumentException::class.java) {
            bundle.requireParcelableList<ParcelUuid>("key2")
        }
    }
}

@Suppress("DEPRECATION")
private fun assertBundleEquals(expected: Bundle?, actual: Bundle?) {
    assertEquals(expected?.keySet(), actual?.keySet())
    for (key in expected?.keySet().orEmpty()) {
        val bundle1 = expected?.get(key)
        val bundle2 = actual?.get(key)
        if (bundle1 is Bundle && bundle2 is Bundle) {
            assertBundleEquals(bundle1, bundle2)
        } else {
            assertEquals(bundle1, bundle2)
        }
    }
}
