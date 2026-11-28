package dev.david.tessel.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.david.tessel.core.content.AccountProgress
import dev.david.tessel.core.content.GarageCatalog
import dev.david.tessel.core.stats.Ledger
import dev.david.tessel.ui.components.*
import dev.david.tessel.ui.theme.*

@Composable
private fun ProfileMark(modifier:Modifier=Modifier){
    val c=Theme.colors
    Canvas(modifier){
        val r=size.minDimension
        drawCircle(c.ink,r*.14f,Offset(size.width*.5f,size.height*.32f),style=Stroke(r*.065f))
        drawArc(c.ink,195f,150f,false,Offset(size.width*.22f,size.height*.50f),Size(r*.56f,r*.44f),style=Stroke(r*.065f,cap=StrokeCap.Round))
    }
}
@Composable
fun ProfileButton(name:String,onClick:()->Unit){
    val c=Theme.colors
    Row(Modifier.heightIn(min=48.dp).clickable(role=Role.Button,onClick=onClick)
        .semantics{contentDescription="Player profile"}.padding(horizontal=8.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
        Text(name,Type.footnote,c.ink2,Modifier.widthIn(max=112.dp),maxLines=1)
        Box(Modifier.size(42.dp).background(c.surface,CircleShape).border(1.dp,c.line,CircleShape),contentAlignment=Alignment.Center){
            ProfileMark(Modifier.size(28.dp))
        }
    }
}

@Composable
fun PlayerProfileScreen(initialName:String,ledger:Ledger,onName:(String)->Unit,onSettings:()->Unit,onStats:()->Unit,onBack:()->Unit,onDev:(()->Unit)?=null){
    val c=Theme.colors
    var username by rememberSaveable(initialName){mutableStateOf(initialName)}
    var saved by rememberSaveable{mutableStateOf(false)}
    val xp=AccountProgress.xp(ledger.totals)
    val level=AccountProgress.level(xp)
    Column(Modifier.fillMaxSize().background(c.ground).windowInsetsPadding(WindowInsets.displayCutout).padding(horizontal=Space.x4)){
        Row(Modifier.fillMaxWidth().height(52.dp),verticalAlignment=Alignment.CenterVertically){
            BackButton("Home",onBack)
            Spacer(Modifier.weight(1f))
            Text("LOCAL",Type.caption,c.accent)
        }
        Divider()
        Row(Modifier.weight(1f).fillMaxWidth().padding(vertical=Space.x2),horizontalArrangement=Arrangement.spacedBy(Space.x4)){
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(9.dp)){
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    Box(Modifier.size(70.dp).background(c.surface,CircleShape).border(1.dp,c.line,CircleShape),contentAlignment=Alignment.Center){ProfileMark(Modifier.size(48.dp))}
                    Column(Modifier.weight(1f)){
                        Text(initialName,Type.title3,c.ink,maxLines=2)
                        Text("Level $level · $xp XP",Type.subhead,c.ink2)
                    }
                }
                Text("Player name",Type.footnote,c.ink2)
                BasicTextField(username,{username=it.replace('|',' ').filterNot{ch->ch.isISOControl()}.take(18);saved=false},singleLine=true,
                    textStyle=Type.body.copy(color=c.ink),cursorBrush=SolidColor(c.accent),
                    modifier=Modifier.fillMaxWidth().background(c.surface,RoundedCornerShape(8.dp)).border(1.dp,c.line,RoundedCornerShape(8.dp)).padding(12.dp))
                PrimaryButton(if(saved)"Saved" else "Save name",{onName(username.trim());saved=true},Modifier.fillMaxWidth(),enabled=username.isNotBlank())
                Text("Used for local multiplayer. Your progress is saved on this device.",Type.footnote,c.ink2)
            }
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(7.dp)){
                Text("Account",Type.headline,c.ink)
                // Deliberate placeholders: do not ask for credentials or imply authentication.
                Row(Modifier.fillMaxWidth().background(c.surface,RoundedCornerShape(8.dp)).padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                    Box(Modifier.size(32.dp),contentAlignment=Alignment.Center){Text("G",Type.title2,c.ink2)}
                    Column(Modifier.weight(1f).padding(start=8.dp)){
                        Text("Google sign-in",Type.subhead,c.ink);Text("Not connected · planned",Type.footnote,c.ink3)
                    }
                }
                Row(Modifier.fillMaxWidth().background(c.surface,RoundedCornerShape(8.dp)).padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                    Canvas(Modifier.size(32.dp)){
                        val p=Path().apply{moveTo(size.width*.24f,size.height*.13f);lineTo(size.width*.82f,size.height*.5f);lineTo(size.width*.24f,size.height*.87f);close()}
                        drawPath(p,c.ink2,style=Stroke(2.dp.toPx(),cap=StrokeCap.Round))
                    }
                    Column(Modifier.weight(1f).padding(start=8.dp)){
                        Text("Google Play",Type.subhead,c.ink);Text("Not published · planned",Type.footnote,c.ink3)
                    }
                }
                Text("Settings & progress",Type.headline,c.ink)
                SecondaryButton("Settings",onSettings,Modifier.fillMaxWidth())
                if(onDev!=null)SecondaryButton("Developer Options",onDev,Modifier.fillMaxWidth())
                SecondaryButton("Statistics",onStats,Modifier.fillMaxWidth())
                Text("${GarageCatalog.earned(ledger.totals)} lifetime garage credits earned",Type.footnote,c.ink2)
            }
        }
    }
}
