package com.ascend.app.data.cloud

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudClientTest {

    @Test
    fun `hunter keys are long enough for the server to accept`() {
        // The server rejects anything under 20 characters as brute-forceable.
        repeat(20) {
            assertTrue(CloudClient.generateHunterKey().length >= 20)
        }
    }

    @Test
    fun `hunter keys do not repeat`() {
        val keys = (1..200).map { CloudClient.generateHunterKey() }.toSet()
        assertEquals("generated keys collided", 200, keys.size)
    }

    @Test
    fun `hunter keys avoid look-alike characters`() {
        // Keys get written down, so no two symbols may look alike. Crockford
        // base32 drops I, L, O and U; 0 and 1 are then unambiguous because
        // the letters they resemble are gone.
        val forbidden = setOf('I', 'L', 'O', 'U')
        repeat(50) {
            val key = CloudClient.generateHunterKey().replace("-", "")
            assertTrue(
                "key contained an ambiguous character: $key",
                key.none { it in forbidden },
            )
        }
    }

    @Test
    fun `the key alphabet is a power of two so keys are not biased`() {
        // Reducing a random byte modulo the alphabet length is only uniform
        // when the length divides 256. A 31-symbol set would over-represent
        // its first symbols and cost real entropy in the only credential
        // protecting the backup.
        val alphabet = (1..4000).flatMap { CloudClient.generateHunterKey().replace("-", "").toList() }
            .toSet()
        assertEquals(32, alphabet.size)
    }

    @Test
    fun `base urls are normalised into an origin`() {
        assertEquals("https://x.vercel.app", CloudClient.normaliseBaseUrl("https://x.vercel.app"))
        assertEquals("https://x.vercel.app", CloudClient.normaliseBaseUrl("https://x.vercel.app/"))
        assertEquals("https://x.vercel.app", CloudClient.normaliseBaseUrl("  x.vercel.app  "))
        // Pasting the API path in is the obvious user error; absorb it rather
        // than producing /api/api/sync and a baffling 404.
        assertEquals("https://x.vercel.app", CloudClient.normaliseBaseUrl("https://x.vercel.app/api"))
    }

    @Test
    fun `an empty url stays empty rather than becoming a bare scheme`() {
        assertEquals("", CloudClient.normaliseBaseUrl(""))
        assertEquals("", CloudClient.normaliseBaseUrl("   "))
    }

    @Test
    fun `http is preserved so a local test server still works`() {
        assertEquals("http://10.0.2.2:3000", CloudClient.normaliseBaseUrl("http://10.0.2.2:3000"))
    }
}

class CloudInsightsTest {

    @Test
    fun `no data yields no lines`() {
        assertTrue(CloudInsights.fromReport(JSONObject("""{"hasData":false}""")).isEmpty())
    }

    @Test
    fun `a malformed report does not throw`() {
        // The server is a moving target; a prompt builder must never be the
        // thing that crashes because a field changed shape.
        assertTrue(CloudInsights.fromReport(JSONObject("{}")).isEmpty())
        val partial = JSONObject("""{"hasData":true,"windows":[{"window":"7d"}]}""")
        CloudInsights.fromReport(partial)
    }

    @Test
    fun `findings are rendered as readable facts`() {
        val report = JSONObject(
            """
            {
              "hasData": true,
              "coverage": {"firstDay":"2026-01-01","lastDay":"2026-08-08","daysRecorded":180},
              "windows": [{"window":"30d","nonNegotiableRate":72,"perfectDays":9,"focusMinutes":420}],
              "momentum": {"recentRate14":80,"priorRate14":65,"changePoints":15},
              "weekdayPattern": [{"weekday":"Sun","nonNegotiableRate":40}],
              "slippingHabits": [{"habit":"Read 20 minutes","priorRate14":90,"recentRate14":50}],
              "longestPerfectRuns": [{"lengthDays":12,"ended":"2026-05-04"}]
            }
            """.trimIndent(),
        )
        val lines = CloudInsights.fromReport(report)
        val text = lines.joinToString("\n")

        assertTrue("coverage missing", text.contains("180 recorded days"))
        assertTrue("adherence missing", text.contains("72%"))
        assertTrue("momentum direction missing", text.contains("up 15 points"))
        assertTrue("weekday breakdown missing", text.contains("Sun 40%"))
        assertTrue("slipping habit missing", text.contains("Read 20 minutes"))
        assertTrue("perfect run missing", text.contains("12 days"))
    }

    @Test
    fun `momentum reads correctly when it is falling`() {
        val report = JSONObject(
            """{"hasData":true,"momentum":{"recentRate14":50,"priorRate14":70,"changePoints":-20}}""",
        )
        val text = CloudInsights.fromReport(report).joinToString("\n")
        assertTrue(text.contains("down 20 points"))
        assertFalse("a fall must not be described as a rise", text.contains("up 20"))
    }

    @Test
    fun `insights state facts rather than passing judgement`() {
        // The model does the interpreting. If this file ever starts emitting
        // verdicts, they will be wrong the first week the pattern changes.
        val report = JSONObject(
            """
            {"hasData":true,
             "weekdayPattern":[{"weekday":"Sun","nonNegotiableRate":10}],
             "slippingHabits":[{"habit":"Gym","priorRate14":95,"recentRate14":20}]}
            """.trimIndent(),
        )
        val text = CloudInsights.fromReport(report).joinToString("\n").lowercase()
        listOf("you always", "you never", "you should", "bad", "lazy").forEach { verdict ->
            assertFalse("insight editorialised with '$verdict'", text.contains(verdict))
        }
    }

    @Test
    fun `per-habit lines are capped so they cannot crowd out the question`() {
        val slipping = (1..30).joinToString(",") {
            """{"habit":"H$it","priorRate14":90,"recentRate14":10}"""
        }
        val report = JSONObject("""{"hasData":true,"slippingHabits":[$slipping]}""")
        val lines = CloudInsights.fromReport(report).filter { it.startsWith("Slipping:") }
        assertTrue("expected the slipping list to be capped", lines.size <= 6)
    }
}
