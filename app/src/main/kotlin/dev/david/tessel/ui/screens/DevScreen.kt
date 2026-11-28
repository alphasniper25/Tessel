package dev.david.tessel.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.david.tessel.core.content.Bestiary
import dev.david.tessel.core.content.TankEvolution
import dev.david.tessel.core.content.SkillGraph
import dev.david.tessel.core.content.SkillTree
import dev.david.tessel.core.content.PerkCombo
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import dev.david.tessel.core.content.Perks
import dev.david.tessel.core.content.Upgrades
import dev.david.tessel.ui.components.NavRow
import dev.david.tessel.ui.components.PrimaryButton
import dev.david.tessel.platform.dev.LogExport
import dev.david.tessel.platform.store.DEV_SPEEDS
import dev.david.tessel.platform.store.Dev
import dev.david.tessel.platform.store.Settings
import dev.david.tessel.ui.components.BackButton
import dev.david.tessel.ui.components.Divider
import dev.david.tessel.ui.components.Group
import dev.david.tessel.ui.components.Segmented
import dev.david.tessel.ui.components.SecondaryButton
import dev.david.tessel.ui.components.Text
import dev.david.tessel.ui.components.ToggleRow
import dev.david.tessel.ui.components.focusRing
import dev.david.tessel.ui.components.pressScale
import dev.david.tessel.ui.theme.Corner
import dev.david.tessel.ui.theme.Space
import dev.david.tessel.ui.theme.Theme
import dev.david.tessel.ui.theme.Type

private val COINS = listOf(0, 500, 5_000, 50_000)

/**
 * Developer options. Everything that changes the game applies to solo runs
 * only, so a team never plays with it, and a run that used any of it is kept
 * out of the saves, the history and the logbook.
 */
