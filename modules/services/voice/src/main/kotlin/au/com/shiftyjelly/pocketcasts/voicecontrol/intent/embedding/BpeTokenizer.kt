package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.embedding

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * Tokenizer that reads a HuggingFace [tokenizer.json] file and encodes text
 * into token IDs compatible with multilingual-e5-small (XLM-RoBERTa base).
 *
 * Supports both BPE and Unigram SentencePiece models. XLM-RoBERTa uses
 * the Unigram model with Metaspace pre-tokenization.
 */
@Singleton
class BpeTokenizer @Inject constructor() : TextTokenizer {

    override val clsTokenId = 0 // <s>
    override val sepTokenId = 2 // </s>

    /** Token string → (id, score). Score = log probability for Unigram, unused for BPE. */
    private data class TokenEntry(val id: Int, val score: Float)

    /** Full vocabulary: token → entry. Used for direct lookup in Viterbi. */
    private var vocab: Map<String, TokenEntry> = emptyMap()

    /** Max token length in the vocabulary (optimization for Viterbi). */
    private var maxTokenLen = 0

    /** BPE merge pairs (a, b) → rank. Only used for BPE models. */
    private var merges: MutableMap<Pair<String, String>, Int> = linkedMapOf()

    private var isUnigram = false
    private var isBpe = false
    private var loaded = false

    override fun load(modelPath: String): Boolean {
        return try {
            val json = JSONObject(File(modelPath).readText())
            val model = json.getJSONObject("model")
            val type = model.getString("type")

            when (type) {
                "Unigram" -> parseUnigramVocab(model)

                "BPE" -> {
                    parseBpeVocab(model)
                    parseBpeMerges(model)
                }

                else -> {
                    Timber.e("Unsupported tokenizer type: %s", type)
                    return false
                }
            }

            loaded = true
            Timber.i(
                "Tokenizer loaded: type=%s, %d vocab, maxTokenLen=%d",
                type,
                vocab.size,
                maxTokenLen,
            )
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to load tokenizer")
            false
        }
    }

    // -- Unigram parsing ----------------------------------------------------

    private fun parseUnigramVocab(model: JSONObject) {
        isUnigram = true
        val vocabArr = model.getJSONArray("vocab")
        val map = mutableMapOf<String, TokenEntry>()
        for (i in 0 until vocabArr.length()) {
            val entry = vocabArr.getJSONArray(i)
            val token = entry.getString(0)
            val score = entry.getDouble(1).toFloat()
            map[token] = TokenEntry(i, score)
            if (token.length > maxTokenLen) maxTokenLen = token.length
        }
        vocab = map
    }

    // -- BPE parsing --------------------------------------------------------

    private fun parseBpeVocab(model: JSONObject) {
        isBpe = true
        val vocabObj = model.getJSONObject("vocab")
        val map = mutableMapOf<String, TokenEntry>()
        for (key in vocabObj.keys()) {
            val id = vocabObj.getInt(key)
            map[key] = TokenEntry(id, 0f)
            if (key.length > maxTokenLen) maxTokenLen = key.length
        }
        vocab = map
    }

    private fun parseBpeMerges(model: JSONObject) {
        val mergesArr = model.getJSONArray("merges")
        val mergesMap = linkedMapOf<Pair<String, String>, Int>()
        for (i in 0 until mergesArr.length()) {
            val parts = mergesArr.getString(i).split(" ")
            if (parts.size == 2) {
                mergesMap[Pair(parts[0], parts[1])] = i
            }
        }
        merges = mergesMap
    }

    // -- Encoding -----------------------------------------------------------

    override fun encode(text: String): IntArray {
        if (!loaded) {
            Timber.w("Tokenizer not loaded, returning empty")
            return intArrayOf(clsTokenId, sepTokenId)
        }

        val tokens = mutableListOf<Int>()
        tokens.add(clsTokenId)

        val prepped = preTokenize(text)
        for (word in prepped) {
            if (word.isEmpty()) continue
            val ids = if (isUnigram) encodeWordUnigram(word) else encodeWordBpe(word)
            for (id in ids) tokens.add(id)
        }

        tokens.add(sepTokenId)
        return tokens.toIntArray()
    }

    // -- Pre-tokenization (Metaspace) ---------------------------------------

    private fun preTokenize(text: String): List<String> {
        val withMarkers = text.replace(' ', '▁')
        return withMarkers.split('▁').filter { it.isNotEmpty() }
    }

    // -- Unigram encoding (Viterbi) -----------------------------------------

    private fun encodeWordUnigram(word: String): List<Int> {
        val w = "▁$word"
        val n = w.length

        // dp[i] = best log-probability for prefix w[0:i]
        val dp = FloatArray(n + 1) { Float.NEGATIVE_INFINITY }
        val back = IntArray(n + 1) { -1 }
        dp[0] = 0f

        for (i in 1..n) {
            val maxStart = maxOf(0, i - maxTokenLen)
            for (j in maxStart until i) {
                val candidate = w.substring(j, i)
                val entry = vocab[candidate] ?: continue
                val score = dp[j] + entry.score
                if (score > dp[i]) {
                    dp[i] = score
                    back[i] = j
                }
            }
        }

        // Backtrack to get token IDs
        val ids = mutableListOf<Int>()
        var pos = n
        while (pos > 0) {
            val prev = back[pos]
            if (prev == -1) {
                // Fallback: use UNK token (id 3) for the last character
                ids.add(3)
                pos -= 1
                // Recompute Viterbi from scratch for the remainder
                val subDp = FloatArray(pos + 1) { Float.NEGATIVE_INFINITY }
                val subBack = IntArray(pos + 1) { -1 }
                subDp[0] = 0f
                for (i2 in 1..pos) {
                    val maxStart2 = maxOf(0, i2 - maxTokenLen)
                    for (j2 in maxStart2 until i2) {
                        val candidate2 = w.substring(j2, i2)
                        val entry2 = vocab[candidate2]
                        if (entry2 != null && subDp[j2] + entry2.score > subDp[i2]) {
                            subDp[i2] = subDp[j2] + entry2.score
                            subBack[i2] = j2
                        }
                    }
                }
                // Use subBack to set back for the remaining positions
                for (k in 0..pos) back[k] = subBack[k]
                continue
            }
            val token = w.substring(prev, pos)
            val entry = vocab[token]
            if (entry != null) ids.add(0, entry.id)
            pos = prev
        }

        return ids
    }

    // -- BPE encoding -------------------------------------------------------

    private fun encodeWordBpe(word: String): List<Int> {
        val chars = mutableListOf<String>()
        chars.add("▁")
        for (c in word) chars.add(c.toString())

        while (chars.size > 1) {
            var bestRank = Int.MAX_VALUE
            var bestIdx = -1
            for (i in 0 until chars.size - 1) {
                val rank = merges[Pair(chars[i], chars[i + 1])] ?: continue
                if (rank < bestRank) {
                    bestRank = rank
                    bestIdx = i
                }
            }
            if (bestIdx == -1) break
            chars[bestIdx] = chars[bestIdx] + chars[bestIdx + 1]
            chars.removeAt(bestIdx + 1)
        }

        return chars.mapNotNull { vocab[it]?.id }
    }
}
