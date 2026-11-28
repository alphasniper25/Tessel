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
import dev.david.tessel.core.content.Perks
import dev.david.tessel.core.content.Upgrades
import dev.david.tessel.core.content.TankEvolution
import dev.david.tessel.core.content.SkillGraph
import dev.david.tessel.core.content.SkillTree
import dev.david.tessel.core.content.CardCode
import dev.david.tessel.core.content.CardRarity
import dev.david.tessel.core.dev.DevScenario
import dev.david.tessel.core.dev.DevReward
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.SolidColor
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
    onStartRun: (DevScenario) -> Unit,
    onStartShop: (DevScenario)->Unit,
    sandbox:Boolean=false,
    onBack: () -> Unit,
) {
    val c = Theme.colors
    val context = LocalContext.current
    val dev = settings.dev
    fun change(next: (Dev) -> Dev) = onChange { it.copy(dev = next(it.dev)) }
    var exported by remember { mutableStateOf<String?>(null) }
    var frame by remember { mutableStateOf(TankEvolution.BASIC) }
    var skillMask by remember { mutableStateOf(0L) }
    var health by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf(listOf(-1,-1,-1)) }
    var reward by remember { mutableStateOf(DevReward.AUTO) }
    var allMissions by remember { mutableStateOf(true) }
    fun scenario()=DevScenario(frame,skillMask,health.toIntOrNull()?:-1,stock,reward,allMissions)

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
        if(!inRun||sandbox){
            Row(Modifier.fillMaxWidth().padding(horizontal=Space.x4,vertical=4.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                PrimaryButton("Open Shop Preview",{onStartShop(scenario())},Modifier.weight(1f))
                SecondaryButton("Try a Run With These",{onStartRun(scenario())},Modifier.weight(1f))
            }
        }else Text("Leave the normal run to start a developer sandbox.",Type.footnote,c.ink2,Modifier.padding(horizontal=Space.x4))
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.x6),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(top = Space.x2),
                verticalArrangement = Arrangement.spacedBy(Space.x6),
            ) {
                Group(
                    "Solo Trial",
                    footer = "These only reach an explicit developer trial or shop preview. Play on the home screen is always a normal run. A team never gets them. A trial run is not saved, not counted and not in the logbook.",
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = Space.x4, vertical = Space.x2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Start Stage", Type.body, c.ink)
                            Text("Skips the shops before it. New runs.", Type.footnote, c.ink3)
                        }
                        Nudge("-5") { change { it.copy(startStage = (it.startStage - 5).coerceAtLeast(1)) } }
                        Nudge("-1") { change { it.copy(startStage = (it.startStage - 1).coerceAtLeast(1)) } }
                        NumberEntry(dev.startStage.toString(),{raw->raw.toIntOrNull()?.let{v->change{it.copy(startStage=v.coerceIn(1,50))}}},Modifier.width(58.dp))
                        Nudge("+1") { change { it.copy(startStage = (it.startStage + 1).coerceAtMost(50)) } }
                        Nudge("+5") { change { it.copy(startStage = (it.startStage + 5).coerceAtMost(50)) } }
                    }
                    Divider()
                    Column(Modifier.padding(horizontal = Space.x4, vertical = Space.x3)) {
                        Text("Start Coins", Type.body, c.ink)
                        Spacer(Modifier.height(Space.x2))
                        NumberEntry(dev.startCoins.toString(),{raw->change{it.copy(startCoins=(raw.toIntOrNull()?:0).coerceIn(0,100_000_000))}},Modifier.fillMaxWidth())
                        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                            COINS.forEach{n->SecondaryButton(n.toString(),{change{it.copy(startCoins=n)}},Modifier.weight(1f))}
                        }
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

                Group("Elites and Perks", footer = "Elites: every enemy that comes is elite. Perks: taken at the start of a trial run.") {
                    ToggleRow("Elite Enemies", dev.elites, { v -> change { it.copy(elites = v) } })
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

                Group("Starting Upgrades", footer = "Levels a trial run begins with. Only for Try a Run With These.") {
                    for (id in 0 until Upgrades.count) {
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



                Group("Tank & scenario",footer="All settings on this page apply only to an unrecorded solo sandbox."){
                    Column(Modifier.padding(Space.x3),verticalArrangement=Arrangement.spacedBy(6.dp)){
                        Text("Starting frame · ${frame.label}",Type.headline,c.ink)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                            for(f in TankEvolution.entries)SecondaryButton(f.label,{frame=f},tint=if(f==frame)c.accent else c.ink)
                        }
                        Text("Starting HP (blank = full)",Type.footnote,c.ink2)
                        NumberEntry(health,{health=it},Modifier.fillMaxWidth())
                    }
                    ToggleRow("All mission cards unlocked",allMissions,{allMissions=it})
                    Column(Modifier.padding(Space.x3),verticalArrangement=Arrangement.spacedBy(6.dp)){
                        Text("Shop reward",Type.headline,c.ink)
                        Segmented(DevReward.entries.map{it.label},reward.ordinal,{reward=DevReward.entries[it]})
                        for(slot in 0..2){
                            val raw=stock[slot];val id=CardCode.item(raw)
                            var rarity by remember(slot){mutableStateOf(CardRarity.COMMON)}
                            Text("Offer ${slot+1}: "+if(id<0)"Random" else Upgrades.of(id).name+" · "+CardCode.tier(raw).label,Type.subhead,c.ink)
                            Segmented(CardRarity.entries.map{it.label},rarity.ordinal,{i->
                                rarity=CardRarity.entries[i]
                                if(id>=0)stock=stock.toMutableList().also{it[slot]=CardCode.pack(id,rarity)}
                            })
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                                SecondaryButton("Random",{stock=stock.toMutableList().also{it[slot]=-1}})
                                for(u in 0 until Upgrades.count)SecondaryButton(Upgrades.of(u).name,{
                                    stock=stock.toMutableList().also{a->
                                        for(i in a.indices)if(CardCode.item(a[i])==u)a[i]=-1
                                        a[slot]=CardCode.pack(u,rarity)
                                    }
                                })
                            }
                        }
                    }
                }
                Group("Skill ownership",footer="Choose any node for a sandbox, even if its prerequisites are not owned."){
                    Row(Modifier.padding(Space.x3),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        SecondaryButton("All skills",{skillMask=SkillGraph.MASK},Modifier.weight(1f))
                        SecondaryButton("Clear skills",{skillMask=0L},Modifier.weight(1f))
                    }
                    for(n in SkillGraph.nodes){
                        ToggleRow(n.name,SkillGraph.owns(skillMask,n.id),{v->skillMask=if(v)skillMask or SkillGraph.bit(n.id) else skillMask and SkillGraph.bit(n.id).inv()},detail=n.description)
                    }
                }
                Group("Enemies and bosses"){
                    DevEnemyOptions(settings,onChange)
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

                Group("Reset", footer = "Reset puts every option above back to normal. Lock hides this page until the version line is tapped exactly five times and the PIN is entered.") {
                    Row(Modifier.padding(Space.x3), horizontalArrangement = Arrangement.spacedBy(Space.x3)) {
                        SecondaryButton("Reset Developer Settings", { change { Dev() };frame=TankEvolution.BASIC;skillMask=0L;health="";stock=listOf(-1,-1,-1);reward=DevReward.AUTO;allMissions=true }, Modifier.weight(1f), tint = c.danger)
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

/** Numeric sandbox controls are intentionally bounded by the game-thread setters. */
@Composable
private fun NumberEntry(value:String,onChange:(String)->Unit,modifier:Modifier=Modifier){
    val c=Theme.colors
    BasicTextField(value,{raw->onChange(raw.filter{it.isDigit()}.take(9))},singleLine=true,
        textStyle=Type.body.copy(color=c.ink),cursorBrush=SolidColor(c.accent),
        modifier=modifier.clip(RoundedCornerShape(8.dp)).background(c.track).padding(8.dp))
}
