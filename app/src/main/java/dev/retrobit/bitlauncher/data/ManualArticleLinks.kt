package dev.retrobit.bitlauncher.data

/** A document-style application hyperlink embedded in the manual article draft. */
data class ManualArticleLink(
    val label: String,
    val appKey: String,
    val raw: String,
    val start: Int,
    val endExclusive: Int,
)

private val ManualArticleLinkPattern = Regex("""\[([^\]\n]+)]\(app:([^)\s]+)\)""")

/** Markdown-like storage keeps links portable in preferences and the existing JSON backup. */
fun parseManualArticleLinks(text: String): List<ManualArticleLink> =
    ManualArticleLinkPattern.findAll(text).map { match ->
        ManualArticleLink(
            label = match.groupValues[1],
            appKey = match.groupValues[2],
            raw = match.value,
            start = match.range.first,
            endExclusive = match.range.last + 1,
        )
    }.toList()

fun manualArticleLinkToken(label: String, appKey: String): String =
    "[${label.trim()}](app:$appKey)"

fun insertManualArticleLink(text: String, label: String, appKey: String): String {
    val token = manualArticleLinkToken(label, appKey)
    val separator = if (text.isEmpty() || text.endsWith(' ') || text.endsWith('\n')) "" else " "
    return text + separator + token
}

fun replaceManualArticleLink(
    text: String,
    link: ManualArticleLink,
    label: String,
    appKey: String,
): String = text.replaceRange(link.start, link.endExclusive, manualArticleLinkToken(label, appKey))

fun removeManualArticleLink(text: String, link: ManualArticleLink): String =
    text.replaceRange(link.start, link.endExclusive, link.label)
