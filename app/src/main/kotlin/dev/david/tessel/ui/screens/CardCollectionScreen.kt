package dev.david.tessel.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.david.tessel.core.content.*
import dev.david.tessel.core.stats.Ledger
import dev.david.tessel.ui.components.*
import dev.david.tessel.ui.theme.*
import java.util.Locale

@Composable
private fun FilterBar(options:List<String>,selected:Int,choose:(Int)->Unit) {
    val c=Theme.colors
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
        options.forEachIndexed { index,label ->
            val shape=RoundedCornerShape(9.dp)
            Box(Modifier.background(if(index==selected)c.pill else c.surface,shape).border(1.dp,if(index==selected)c.accent else c.line,shape)
                .clickable { choose(index) }.padding(horizontal=12.dp,vertical=9.dp)){
                Text(label,Type.subhead,if(index==selected)c.ink else c.ink2)
            }
        }
    }
}
private fun f(v:Float)=String.format(Locale.US,"%.2f",v).trimEnd('0').trimEnd('.')
private fun familyName(f:CardFamily)=f.name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
fun CardCollectionScreen(ledger:Ledger,onBack:()->Unit,onTap:()->Unit={}) {
    val c=Theme.colors
    val totals=ledger.totals
    val xp=AccountProgress.xp(totals)
    val level=AccountProgress.level(xp)
    var tab by rememberSaveable{mutableIntStateOf(0)}
    var build by rememberSaveable{mutableIntStateOf(0)}
    var tier by rememberSaveable{mutableIntStateOf(0)}
    var status by rememberSaveable{mutableIntStateOf(0)}
    var family by rememberSaveable{mutableIntStateOf(0)}
    var cardBuild by rememberSaveable{mutableIntStateOf(0)}
    var moreFilters by rememberSaveable{mutableStateOf(false)}
    var frameId by rememberSaveable{mutableIntStateOf(-1)}
    var cardId by rememberSaveable{mutableIntStateOf(-1)}
    val detail=frameId>=0 || cardId>=0
    val back={onTap();if(detail){frameId=-1;cardId=-1}else onBack()}
    BackHandler(onBack=back)
    Column(Modifier.fillMaxSize().background(c.ground).windowInsetsPadding(WindowInsets.displayCutout).padding(horizontal=Space.x4)){
        Row(Modifier.fillMaxWidth().height(52.dp),verticalAlignment=Alignment.CenterVertically){
            BackButton(if(detail)"Arsenal" else "Home",back)
            Spacer(Modifier.weight(1f))
            Text(if(frameId>=0)TankEvolution.fromId(frameId).label else if(cardId>=0)Upgrades.of(cardId).name else "Arsenal · Tanks & Cards",Type.title2,c.ink)
            Spacer(Modifier.weight(1f))
            Text("LV $level",Type.caption,c.accent)
        }
        if(frameId>=0){
            TankDetails(TankEvolution.fromId(frameId),Modifier.weight(1f))
        }else if(cardId>=0){
            CardDetails(cardId,ledger,Modifier.weight(1f))
        }else{
            Segmented(listOf("Tanks","Cards","Missions"),tab,{tab=it;onTap()})
            Spacer(Modifier.height(6.dp))
            if(tab==0){
                FilterBar(BuildFilter.entries.map{it.label},build,{build=it})
                Spacer(Modifier.height(4.dp))
                FilterBar(listOf("All tiers","Basic","Tier 1","Tier 2","Tier 3","Tier 4"),tier,{tier=it})
                val filtered=TankEvolution.entries.filter { BuildFilter.entries[build].matches(it) && (tier==0 || it.tier==tier-1) }
                Text("${filtered.size} / ${TankEvolution.entries.size} tanks · Tap a tank to inspect",Type.footnote,c.ink2,Modifier.padding(vertical=4.dp))
                LazyColumn(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    items(filtered.chunked(2)){row->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            for(frame in row){
                                Row(Modifier.weight(1f).background(c.surface,RoundedCornerShape(10.dp)).clickable{onTap();frameId=frame.ordinal}.padding(8.dp),verticalAlignment=Alignment.CenterVertically){
                                    TankPreview(CharacterDef.BALANCED,TankLook(),Modifier.size(60.dp),frame)
                                    Column(Modifier.weight(1f)){
                                        Text(frame.label,Type.headline,c.ink,maxLines=2)
                                        Text("Tier ${frame.tier} · ${CodexCatalog.role(frame)}",Type.caption,if(frame.secret)c.coin else c.ink2,maxLines=2)
                                    }
                                    Chevron(c.ink3)
                                }
                            }
                            if(row.size==1)Spacer(Modifier.weight(1f))
                        }
                    }
                    if(filtered.isEmpty())item{Text("No tanks match these filters.",Type.body,c.ink2)}
                    item{Spacer(Modifier.height(12.dp))}
                }
            }else{
                Row(verticalAlignment=Alignment.CenterVertically){
                    Box(Modifier.weight(1f)){FilterBar(listOf("All","Unlocked","Locked","Legacy"),status,{status=it})}
                    SecondaryButton(if(moreFilters)"Hide build" else "Build filter",{if(moreFilters)cardBuild=0;moreFilters=!moreFilters})
                }
                if(moreFilters){FilterBar(BuildFilter.entries.map{it.label},cardBuild,{cardBuild=it})}
                Spacer(Modifier.height(4.dp))
                FilterBar(listOf("All families")+CardFamily.entries.map(::familyName),family,{family=it})
                val cards=(0 until Upgrades.count).filter { id ->
                    val mission=CardMission.forUpgrade(id)
                    val legacy=CodexCatalog.legacy(id)
                    val unlocked=!legacy && (mission==null || mission.unlocked(totals))
                    (tab!=2 || mission!=null) &&
                    (family==0 || Upgrades.of(id).family.ordinal==family-1) &&
                    (cardBuild==0 || CodexCatalog.compatible(id,BuildFilter.entries[cardBuild])) &&
                    when(status){1->unlocked;2->!legacy&&!unlocked;3->legacy;else->true}
                }
                Text(if(tab==2)"${CardMission.entries.count{it.unlocked(totals)}} / ${CardMission.entries.size} missions unlocked · rewards enter new runs" else "${cards.size} cards · Tap for stats and compatible builds",Type.footnote,c.ink2,Modifier.padding(vertical=4.dp))
                LazyColumn(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    items(cards.chunked(2)){row->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            for(id in row){
                                val def=Upgrades.of(id);val mission=CardMission.forUpgrade(id)
                                val legacy=CodexCatalog.legacy(id);val locked=mission!=null&&!mission.unlocked(totals)
                                Column(Modifier.weight(1f).background(c.surface,RoundedCornerShape(10.dp)).clickable{onTap();cardId=id}.padding(11.dp)){
                                    Text(when{legacy->"LEGACY";locked->"LOCKED";mission!=null->mission.rarity.label.uppercase();Upgrades.firstStage(id)==15->"STAGE 15+";else->familyName(def.family).uppercase()},Type.caption,if(locked||legacy)c.ink3 else c.accent)
                                    Row(verticalAlignment=Alignment.CenterVertically){
                                        Text(def.name,Type.headline,c.ink,Modifier.weight(1f),maxLines=2);Chevron(c.ink3)
                                    }
                                    Text(def.blurb,Type.footnote,c.ink2,maxLines=2)
                                    if(mission!=null) {
                                        if(tab==2)Text(mission.goal,Type.footnote,c.ink2)
                                        val progress=mission.progress(totals).coerceAtMost(mission.target)
                                        Row(Modifier.fillMaxWidth().padding(top=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                            Box(Modifier.weight(1f).height(4.dp).background(c.track,RoundedCornerShape(2.dp))){
                                                Box(Modifier.fillMaxWidth(progress.toFloat()/mission.target).height(4.dp).background(c.accent,RoundedCornerShape(2.dp)))
                                            }
                                            Text("$progress / ${mission.target}",Type.caption,c.ink2)
                                        }
                                    }
                                }
                            }
                            if(row.size==1)Spacer(Modifier.weight(1f))
                        }
                    }
                    if(cards.isEmpty())item{Text("No cards match these filters.",Type.body,c.ink2)}
                    item{Spacer(Modifier.height(12.dp))}
                }
            }
        }
    }
}

