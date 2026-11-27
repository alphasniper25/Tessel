package dev.david.tessel.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import dev.david.tessel.core.content.*
import dev.david.tessel.core.stats.Ledger
import dev.david.tessel.platform.game.TankArt
import dev.david.tessel.platform.store.Settings
import dev.david.tessel.ui.components.*
import dev.david.tessel.ui.theme.*

@Composable
fun TankPreview(character: CharacterDef, look: TankLook, modifier: Modifier = Modifier) {
    val colors = Theme.colors
    val art = remember { TankArt() }
    Canvas(modifier) {
        art.draw(drawContext.canvas.nativeCanvas, size.width * .46f, size.height * .5f,
            minOf(size.width,size.height)*.30f, .8f, -.6f, character, look,
            if (colors.dark) dev.david.tessel.design.mix(colors.ink.toArgb(), colors.ground.toArgb(), .16f) else colors.ink.toArgb(),
            if (colors.dark) colors.ink.toArgb() else dev.david.tessel.design.mix(colors.ink.toArgb(), colors.ground.toArgb(), .32f), colors.ground.toArgb())
    }
}

@Composable
fun CharacterPreview(character: CharacterDef, look: TankLook, difficulty: Difficulty, modifier: Modifier = Modifier) {
    val c = Theme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TankPreview(character, look, Modifier.size(66.dp))
        Column(Modifier.weight(1f)) {
            Text(character.label, Type.headline, c.ink)
            Text("HP ${(Balance.PLAYER_HP + difficulty.bonusHp + character.extraHp).coerceAtLeast(1)}  ·  Damage ${(character.damageScale*100).toInt()}%", Type.footnote, c.ink)
            Text("Speed ${(character.speedScale*100).toInt()}%  ·  Ram ${character.ramBase}", Type.footnote, c.ink)
            Text(character.description, Type.footnote, c.ink2)
        }
    }
}

@Composable
fun GarageScreen(settings: Settings, ledger: Ledger, onChange: ((Settings) -> Settings) -> Unit, onBack: () -> Unit) {
    val c = Theme.colors
    var tab by remember { mutableIntStateOf(0) }
    var previewId by remember { mutableIntStateOf(-1) }
    val slot = CosmeticSlot.entries[tab]
    val selected = GarageCatalog.items.getOrNull(previewId)?.takeIf { it.slot == slot }
    val balance = GarageCatalog.balance(ledger.totals, settings.cosmeticOwned)
    val look = if (selected == null) settings.tankLook else settings.tankLook.equip(selected.id, settings.cosmeticOwned or (1 shl selected.id))
    Column(Modifier.fillMaxSize().background(c.ground).windowInsetsPadding(WindowInsets.displayCutout).padding(horizontal=16.dp)) {
        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton("Home", onBack)
            Spacer(Modifier.weight(1f)); Text("Tank Garage", Type.title2, c.ink); Spacer(Modifier.weight(1f))
            Text("$balance credits", Type.headline, c.coin)
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tank type", Type.headline, c.ink)
            Segmented(CharacterDef.entries.map { it.label }, settings.tankClass.ordinal, { i -> onChange { it.copy(tankClass=CharacterDef.fromId(i)) } })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TankPreview(settings.tankClass, look, Modifier.width(150.dp).height(112.dp).background(c.surface, RoundedCornerShape(12.dp)))
                Column(Modifier.weight(1f)) {
                    Text(settings.tankClass.label, Type.title3, c.ink)
                    Text("HP ${(Balance.PLAYER_HP + settings.difficulty.bonusHp + settings.tankClass.extraHp).coerceAtLeast(1)} · Damage ${(settings.tankClass.damageScale*100).toInt()}% · Speed ${(settings.tankClass.speedScale*100).toInt()}% · Ram ${settings.tankClass.ramBase}", Type.subhead, c.ink)
                    Text(settings.tankClass.description, Type.footnote, c.ink2)
                    Text("Cosmetics do not change stats. Credits are earned from finished runs, kills, survival and bosses.", Type.footnote, c.ink3)
                }
            }
            Segmented(listOf("Body", "Turret", "Paint", "Skin"), tab, { tab=it; previewId=-1 })
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (item in GarageCatalog.bySlot[slot.ordinal]) {
                    val isOwned=GarageCatalog.owned(settings.cosmeticOwned,item.id)
                    val equipped=settings.tankLook.get(slot)==item.id
                    val isSelected=selected?.id==item.id
                    Column(Modifier.width(110.dp).border(if(isSelected || equipped) 2.dp else 1.dp, if(isSelected || equipped) c.ink else c.track, RoundedCornerShape(10.dp))
                        .background(c.surface,RoundedCornerShape(10.dp)).clickable { previewId=item.id }.padding(6.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                        TankPreview(settings.tankClass, settings.tankLook.equip(item.id, settings.cosmeticOwned or (1 shl item.id)), Modifier.size(54.dp))
                        Text(item.label, Type.subhead, c.ink, maxLines=1)
                        Text(if(equipped) "Equipped" else if(isOwned) "Owned" else "${item.cost} credits", Type.footnote, c.ink2)
                    }
                }
            }
            if(selected!=null) {
                val owned=GarageCatalog.owned(settings.cosmeticOwned,selected.id)
                PrimaryButton(if(owned) "Equip ${selected.label}" else "Buy & equip · ${selected.cost}", {
                    onChange { old -> val mask=GarageCatalog.purchase(ledger.totals,old.cosmeticOwned,selected.id)
                        old.copy(cosmeticOwned=mask,tankLook=old.tankLook.equip(selected.id,mask)) }
                }, Modifier.fillMaxWidth(), enabled=owned || balance>=selected.cost)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
