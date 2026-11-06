package io.github.chaitanyakelkar.mdreader.markdown

enum class TokenType { KEYWORD, STRING, COMMENT, NUMBER }

data class CodeToken(val type: TokenType, val start: Int, val end: Int)

/**
 * A small, language-family tokenizer. It colors comments, strings, numbers and keywords,
 * and leaves unknown languages uncolored rather than guessing.
 */
object CodeHighlighter {
    private data class Language(
        val keywords: Set<String>,
        val lineComments: List<String> = emptyList(),
        val blockComment: Pair<String, String>? = null,
        val quotes: String = "\"'",
        val ignoreCase: Boolean = false,
    )

    private fun words(text: String) = text.split(' ').toSet()

    private val cStyle = Language(
        keywords = words(
            "abstract as async await break case catch class const continue data default do else enum export " +
                "extends false final finally for fn fun func function go if impl implements import in " +
                "interface is let match mod module mut new nil null object override package private " +
                "protected pub public return sealed self static struct super switch this throw throws trait " +
                "true try type typeof undefined use val var void when where while yield int long float double " +
                "boolean bool char string",
        ),
        lineComments = listOf("//"),
        blockComment = "/*" to "*/",
        quotes = "\"'`",
    )

    private val python = Language(
        keywords = words(
            "and as assert async await break class continue def del elif else except False finally for from " +
                "global if import in is lambda None nonlocal not or pass raise return self True try while with yield",
        ),
        lineComments = listOf("#"),
    )

    private val shell = Language(
        keywords = words("if then else elif fi for while do done case esac in function return export local echo sudo cd"),
        lineComments = listOf("#"),
    )

    private val sql = Language(
        keywords = words(
            "select from where insert into update delete create table alter drop join left right inner outer on " +
                "group by order having as and or not null values set primary key limit",
        ),
        lineComments = listOf("--"),
        blockComment = "/*" to "*/",
        ignoreCase = true,
    )

    private val data = Language(keywords = words("true false null yes no"), lineComments = listOf("#"))

    private val languages: Map<String, Language> = buildMap {
        listOf(
            "kotlin", "kt", "kts", "java", "javascript", "js", "jsx", "typescript", "ts", "tsx",
            "c", "cpp", "c++", "h", "csharp", "cs", "go", "rust", "rs", "swift", "dart", "scala", "php",
        ).forEach { put(it, cStyle) }
        listOf("python", "py").forEach { put(it, python) }
        listOf("bash", "sh", "shell", "zsh", "console").forEach { put(it, shell) }
        put("sql", sql)
        listOf("yaml", "yml", "toml", "json").forEach { put(it, data) }
    }

    fun tokenize(code: String, language: String?): List<CodeToken> {
        val lang = language?.lowercase()?.let(languages::get) ?: return emptyList()
        val tokens = ArrayList<CodeToken>()
        var i = 0
        while (i < code.length) {
            val c = code[i]
            val block = lang.blockComment
            when {
                block != null && code.startsWith(block.first, i) -> {
                    val close = code.indexOf(block.second, i + block.first.length)
                    val end = if (close < 0) code.length else close + block.second.length
                    tokens += CodeToken(TokenType.COMMENT, i, end)
                    i = end
                }
                lang.lineComments.any { code.startsWith(it, i) } -> {
                    val newline = code.indexOf('\n', i)
                    val end = if (newline < 0) code.length else newline
                    tokens += CodeToken(TokenType.COMMENT, i, end)
                    i = end
                }
                c in lang.quotes -> {
                    val end = stringEnd(code, i, c)
                    tokens += CodeToken(TokenType.STRING, i, end)
                    i = end
                }
                c.isDigit() -> {
                    var end = i + 1
                    while (end < code.length && (code[end].isLetterOrDigit() || code[end] == '.' || code[end] == '_')) end++
                    tokens += CodeToken(TokenType.NUMBER, i, end)
                    i = end
                }
                c.isLetter() || c == '_' -> {
                    var end = i + 1
                    while (end < code.length && (code[end].isLetterOrDigit() || code[end] == '_')) end++
                    val word = code.substring(i, end)
                    if ((if (lang.ignoreCase) word.lowercase() else word) in lang.keywords) {
                        tokens += CodeToken(TokenType.KEYWORD, i, end)
                    }
                    i = end
                }
                else -> i++
            }
        }
        return tokens
    }

    /** Strings end at the closing quote, or at the line end if unterminated (backticks span lines). */
    private fun stringEnd(code: String, start: Int, quote: Char): Int {
        var i = start + 1
        while (i < code.length) {
            val c = code[i]
            when {
                c == '\\' -> i++
                c == quote -> return i + 1
                c == '\n' && quote != '`' -> return i
            }
            i++
        }
        return code.length
    }
}