@Composable
private fun TankDetails(frame:TankEvolution,modifier:Modifier) {
    val c=Theme.colors
    Row(modifier.fillMaxWidth().padding(vertical=Space.x2),horizontalArrangement=Arrangement.spacedBy(Space.x4)){
        Column(Modifier.weight(1.05f).verticalScroll(rememberScrollState())) {
            CodexCombatPreview(frame)
            Text("Demonstration uses the real tank and enemy simulation. No progress is recorded.",Type.footnote,c.ink3)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Text(CodexCatalog.role(frame),Type.title3,c.accent)
            Text(frame.detail,Type.subhead,c.ink)
            DetailStat("Unlock",if(frame.tier==0)"Starting frame" else "Stage ${CodexCatalog.unlockStage(frame)}")
            if(frame.parent>=0)DetailStat("Evolves from",TankEvolution.fromId(frame.parent).label)
            DetailStat("Movement","${f(Balance.PLAYER_SPEED*frame.move)} units/s")
            DetailStat("Base hull bonus","+${frame.hullBonus} HP")
            val drone=frame.droneSpec
            if(drone!=null){
                DetailStat("Drone capacity","${drone.cap} units")
                DetailStat("Drone health","${drone.hp} HP each")
                DetailStat("Impact damage",f(Balance.BULLET_DAMAGE*frame.damage*drone.impact))
                DetailStat("Replacement","${f(drone.respawn)} s")
                DetailStat("Command range",f(drone.reach))
                DetailStat("Defense","Absorb hostile shots; each block costs 1 drone HP")
                DetailStat("Control","Aim to command the wing; release to guard")
                DetailStat("Limitation","Destroyable units; serial replacement")
            }else if(frame.melee){
                DetailStat("Base impact",f(frame.ram))
                DetailStat("Shield recharge","${f(frame.shieldSeconds)} s")
                DetailStat("Regeneration","${frame.regenSeconds}s per HP")
                DetailStat("Limitation","Must make contact")
            }else{
                DetailStat("Barrels",frame.barrels.toString())
                DetailStat("Damage / shot",f(Balance.BULLET_DAMAGE*frame.damage))
                DetailStat("Volleys / second",f(Balance.FIRE_RATE*frame.rate))
                DetailStat("Range","${f(Balance.BULLET_RANGE*frame.reach)} units")
                DetailStat("Extra pierce",frame.extraPierce.toString())
                DetailStat("Limitation",when{BuildFilter.CANNON.matches(frame)->"Slow reload";BuildFilter.SNIPER.matches(frame)->"Precise aim";BuildFilter.RAPID.matches(frame)->"Lower per-shot power";else->"Positioning and aim"})
            }
            Text("Base values before character passives and purchased upgrades.",Type.footnote,c.ink3)
        }
    }
}
@Composable
private fun CardDetails(id:Int,ledger:Ledger,modifier:Modifier){
    val c=Theme.colors;val def=Upgrades.of(id);val mission=CardMission.forUpgrade(id)
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical=Space.x2),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text(def.blurb,Type.title3,c.ink)
        DetailStat("Family",familyName(def.family))
        DetailStat("Run rank limit",Upgrades.runCap(id).toString())
        if(CodexCatalog.legacy(id)){
            Text("Legacy only: existing old saves may contain this card, but it no longer appears in new shops.",Type.body,c.ink2)
        }else{
            val stage=CodexCatalog.firstStage(id)
            DetailStat("First eligible stage",stage.toString())
            DetailStat("First-rank base price","${Upgrades.price(id,0,stage)} coins (before rarity, character and skill discounts)")
            Text("Rarity & rank gains",Type.headline,c.ink)
            for(rarity in CardRarity.entries.filter{mission==null||it==mission.rarity})
                DetailStat(rarity.label,"Up to ${rarity.levels} rank(s) · ${rarity.weight}% initial base roll")
            if(mission!=null){
                Text(if(mission.unlocked(ledger.totals))"Mission complete" else "Unlock mission",Type.headline,c.accent)
                Text(mission.goal,Type.body,c.ink)
                Text("${mission.progress(ledger.totals).coerceAtMost(mission.target)} / ${mission.target}",Type.subhead,c.ink2)
            }
            Text("Compatible tank families",Type.headline,c.ink)
            Text(BuildFilter.entries.filter{it!=BuildFilter.ALL && CodexCatalog.compatible(id,it)}.joinToString(" · "){it.label},Type.body,c.ink2)
            Text("Exact compatible frames",Type.headline,c.ink)
            Text(TankEvolution.entries.filter{Upgrades.offered(id,it)}.joinToString(" · "){it.label},Type.footnote,c.ink2)
        }
    }
}
@Composable
private fun DetailStat(label:String,value:String){
    val c=Theme.colors
    Row(Modifier.fillMaxWidth().background(c.surface,RoundedCornerShape(8.dp)).padding(horizontal=10.dp,vertical=7.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
        Text(label,Type.footnote,c.ink2,Modifier.weight(.85f))
        Text(value,Type.subhead,c.ink,Modifier.weight(1.15f))
    }
}
