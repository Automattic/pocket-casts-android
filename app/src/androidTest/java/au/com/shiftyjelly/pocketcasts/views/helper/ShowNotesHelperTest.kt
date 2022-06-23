package au.com.shiftyjelly.pocketcasts.views.helper

import org.junit.Assert.assertEquals
import org.junit.Test

class ShowNotesHelperTest {

    @Test
    fun convertTimesToLinks() {
        var actual = ShowNotesHelper.convertTimesToLinks("<p><strong>57:00</strong> - PDF Assets on iOS</p>")
        var expected = "<p><strong><a href=\"http://localhost/#playerJumpTo=57:00\">57:00</a></strong> - PDF Assets on iOS</p>"
        assertEquals(actual, expected)

        actual = ShowNotesHelper.convertTimesToLinks("<p><strong>6:20</strong> - How do you see the trend of designers moving from small agencies to product companies?</p>")
        expected = "<p><strong><a href=\"http://localhost/#playerJumpTo=6:20\">6:20</a></strong> - How do you see the trend of designers moving from small agencies to product companies?</p>"
        assertEquals(actual, expected)

        actual = ShowNotesHelper.convertTimesToLinks("<p><strong>10:00</strong> - Getting work and creating products as an agency</p>")
        expected = "<p><strong><a href=\"http://localhost/#playerJumpTo=10:00\">10:00</a></strong> - Getting work and creating products as an agency</p>"
        assertEquals(actual, expected)

        actual = ShowNotesHelper.convertTimesToLinks("Grizzlies win over the Timberwolves (8:52), from Anthony Davis (25:32), and the Lakers' new life without Kobe (35:35).")
        expected = "Grizzlies win over the Timberwolves (<a href=\"http://localhost/#playerJumpTo=8:52\">8:52</a>), from Anthony Davis (<a href=\"http://localhost/#playerJumpTo=25:32\">25:32</a>), and the Lakers' new life without Kobe (<a href=\"http://localhost/#playerJumpTo=35:35\">35:35</a>)."
        assertEquals(actual, expected)

        actual = ShowNotesHelper.convertTimesToLinks("Grizzlies win over the Timberwolves (8:52, from Anthony Davis 25:32), and the Lakers' new life without Kobe (35:35).")
        expected = "Grizzlies win over the Timberwolves (<a href=\"http://localhost/#playerJumpTo=8:52\">8:52</a>, from Anthony Davis <a href=\"http://localhost/#playerJumpTo=25:32\">25:32</a>), and the Lakers' new life without Kobe (<a href=\"http://localhost/#playerJumpTo=35:35\">35:35</a>)."
        assertEquals(actual, expected)

        // Accidental Tech Podcast - https://nodeweb.pocketcasts.com/admin/podcasts/180561
        actual = ShowNotesHelper.convertTimesToLinks("<li><a href=\"https://overcast.fm/+BtuyYAAIQ/16:45\">Confirmation of John's prediction about face swipe timing</a></li>")
        expected = "<li><a href=\"https://overcast.fm/+BtuyYAAIQ/16:45\">Confirmation of John's prediction about face swipe timing</a></li>"
        assertEquals(actual, expected)
    }
}
