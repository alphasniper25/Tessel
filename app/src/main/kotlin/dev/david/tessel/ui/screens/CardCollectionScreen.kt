package dev.david.tessel.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import dev.david.tessel.core.sim.ArsenalFrameStats
import dev.david.tessel.core.sim.ArsenalStat
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
    val tankScroll=rememberLazyListState()
    val cardScroll=rememberLazyListState()
    val missionScroll=rememberLazyListState()
    val detail=frameId>=0 || cardId>=0
    val back={onTap();if(detail){frameId=-1;cardId=-1}else onBack()}
    BackHandler(onBack=back)
    BoxWithConstraints(Modifier.fillMaxSize().background(c.ground).windowInsetsPadding(WindowInsets.displayCutout)){
        val columns=if(maxWidth>=650.dp)2 else 1
        Column(Modifier.fillMaxHeight().widthIn(max=900.dp).fillMaxWidth().align(Alignment.TopCenter).padding(horizontal=Space.x3)){
        Row(Modifier.fillMaxWidth().height(52.dp),verticalAlignment=Alignment.CenterVertically){
            BackButton(if(detail)"Arsenal" else "Home",back)
            Spacer(Modifier.weight(1f))
            Text("LV $level",Type.caption,c.ink2)
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
                LazyColumn(Modifier.weight(1f).fillMaxWidth(),state=tankScroll,verticalArrangement=Arrangement.spacedBy(6.dp)){
                    items(filtered.chunked(columns),key={it.first().ordinal}){row->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            for(frame in row){
                                Row(Modifier.weight(1f).heightIn(min=72.dp).background(c.surface,RoundedCornerShape(8.dp)).clickable{onTap();frameId=frame.ordinal}.padding(horizontal=10.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                    TankPreview(CharacterDef.BALANCED,TankLook(),Modifier.size(60.dp),frame)
                                    Column(Modifier.weight(1f)){
                                        Text(frame.label,Type.headline,c.ink,maxLines=2)
                                        Text(if(frame.secret)"Secret · ${CodexCatalog.role(frame)}" else "Tier ${frame.tier} · ${CodexCatalog.role(frame)}",Type.caption,if(frame.secret)c.coin else c.ink2,maxLines=2)
                                    }
                                    Chevron(c.ink3)
                                }
                            }
                            if(row.size<columns)Spacer(Modifier.weight(1f))
                        }
                    }
                    if(filtered.isEmpty())item{Text("No tanks match these filters.",Type.body,c.ink2)}
                    item{Spacer(Modifier.height(12.dp))}
                }
            }else{
                FilterBar(listOf("All","Unlocked","Locked","Legacy"),status,{status=it})
                Row(Modifier.fillMaxWidth().padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
                    Text(if(moreFilters)"Compatible builds" else "Filter by build",Type.footnote,c.ink2,Modifier.weight(1f))
                    Text(if(moreFilters)"Hide" else "Show",Type.subhead,c.accent,Modifier.clickable{if(moreFilters)cardBuild=0;moreFilters=!moreFilters}.padding(horizontal=12.dp,vertical=8.dp))
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
                LazyColumn(Modifier.weight(1f).fillMaxWidth(),state=if(tab==2)missionScroll else cardScroll,verticalArrangement=Arrangement.spacedBy(6.dp)){
                    items(cards.chunked(columns),key={it.first()}){row->
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
                            if(row.size<columns)Spacer(Modifier.weight(1f))
                        }
                    }
                    if(cards.isEmpty())item{Text("No cards match these filters.",Type.body,c.ink2)}
                    item{Spacer(Modifier.height(12.dp))}
                }
            }
        }
        }
    }
}

