package dev.david.tessel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.david.tessel.core.content.*
import dev.david.tessel.core.stats.Ledger
import dev.david.tessel.ui.components.*
import dev.david.tessel.ui.theme.Space
import dev.david.tessel.ui.theme.Theme
import dev.david.tessel.ui.theme.Type

@Composable
fun CardCollectionScreen(ledger: Ledger, onBack: () -> Unit) {
    val c = Theme.colors
    val totals = ledger.totals
    val xp = AccountProgress.xp(totals)
    val level = AccountProgress.level(xp)
    val unlocked = CardMission.entries.count { it.unlocked(totals) }
    Column(Modifier.fillMaxSize().background(c.ground).windowInsetsPadding(WindowInsets.displayCutout)) {
        Box(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Space.x4)) {
            BackButton("Home", onBack, Modifier.align(Alignment.CenterStart))
            Text("Cards & Missions", Type.headline, c.ink, Modifier.align(Alignment.Center))
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize().padding(horizontal = Space.x6), verticalArrangement = Arrangement.spacedBy(Space.x3)) {
                item {
                    Text("Level $level · $xp XP", Type.title2, c.ink)
                    Text("${AccountProgress.threshold(level + 1) - xp} XP to next level", Type.subhead, c.ink2)
                    Text("${Upgrades.count - CardMission.entries.size + unlocked} / ${Upgrades.count} cards unlocked", Type.subhead, c.ink2)
                    Text("Unlocked cards enter your shop pool on the next new run. All purchases still cost run coins. XP is earned from survival, kills, bosses and wins.", Type.footnote, c.ink3)
                }
                item { Text("Mission cards", Type.title3, c.ink) }
                items(CardMission.entries) { m ->
                    val def = Upgrades.of(m.upgrade)
                    val done = m.unlocked(totals)
                    Group(title = null) {
                        Column(Modifier.fillMaxWidth().padding(Space.x4)) {
                            Text("${m.rarity.label} · ${def.name} · ${if (done) "Unlocked" else "Locked"}", Type.headline, if (done) c.ink else c.ink2)
                            Text(def.blurb, Type.subhead, c.ink2)
                            Text(m.goal, Type.footnote, c.ink3)
                            Text("${m.progress(totals).coerceAtMost(m.target)} / ${m.target}", Type.footnote.merge(Type.numbers), c.ink2)
                        }
                    }
                }
                item { Text("Starter collection", Type.title3, c.ink) }
                items((0 until Upgrades.count).filter { CardMission.forUpgrade(it) == null }) { id ->
                    val def = Upgrades.of(id)
                    Group(title = null) {
                        Column(Modifier.fillMaxWidth().padding(Space.x4)) {
                            Text("${def.name} · Unlocked", Type.headline, c.ink)
                            Text(def.blurb, Type.subhead, c.ink2)
                            Text("${def.family.name.lowercase()} · all five rarities", Type.footnote, c.ink3)
                        }
                    }
                }
                item { Spacer(Modifier.height(Space.x6)) }
            }
        }
    }
}
