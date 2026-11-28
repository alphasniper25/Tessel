package dev.david.tessel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.david.tessel.core.content.Perks
import dev.david.tessel.core.content.SkillTree
import dev.david.tessel.platform.game.PerkOffer
import dev.david.tessel.ui.components.*
import dev.david.tessel.ui.theme.*

/** A full-screen tree, also available before starting a run. Only the host's current offer can be purchased. */
@Composable
fun SkillTreeScreen(clearedStage:Int, mask:Int, available:List<PerkOffer>, canSpend:Boolean,
    onSpend:(Int)->Unit, onBack:()->Unit, backLabel:String="Home") {
    val c=Theme.colors
    val earned=SkillTree.earned(clearedStage)
    val points=SkillTree.unspent(clearedStage,mask)
    val tints=listOf(c.accent,c.perk,c.coin)
    var pendingMask by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(mask,pendingMask) {
        if(pendingMask!=null) {
            if(pendingMask==mask) delay(1500L)
            pendingMask=null
        }
    }
    Column(Modifier.fillMaxSize().background(c.ground).pointerInput(Unit){detectTapGestures { }}.windowInsetsPadding(WindowInsets.displayCutout).padding(horizontal=Space.x4)) {
        Row(Modifier.fillMaxWidth().height(52.dp),verticalAlignment=Alignment.CenterVertically) {
            BackButton(backLabel,onBack)
            Spacer(Modifier.weight(1f))
            Text("Skill Tree",Type.title2,c.ink)
            Spacer(Modifier.weight(1f))
            Text("$points available",Type.headline,c.accent)
        }
        Text("RUN PROGRESSION · $earned / 7 points earned",Type.caption,c.ink2)
        Text(SkillTree.nextMilestone(clearedStage)?.let { "Next point at stage $it · Unlock a branch rank at a shop." } ?: "All seven milestones reached.",Type.footnote,c.ink2)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical=Space.x2)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                for(branch in 0..2) {
                    val tint=tints[branch]
                    val owned=SkillTree.rank(mask,branch)
                    Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(SkillTree.name(branch).uppercase(),Type.headline,tint)
                        Text(listOf("Free rerolls","Rarity odds","Shop discount")[branch],Type.caption,c.ink2)
                        for(rank in 1..3) {
                            if(rank>1) Box(Modifier.width(2.dp).height(8.dp).background(if(owned>=rank-1)tint else c.track))
                            val id=SkillTree.node(branch,rank)
                            val offer=available.firstOrNull{it.id==id}
                            val unlocked=rank<=owned
                            val ready=canSpend && points>0 && offer!=null && rank==owned+1 && pendingMask!=mask
                            val shape=RoundedCornerShape(10.dp)
                            Column(Modifier.fillMaxWidth().heightIn(min=78.dp).border(if(ready)2.dp else 1.dp,if(unlocked||ready)tint else c.line,shape)
                                .background(if(unlocked)tint.copy(alpha=.12f) else c.surface,shape)
                                .clickable(enabled=ready,role=Role.Button){if(pendingMask!=mask)offer?.let{pendingMask=mask;onSpend(it.slot)}}.padding(9.dp)) {
                                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                                    Text(Perks.of(id).name,Type.subhead,c.ink,Modifier.weight(1f))
                                    Text(if(unlocked)"OWNED" else if(ready)"UNLOCK" else if(rank==owned+1)"NEXT" else "LOCKED",Type.caption,if(unlocked||ready)tint else c.ink3)
                                }
                                Text(when(branch){0->"$rank free reroll(s) per shop";1->"Epic ${7+2*rank}% · Legendary ${1+2*rank}%";else->"${5*rank}% upgrade discount"},Type.footnote,c.ink2)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(Space.x2))
            Text("Milestones: "+SkillTree.schedule().joinToString("  ·  "),Type.footnote,c.ink2)
            Text("Seven points, nine nodes. Your choices reset each run; no account grind. Locked cards keep their quoted prices. Rarity numbers are initial roll probabilities.",Type.footnote,c.ink3)
            if(!canSpend && points>0) Text("Spend points when the shop offers your next skill.",Type.footnote,c.accent)
        }
        if(backLabel=="Shop") PrimaryButton(if(points==0)"Return to Shop" else "Back to Shop",onBack,Modifier.fillMaxWidth().padding(bottom=6.dp))
    }
}
