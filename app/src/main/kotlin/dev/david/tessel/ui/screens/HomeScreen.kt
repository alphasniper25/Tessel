package dev.david.tessel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.david.tessel.core.stats.Ledger
import dev.david.tessel.platform.store.Settings
import dev.david.tessel.ui.components.PrimaryButton
import dev.david.tessel.ui.components.SecondaryButton
import dev.david.tessel.ui.components.ShardMark
import dev.david.tessel.ui.components.Text
import dev.david.tessel.ui.theme.Space
import dev.david.tessel.ui.theme.Theme
import dev.david.tessel.ui.theme.Type

@Composable
fun HomeScreen(
    settings: Settings,
    ledger: Ledger,
    onPlay: () -> Unit,
    onTogether: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onLogo: (() -> Unit)? = null,
    onCards: () -> Unit = {},
    onGarage: () -> Unit = {},
    onTree: () -> Unit = {},
) {
    val c = Theme.colors
    Row(
        Modifier
            .fillMaxSize()
            .background(c.ground)
            .windowInsetsPadding(WindowInsets.displayCutout)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.x4),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // With developer options on, the logo is a shortcut to them.
            ShardMark(
                Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Tessel logo" }
                    .then(if (onLogo == null) Modifier else Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onLogo() }),
            )
            Spacer(Modifier.height(Space.x2))
            Text("TESSEL", Type.largeTitle, c.ink)
            Text("ARENA SURVIVAL  /  ALPHA 12",Type.caption,c.accent)
            Spacer(Modifier.height(Space.x2))
            val best = ledger.bestOverall()
            val line = if (best == null) {
                "No runs yet"
            } else {
                "Best: ${grouped(best.score)}  ·  Stage ${best.stage} in ${clock(best.seconds)}"
            }
            Text(line, Type.subhead.merge(Type.numbers), c.ink2)
            val xp = dev.david.tessel.core.content.AccountProgress.xp(ledger.totals)
            Text("Level ${dev.david.tessel.core.content.AccountProgress.level(xp)} · $xp XP", Type.subhead.merge(Type.numbers), c.ink2)
            CharacterPreview(settings.tankClass, settings.tankLook, settings.difficulty, Modifier.padding(top=8.dp), compact = true)
        }

        Column(Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(Space.x2)) {
            PrimaryButton("Play", onPlay, Modifier.fillMaxWidth().height(56.dp))
            // Keep Play outside the overflow area, even on very short displays.
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Space.x2)) {
                SecondaryButton("Play Together", onTogether, Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                    SecondaryButton("Tank Garage", onGarage, Modifier.weight(1f))
                    SecondaryButton("Tank & Card Codex", onCards, Modifier.weight(1f))
                }
                SecondaryButton("Skill Tree",onTree,Modifier.fillMaxWidth(),tint=c.accent)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                    SecondaryButton("Statistics", onStats, Modifier.weight(1f))
                    SecondaryButton("Settings", onSettings, Modifier.weight(1f))
                }
            }
        }
    }
}

fun clock(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)
