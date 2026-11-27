package dev.david.tessel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.david.tessel.core.content.Difficulty
import dev.david.tessel.core.content.CharacterDef
import dev.david.tessel.platform.store.SoloSave
import dev.david.tessel.platform.store.SoloSaves
import dev.david.tessel.ui.components.PrimaryButton
import dev.david.tessel.ui.components.SecondaryButton
import dev.david.tessel.ui.components.Segmented
import dev.david.tessel.ui.components.Text
import dev.david.tessel.ui.theme.Corner
import dev.david.tessel.ui.theme.Space
import dev.david.tessel.ui.theme.Theme
import dev.david.tessel.ui.theme.Type

/**
 * What Play opens when there are runs waiting: a new run on the left, the
 * saved ones on the right. Deleting a saved run asks once more, in its row.
 */
@Composable
fun PlayPanel(
    saves: List<SoloSave>,
    difficulty: Difficulty,
    now: Long,
    onDifficulty: (Difficulty) -> Unit,
    onNew: () -> Unit,
    onContinue: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onNewCharacter: ((CharacterDef) -> Unit)? = null,
    initialCharacter: CharacterDef = CharacterDef.BALANCED,
    look: dev.david.tessel.core.content.TankLook = dev.david.tessel.core.content.TankLook(),
) {
    val c = Theme.colors
    val full = saves.size >= SoloSaves.SLOTS
    var selected by remember(initialCharacter) { mutableIntStateOf(initialCharacter.ordinal) }
    // Reserve the action row before measuring scrollable content. The sheet can
    // be shorter than the preferred layout on a phone with a large font scale.
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = Space.x4, vertical = Space.x2)) {
        val panelHeight = minOf(maxHeight, 292.dp)
        Column(Modifier.fillMaxWidth().height(panelHeight), verticalArrangement = Arrangement.spacedBy(Space.x2)) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.x4)) {
                Column(Modifier.weight(1.05f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Space.x1)) {
                    Text("Start a run", Type.title2, c.ink)
                    Text("Difficulty", Type.footnote, c.ink2)
                    Segmented(Difficulty.entries.map { it.label }, difficulty.ordinal, { onDifficulty(Difficulty.entries[it]) })
                    if (onNewCharacter != null) {
                        Text("Tank type", Type.footnote, c.ink2)
                        Segmented(CharacterDef.entries.map { it.label }, selected, { selected = it })
                        CharacterPreview(CharacterDef.fromId(selected), look, difficulty,
                            Modifier.clip(RoundedCornerShape(Corner.chip)).background(c.raised).padding(horizontal = Space.x2, vertical = Space.x1), compact = true)
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Saved Runs", Type.title2, c.ink, Modifier.weight(1f))
                        Text("${saves.size} / ${SoloSaves.SLOTS}", Type.footnote.merge(Type.numbers), c.ink3)
                    }
                    Spacer(Modifier.height(Space.x2))
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Space.x2)) {
                        if (saves.isEmpty()) Text("No saved runs. Save and quit during a run to continue later.", Type.subhead, c.ink3)
                        for (save in saves) SavedRun(save, now, { onContinue(save.slot) }, { onDelete(save.slot) })
                    }
                }
            }
            dev.david.tessel.ui.components.Divider()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.x4)) {
                PrimaryButton("New Run", { if (onNewCharacter == null) onNew() else onNewCharacter(CharacterDef.fromId(selected)) }, Modifier.weight(1.05f), enabled = !full)
                Text(if (full) "All slots are in use. Delete a saved run to start another." else "${difficulty.label} · ${CharacterDef.fromId(selected).label}", Type.footnote, c.ink2, Modifier.weight(1f), maxLines = 2)
            }
        }
    }
}

@Composable
private fun SavedRun(save: SoloSave, now: Long, onContinue: () -> Unit, onDelete: () -> Unit) {
    val c = Theme.colors
    val cp = save.checkpoint
    var asking by remember { mutableIntStateOf(0) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Corner.button))
            .background(c.raised)
            .padding(Space.x3),
    ) {
        Text("Stage ${cp.nextStage}  ·  ${cp.difficulty.label}", Type.headline, c.ink, maxLines = 1)
        Text("Score ${grouped(cp.score())}  ·  ${clock(cp.seconds)}  ·  ${ago(save.savedAt, now)}",
            Type.footnote.merge(Type.numbers), c.ink2, maxLines = 1)
        if (asking == 0) {
            Spacer(Modifier.height(Space.x2))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                SecondaryButton("Delete", { asking = 1 }, Modifier.weight(1f), tint = c.danger)
                PrimaryButton("Continue", onContinue, Modifier.weight(1.2f))
            }
        }
        if (asking == 1) {
            Spacer(Modifier.height(Space.x2))
            Text("Delete this run? It cannot be brought back.", Type.subhead, c.ink)
            Spacer(Modifier.height(Space.x2))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
                SecondaryButton("Cancel", { asking = 0 }, Modifier.weight(1f))
                SecondaryButton("Delete", onDelete, Modifier.weight(1f), tint = c.danger)
            }
        }
    }
}

/** How long ago, in the fewest words: just now, 5 min ago, 3 h ago, yesterday, 4 days ago. */
internal fun ago(then: Long, now: Long): String {
    val s = ((now - then) / 1000).coerceAtLeast(0)
    return when {
        s < 60 -> "just now"
        s < 3_600 -> "${s / 60} min ago"
        s < 86_400 -> "${s / 3_600} h ago"
        s < 2 * 86_400 -> "yesterday"
        else -> "${s / 86_400} days ago"
    }
}
