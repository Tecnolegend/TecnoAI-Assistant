package com.tecnolegend.tecnoai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private sealed interface MdBlock {
    data class Paragraph(val text: String) : MdBlock
    data class Heading(val text: String, val level: Int) : MdBlock
    data class Bullet(val text: String, val marker: String, val indent: Int) : MdBlock
    data class Code(val code: String) : MdBlock
}

private fun parseBlocks(md: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = md.replace("\r", "").split("\n")
    val para = StringBuilder()
    fun flush() {
        if (para.isNotBlank()) blocks.add(MdBlock.Paragraph(para.toString().trim()))
        para.clear()
    }
    var i = 0
    while (i < lines.size) {
        val raw = lines[i]
        val line = raw.trimEnd()
        val trimmed = line.trimStart()
        when {
            trimmed.startsWith("```") -> {
                flush()
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    code.append(lines[i]).append('\n')
                    i++
                }
                blocks.add(MdBlock.Code(code.toString().trimEnd()))
            }
            trimmed.startsWith("#") -> {
                flush()
                val level = trimmed.takeWhile { it == '#' }.length
                blocks.add(MdBlock.Heading(trimmed.drop(level).trim(), level))
            }
            Regex("^([-*•])\\s+").containsMatchIn(trimmed) -> {
                flush()
                blocks.add(MdBlock.Bullet(trimmed.replaceFirst(Regex("^([-*•])\\s+"), ""), "•", (line.length - trimmed.length) / 2))
            }
            Regex("^\\d+[.)]\\s+").containsMatchIn(trimmed) -> {
                flush()
                val num = trimmed.takeWhile { it.isDigit() }
                blocks.add(MdBlock.Bullet(trimmed.replaceFirst(Regex("^\\d+[.)]\\s+"), ""), "$num.", (line.length - trimmed.length) / 2))
            }
            trimmed.isEmpty() -> flush()
            else -> {
                if (para.isNotEmpty()) para.append(' ')
                para.append(trimmed)
            }
        }
        i++
    }
    flush()
    return blocks
}

/** Inline **bold**, *italic*, `code` and [links](url) (link text only). */
fun inlineMarkdown(text: String, codeBackground: Color = Color(0xFF242426)): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) && text.indexOf("**", i + 2) > i + 2 -> {
                val end = text.indexOf("**", i + 2)
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(text.substring(i + 2, end)) }
                i = end + 2
            }
            text[i] == '`' && text.indexOf('`', i + 1) > i + 1 -> {
                val end = text.indexOf('`', i + 1)
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground, fontSize = 14.sp)) {
                    append(" ${text.substring(i + 1, end)} ")
                }
                i = end + 1
            }
            (text[i] == '*' || text[i] == '_') && i + 1 < text.length && text[i + 1] != ' ' &&
                text.indexOf(text[i], i + 1) > i + 1 -> {
                val end = text.indexOf(text[i], i + 1)
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                i = end + 1
            }
            text[i] == '[' && text.indexOf("](", i) > i && text.indexOf(')', text.indexOf("](", i)) > 0 -> {
                val mid = text.indexOf("](", i)
                val end = text.indexOf(')', mid)
                withStyle(SpanStyle(color = Color(0xFF8AB4F8))) { append(text.substring(i + 1, mid)) }
                i = end + 1
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFEDEDED),
    fontSize: Int = 16
) {
    val blocks = remember(markdown) { parseBlocks(markdown) }
    SelectionContainer {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            blocks.forEach { block ->
                when (block) {
                    is MdBlock.Paragraph -> Text(
                        text = inlineMarkdown(block.text),
                        color = color,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.55f).sp
                    )
                    is MdBlock.Heading -> Text(
                        text = inlineMarkdown(block.text),
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (fontSize + if (block.level <= 1) 4 else 2).sp,
                        lineHeight = (fontSize * 1.5f + 4).sp
                    )
                    is MdBlock.Bullet -> Row(modifier = Modifier.padding(start = (block.indent * 14).dp)) {
                        Text(
                            text = block.marker,
                            color = Color(0xFF9A9AA0),
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.55f).sp,
                            modifier = Modifier.width(if (block.marker == "•") 18.dp else 26.dp)
                        )
                        Text(
                            text = inlineMarkdown(block.text),
                            color = color,
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.55f).sp
                        )
                    }
                    is MdBlock.Code -> Text(
                        text = block.code,
                        color = Color(0xFFDADADA),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF161618), RoundedCornerShape(12.dp))
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}