@Composable
private fun TankDetails(frame:TankEvolution,modifier:Modifier) {
    val c=Theme.colors
    val spec=remember(frame){ArsenalFrameStats.forFrame(frame)}
    LazyColumn(modifier.fillMaxWidth().testTag("tank-details"),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=18.dp)){
        item {
            Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text(frame.label,Type.largeTitle,c.ink)
                Text(CodexCatalog.role(frame),Type.subhead,c.accent)
                Text(frame.detail,Type.body,c.ink2)
            }
        }
        item { TankDiagram(frame,spec.primary) }
        item { Text("Base values before difficulty, character passives and purchased upgrades.",Type.caption,c.ink3) }
        item { CodexCombatPreview(frame) }
        item {
            Text("Specifications",Type.headline,c.ink)
            Spacer(Modifier.height(5.dp))
            for(stat in spec.secondary){DetailStat(stat.label,stat.value);Divider()}
            DetailStat("Limitation",spec.limitation)
        }
    }
}

/** A common schematic template; values and labels are selected by actual frame type. */
@Composable
private fun TankDiagram(frame:TankEvolution,stats:List<ArsenalStat>){
    val c=Theme.colors
    BoxWithConstraints(Modifier.fillMaxWidth().height(176.dp).background(c.surface,RoundedCornerShape(10.dp)).testTag("tank-diagram")){
        Canvas(Modifier.fillMaxSize()){
            val w=size.width;val h=size.height
            val grid=22.dp.toPx()
            var x=grid
            while(x<w){drawLine(c.ink.copy(alpha=.035f),Offset(x,0f),Offset(x,h));x+=grid}
            var y=grid
            while(y<h){drawLine(c.ink.copy(alpha=.035f),Offset(0f,y),Offset(w,y));y+=grid}
            for(row in 0..1){
                val sy=h*(if(row==0).27f else .73f)
                val ey=h*(if(row==0).42f else .58f)
                for(left in listOf(true,false)){
                    val start=Offset(w*(if(left).275f else .725f),sy)
                    val elbow=Offset(w*(if(left).335f else .665f),sy)
                    val end=Offset(w*(if(left).43f else .57f),ey)
                    drawLine(c.ink2.copy(alpha=.65f),start,elbow,1.dp.toPx(),StrokeCap.Round)
                    drawLine(c.ink2.copy(alpha=.65f),elbow,end,1.dp.toPx(),StrokeCap.Round)
                    drawCircle(c.accent,2.dp.toPx(),end)
                }
            }
        }
        Row(Modifier.fillMaxSize().padding(horizontal=7.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(.31f).fillMaxHeight(),verticalArrangement=Arrangement.SpaceAround){
                DiagramValue(stats[0],TextAlign.Start)
                DiagramValue(stats[1],TextAlign.Start)
            }
            Box(Modifier.weight(.38f).fillMaxHeight(),contentAlignment=Alignment.Center){
                TankPreview(CharacterDef.BALANCED,TankLook(),Modifier.size(minOf(150.dp,maxWidth*.38f)),frame)
            }
            Column(Modifier.weight(.31f).fillMaxHeight(),verticalArrangement=Arrangement.SpaceAround){
                DiagramValue(stats[2],TextAlign.End)
                DiagramValue(stats[3],TextAlign.End)
            }
        }
    }
}
@Composable
private fun DiagramValue(stat:ArsenalStat,align:TextAlign){
    val c=Theme.colors
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(1.dp)){
        Text(stat.label,Type.caption,c.ink2,Modifier.fillMaxWidth(),align=align,maxLines=2)
        Text(stat.value,Type.headline.merge(Type.numbers),c.ink,Modifier.fillMaxWidth(),align=align,maxLines=2)
    }
}

@Composable
private fun CardDetails(id:Int,ledger:Ledger,modifier:Modifier){
    val c=Theme.colors;val def=Upgrades.of(id);val mission=CardMission.forUpgrade(id)
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical=Space.x2),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text(def.name,Type.largeTitle,c.ink)
        Text(def.blurb,Type.body,c.ink2)
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
    Row(Modifier.fillMaxWidth().padding(horizontal=5.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
        Text(label,Type.footnote,c.ink2,Modifier.weight(.85f))
        Text(value,Type.subhead,c.ink,Modifier.weight(1.15f))
    }
}
