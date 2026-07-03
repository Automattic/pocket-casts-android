package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.embedding

/**
 * Tokenizes text into token IDs for the embedding model.
 * Separated as an interface to allow swapping between JNI SentencePiece
 * and pure-Kotlin BPE implementations.
 */
interface TextTokenizer {
    /** Load the tokenizer model file. Called once at startup. */
    fun load(modelPath: String): Boolean

    /** Tokenize text and return token IDs. */
    fun encode(text: String): IntArray

    /** Returns the CLS token ID (typically 0 for BERT-style models). */
    val clsTokenId: Int

    /** Returns the SEP token ID (typically 2 for BERT-style models). */
    val sepTokenId: Int
}
