package io.github.chaitanyakelkar.mdreader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentsCodecTest {
    private val item = RecentFile("content://a/b%20c.md", "b \"c\" é.md", 123L, 4, 5)

    @Test fun `round trips`() {
        assertEquals(listOf(item), RecentsCodec.decode(RecentsCodec.encode(listOf(item))))
    }

    @Test fun `missing or corrupt data decodes to empty`() {
        listOf(null, "", "  ", "not json", "{}", "[{\"uri\":1}]", "[1]").forEach {
            assertTrue("input: $it", RecentsCodec.decode(it).isEmpty())
        }
    }

    @Test fun `opening moves to the front and keeps scroll position`() {
        val other = RecentFile("u2", "two", 2)
        val updated = RecentsCodec.withOpened(listOf(other, item), item.uri, "renamed", 999)
        assertEquals(listOf(item.uri, "u2"), updated.map { it.uri })
        assertEquals(item.copy(name = "renamed", openedAt = 999), updated[0])
    }

    @Test fun `list length is capped`() {
        var list = emptyList<RecentFile>()
        repeat(RecentsCodec.MAX_RECENTS + 5) { list = RecentsCodec.withOpened(list, "u$it", "n", it.toLong()) }
        assertEquals(RecentsCodec.MAX_RECENTS, list.size)
        assertEquals("u${RecentsCodec.MAX_RECENTS + 4}", list[0].uri)
    }
}