@Composable
fun DevScreen(
    settings: Settings,
    version: String,
    inRun: Boolean,
    onChange: ((Settings) -> Settings) -> Unit,
    onBreakNow: () -> Unit,
    onTutorial: () -> Unit,
    onStartRun: () -> Unit,
    onStartShop: () -> Unit,
    canLaunchTrial:Boolean,
    onBack: () -> Unit,
) {
    val c = Theme.colors
    val context = LocalContext.current
    val dev = settings.dev
    fun change(next: (Dev) -> Dev) = onChange { it.copy(dev = next(it.dev)) }
    var exported by remember { mutableStateOf<String?>(null) }
    var showSkills by remember { mutableStateOf(false) }
    var showEnemies by remember { mutableStateOf(false) }
    var showUpgrades by remember { mutableStateOf(true) }
    var showFrames by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.ground)
            .windowInsetsPadding(WindowInsets.displayCutout),
    ) {
        Box(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Space.x4)) {
            BackButton("Settings", onBack, Modifier.align(Alignment.CenterStart))
            Text("Developer Options", Type.headline, c.ink, Modifier.align(Alignment.Center))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=Space.x4,vertical=Space.x2),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            PrimaryButton("Open Shop · Stage ${dev.startStage}",onStartShop,Modifier.weight(1f),enabled=canLaunchTrial)
            SecondaryButton("Try a Run With These",onStartRun,Modifier.weight(1f),enabled=canLaunchTrial)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.x6),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(top = Space.x2),
                verticalArrangement = Arrangement.spacedBy(Space.x6),
            ) {
                Group(
                    "Solo Trial",
                    footer = "These only reach a run started with Try a Run With These. Play on the home screen is always a normal run. A team never gets them. A trial run is not saved, not counted and not in the logbook.",
                ) {
                    Text("Test runs are isolated. Normal saves, score, missions, account rewards and multiplayer are untouched.",Type.footnote,c.ink2,Modifier.padding(Space.x3))
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = Space.x4, vertical = Space.x2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Start Stage", Type.body, c.ink)
                            Text("1–50. Open Shop starts here immediately.", Type.footnote, c.ink3)
                        }
                        Nudge("-5") { change { it.copy(startStage = (it.startStage - 5).coerceAtLeast(1)) } }
                        Nudge("-1") { change { it.copy(startStage = (it.startStage - 1).coerceAtLeast(1)) } }
                        Text(dev.startStage.toString(), Type.headline.merge(Type.numbers), c.ink, Modifier.width(40.dp), align = androidx.compose.ui.text.style.TextAlign.Center)
                        Nudge("+1") { change { it.copy(startStage = (it.startStage + 1).coerceAtMost(50)) } }
                        Nudge("+5") { change { it.copy(startStage = (it.startStage + 5).coerceAtMost(50)) } }
                    }
                    Divider()
                    Column(Modifier.padding(horizontal = Space.x4, vertical = Space.x3)) {
                        Text("Start Coins · ${dev.startCoins}", Type.body, c.ink)
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Nudge("-1k"){change{it.copy(startCoins=(it.startCoins-1000).coerceAtLeast(0))}}
                            Nudge("+1k"){change{it.copy(startCoins=(it.startCoins+1000).coerceAtMost(10000000))}}
                            Nudge("+10k"){change{it.copy(startCoins=(it.startCoins+10000).coerceAtMost(10000000))}}
                        }
                        Spacer(Modifier.height(Space.x2))
                        Segmented(
                            options = COINS.map { "%,d".format(it) },
                            selected = COINS.indexOf(dev.startCoins).coerceAtLeast(0),
                            onSelect = { i -> change { it.copy(startCoins = COINS[i]) } },
                        )
                    }
                    Divider()
                    ToggleRow("Invincible", dev.invincible, { v -> change { it.copy(invincible = v) } }, detail = "Hits still show, but cost no health.")
                    Divider()
                    ToggleRow("No Enemies", dev.noSpawns, { v -> change { it.copy(noSpawns = v) } }, detail = "Nothing spawns. The stage clock still runs.")
                    Divider()
                    Column(Modifier.padding(horizontal = Space.x4, vertical = Space.x3)) {
                        Text("Game Speed", Type.body, c.ink)
                        Spacer(Modifier.height(Space.x2))
                        Segmented(
                            options = DEV_SPEEDS.map { if (it == 1f) "1x" else "${it}x".replace(".0x", "x") },
                            selected = DEV_SPEEDS.indexOf(dev.speed).coerceAtLeast(0),
                            onSelect = { i -> change { it.copy(speed = DEV_SPEEDS[i]) } },
                        )
                    }
                }

                Group("Test fixture") {
                    NavRow("Tank frame · ${TankEvolution.fromId(dev.frame).label}",{showFrames=!showFrames},detail="Any of the 33 frames, including secret evolutions")
                    if(showFrames)for(frame in TankEvolution.entries){
                        ToggleRow(frame.label,dev.frame==frame.ordinal,{v->if(v)change{it.copy(frame=frame.ordinal)}},detail=frame.detail)
                    }
                    Divider()
                    ToggleRow("All mission cards",dev.allMissions,{v->change{it.copy(allMissions=v)}})
                    Divider()
                    Row(Modifier.fillMaxWidth().padding(Space.x3),verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){Text("Starting health",Type.body,c.ink);Text(if(dev.startHp<0)"Full hull" else "${dev.startHp} HP",Type.footnote,c.ink2)}
                        Nudge("-1"){change{it.copy(startHp=(if(it.startHp<0)10 else it.startHp-1).coerceAtLeast(1))}}
                        Nudge("+1"){change{it.copy(startHp=(if(it.startHp<0)10 else it.startHp+1).coerceAtMost(999))}}
                        Nudge("Full"){change{it.copy(startHp=-1)}}
                    }
                }
                Group("Enemies and bosses"){
                    NavRow(if(showEnemies)"Collapse enemy controls" else "Show all enemy controls",{showEnemies=!showEnemies},detail="Spawn filters, boss selection, individual speed / HP / damage")
                    if(showEnemies)DevEnemyControls(settings,onChange)
                }
                Group("Skills · ${SkillGraph.count(dev.skills)} / 60") {
                    Row(Modifier.padding(Space.x3),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        SecondaryButton("All skills",{change{it.copy(skills=SkillGraph.MASK)}},Modifier.weight(1f))
                        SecondaryButton("Clear skills",{change{it.copy(skills=0L)}},Modifier.weight(1f))
                    }
                    NavRow(if(showSkills)"Hide skill controls" else "Show all 60 skills",{showSkills=!showSkills})
                    if(showSkills)for(node in SkillGraph.nodes){
                        ToggleRow(node.name,SkillGraph.owns(dev.skills,node.id),{v->change{it.copy(skills=if(v)it.skills or SkillGraph.bit(node.id) else it.skills and SkillGraph.bit(node.id).inv())}},detail=node.description)
                    }
                }
                Group("Elites and Perks", footer = "Elites: every enemy that comes is elite. Perks: taken at the start of a trial run.") {
                    ToggleRow("Elite Enemies", dev.elites, { v -> change { it.copy(elites = v) } })
                    Row(Modifier.padding(Space.x3),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        SecondaryButton("All perks",{change{it.copy(perks=(0 until SkillTree.ORIGINAL_PERKS).toSet())}},Modifier.weight(1f))
                        SecondaryButton("Clear perks",{change{it.copy(perks=emptySet())}},Modifier.weight(1f))
                    }
                    for (k in 0 until SkillTree.ORIGINAL_PERKS) {
                        Divider()
                        ToggleRow(
                            Perks.of(k).name,
                            k in dev.perks,
                            { v -> change { it.copy(perks = if (v) it.perks + k else it.perks - k) } },
                            detail = Perks.of(k).blurb,
                        )
                    }
                }

                Group("Starting Upgrades", footer = "Trial ranks only. All real and legacy upgrade IDs are testable.") {
                    Row(Modifier.padding(Space.x3),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        SecondaryButton("Max all",{change{it.copy(upgrades=(0 until Upgrades.count).associateWith{u->Upgrades.of(u).maxLevel})}},Modifier.weight(1f))
                        SecondaryButton("Clear",{change{it.copy(upgrades=emptyMap())}},Modifier.weight(1f))
                    }
                    NavRow(if(showUpgrades)"Hide ranks" else "Show all ranks",{showUpgrades=!showUpgrades})
                    if(showUpgrades)for (id in 0 until Upgrades.count) {
                        val def = Upgrades.of(id)
                        val level = dev.upgrades[id] ?: 0
                        fun set(n: Int) = change {
                            val v = n.coerceIn(0, def.maxLevel)
                            it.copy(upgrades = if (v == 0) it.upgrades - id else it.upgrades + (id to v))
                        }
                        if (id > 0) Divider()
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = Space.x4, vertical = Space.x2),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(def.name, Type.body, c.ink)
                                Text(def.blurb, Type.footnote, c.ink3, maxLines = 1)
                            }
                            Nudge("-1") { set(level - 1) }
                            Text("$level", Type.headline.merge(Type.numbers), c.ink, Modifier.width(44.dp), align = androidx.compose.ui.text.style.TextAlign.Center)
                            Nudge("+1") { set(level + 1) }
                            Nudge("Max") { set(def.maxLevel) }
                        }
                    }
                }


                Group("Tools", footer = exported) {
                    ActionLine("Export Log", "Writes a text file and opens the share sheet.") {
                        exported = exportLog(context, settings, version)
                    }
                    Divider()
                    ActionLine("Break Reminder Now", if (inRun) "Pauses the run with the reminder card." else "Only while a run is on.", enabled = inRun) { onBreakNow() }
                    Divider()
                    ActionLine("Open Tutorial", "Starts the practice, also from here.", enabled = !inRun) { onTutorial() }
                    Divider()
                    ActionLine("Show Welcome Prompt Again", "Offers the tutorial the way a new install does.") {
                        onChange { it.copy(tutorialSeen = false) }
                    }
                }

                Group("Reset", footer = "Reset puts every option above back to normal. Lock hides this page until the version line is tapped exactly five times again.") {
                    Row(Modifier.padding(Space.x3), horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
                        SecondaryButton("Reset Developer Settings", { change { Dev() } }, Modifier.weight(1f), tint = c.danger)
                        SecondaryButton("Lock", { onChange { it.copy(devUnlocked = false, dev = Dev()) }; onBack() }, Modifier.width(96.dp))
                    }
                }
                Spacer(Modifier.height(Space.x4))
            }
        }
    }
}

private fun exportLog(context: Context, settings: Settings, version: String): String =
    runCatching {
        val file = LogExport.write(context, settings, version)
        LogExport.share(context, file)
        "Saved as ${file.name} in the app's files folder."
    }.getOrElse { "Could not write the log: ${it.message}" }

@Composable
private fun ActionLine(title: String, detail: String, enabled: Boolean = true, onClick: () -> Unit) {
    val c = Theme.colors
    val source = remember { MutableInteractionSource() }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .focusRing(source, corner = 0.dp)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button) { onClick() }
            .padding(horizontal = Space.x4, vertical = Space.x2),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, Type.body, if (enabled) c.ink else c.ink3)
        Text(detail, Type.footnote, c.ink3)
    }
}

@Composable
private fun Nudge(label: String, onClick: () -> Unit) {
    val c = Theme.colors
    val source = remember { MutableInteractionSource() }
    Box(
        Modifier
            .padding(start = Space.x1)
            .pressScale(source)
            .width(44.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(Corner.chip))
            .background(c.track)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, Type.subhead.merge(Type.numbers), c.ink, maxLines = 1)
    }
}
