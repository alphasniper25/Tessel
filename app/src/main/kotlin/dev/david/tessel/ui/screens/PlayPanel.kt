package dev.david.tessel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
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
import dev.david.tessel.core.content.Balance
import dev.david.tessel.ui.components.BackButton
import dev.david.tessel.ui.components.Divider
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
    onBack: (() -> Unit)? = null,
) {
    val c = Theme.colors
    val full = saves.size >= SoloSaves.SLOTS
    var selected by remember(initialCharacter) { mutableIntStateOf(initialCharacter.ordinal) }
    val frame=CharacterDef.fromId(selected)
    // Permanent header and footer; only the two content columns scroll. No fixed 292dp sheet.
    Column(Modifier.fillMaxSize().background(c.ground).windowInsetsPadding(WindowInsets.displayCutout)
        .padding(horizontal=Space.x4,vertical=Space.x2)) {
        Row(Modifier.fillMaxWidth().height(44.dp),verticalAlignment=Alignment.CenterVertically) {
            if(onBack!=null)BackButton("Home",onBack)
            Spacer(Modifier.weight(1f))
            Text("Start a run",Type.title2,c.ink)
            Spacer(Modifier.weight(1f))
            Text("SOLO",Type.caption,c.ink2)
        }
        Divider()
        Row(Modifier.weight(1f).fillMaxWidth().padding(vertical=Space.x2),horizontalArrangement=Arrangement.spacedBy(Space.x4)){
            Column(Modifier.weight(1.05f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text("Difficulty",Type.footnote,c.ink2)
                Segmented(Difficulty.entries.map { it.label },difficulty.ordinal,{onDifficulty(Difficulty.entries[it])})
                if(onNewCharacter!=null){
                    Spacer(Modifier.height(2.dp))
                    Text("Starting tank",Type.footnote,c.ink2)
                    Segmented(CharacterDef.entries.map{it.label},selected,{selected=it})
                    Row(Modifier.fillMaxWidth().background(c.surface,RoundedCornerShape(8.dp)).padding(horizontal=8.dp,vertical=4.dp),
                        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        TankPreview(frame,look,Modifier.size(40.dp))
                        Column(Modifier.weight(1f)){
                            Text("HP ${(Balance.PLAYER_HP+difficulty.bonusHp+frame.extraHp).coerceAtLeast(1)}  ·  DMG ${(frame.damageScale*100).toInt()}%",Type.footnote.merge(Type.numbers),c.ink)
                            Text("Speed ${(frame.speedScale*100).toInt()}%  ·  Fire ${(frame.fireScale*100).toInt()}%",Type.footnote.merge(Type.numbers),c.ink2)
                        }
                    }
                    Text(frame.description,Type.footnote,c.ink2)
                }
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(c.line))
            Column(Modifier.weight(1f).fillMaxHeight()){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text("Saved runs",Type.headline,c.ink,Modifier.weight(1f))
                    Text("${saves.size} / ${SoloSaves.SLOTS}",Type.caption.merge(Type.numbers),c.ink3)
                }
                Spacer(Modifier.height(Space.x2))
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    if(saves.isEmpty()){
                        Box(Modifier.fillMaxWidth().border(1.dp,c.line,RoundedCornerShape(10.dp)).padding(Space.x3)){
                            Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
                                Text("No runs in progress",Type.subhead,c.ink)
                                Text("Save and quit between stages to resume your campaign here.",Type.footnote,c.ink2)
                                Text("5 save slots available",Type.caption,c.ink3)
                            }
                        }
                    }
                    for(save in saves)SavedRun(save,now,{onContinue(save.slot)},{onDelete(save.slot)})
                }
            }
        }
        Divider()
        Row(Modifier.fillMaxWidth().padding(top=Space.x2),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(Space.x4)){
            Column(Modifier.weight(1f)){
                Text(if(full)"All slots occupied" else "${difficulty.label} · ${frame.label}",Type.headline,c.ink)
                Text(if(full)"Delete a saved run to start another." else "First evolution after stage 5",Type.footnote,c.ink2)
            }
            PrimaryButton("New Run",{if(onNewCharacter==null)onNew() else onNewCharacter(frame)},Modifier.weight(1f),enabled=!full)
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
