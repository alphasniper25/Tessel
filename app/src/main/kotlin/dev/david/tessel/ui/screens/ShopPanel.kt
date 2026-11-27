package dev.david.tessel.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.david.tessel.core.content.CardRarity
import dev.david.tessel.platform.game.ShopOffer
import dev.david.tessel.platform.game.ShopView
import dev.david.tessel.ui.components.CoinAmount
import dev.david.tessel.ui.components.LevelPips
import dev.david.tessel.ui.components.PrimaryButton
import dev.david.tessel.ui.components.Text
import dev.david.tessel.ui.components.pressScale
import dev.david.tessel.ui.theme.Corner
import dev.david.tessel.ui.theme.Space
import dev.david.tessel.ui.theme.Theme
import dev.david.tessel.ui.theme.Type

private const val COLUMNS = 3

/** Between stages: buy single-use cards, refresh unlocked slots, lock cards to save them. */
@Composable
fun ShopPanel(
    shop: ShopView,
    vibrate: Boolean,
    onBuy: (Int) -> Unit,
    onHeal: () -> Unit,
    onReroll: () -> Unit,
    onNext: () -> Unit,
    onPerk: (Int) -> Unit = {},
    onLock: (Int, Boolean) -> Unit = { _, _ -> },
) {
    val c = Theme.colors
    val view = LocalView.current
    val tap = { if (vibrate) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }

    Column(
        Modifier
            .padding(start = Space.x6, end = Space.x6, top = Space.x2, bottom = Space.x2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Stage ${shop.stage} cleared", Type.title3, c.ink)
                val team = if (shop.coop) "   ·   ${shop.readyCount} of ${shop.teamSize} ready" else ""
                Text("Health ${shop.hp} of ${shop.maxHp}$team", Type.footnote.merge(Type.numbers), c.ink2)
            }
            CoinAmount(shop.coins, style = Type.title3)
        }
        Spacer(Modifier.height(Space.x2))

        Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())) {
        // After a boss: one perk to take before the shop can be left.
        if (shop.perkOffers.isNotEmpty()) {
            Text("Choose a Perk", Type.headline, c.ink)
            Text("It stays for the rest of the run.", Type.footnote, c.ink3)
            Spacer(Modifier.height(Space.x2))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (offer in shop.perkOffers) {
                    Tile(
                        name = offer.name,
                        blurb = offer.blurb,
                        tint = c.perk,
                        enabled = true,
                        faded = false,
                        onClick = {
                            tap()
                            onPerk(offer.slot)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Spacer(Modifier.weight(1f))
                        Text("Take", Type.subhead, c.ink)
                    }
                }
                repeat(COLUMNS - shop.perkOffers.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(Space.x4))
        }

        if (shop.perkOffers.isEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (offer in shop.offers) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OfferTile(offer, Modifier.fillMaxWidth(), enabled = !shop.selfReady) { tap(); onBuy(offer.slot) }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CompactAction(if (offer.locked) "Locked • Unlock" else "Lock", Modifier.weight(1f), !offer.empty && !offer.maxed && !shop.selfReady, offer.locked) { tap(); onLock(offer.slot, !offer.locked) }
                            CompactAction(if (offer.empty) "Sold" else if (offer.maxed) "Maxed" else "Buy ${offer.price}", Modifier.weight(1f), offer.affordable && !offer.maxed && !offer.empty && !shop.selfReady, false) { tap(); onBuy(offer.slot) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        }
        dev.david.tessel.ui.components.Divider()
        Spacer(Modifier.height(Space.x2))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CompactAction("Heal +${shop.healAmount} · ${shop.healPrice} coins · ${shop.healsLeft} left", Modifier.weight(1.5f), shop.canHeal && !shop.selfReady, false) { tap(); onHeal() }
            CompactAction("Refresh · ${shop.rerollPrice}", Modifier.weight(1f), shop.canReroll && !shop.selfReady && shop.perkOffers.isEmpty(), false) { tap(); onReroll() }
            val choosing = shop.perkOffers.isNotEmpty()
            PrimaryButton(if (shop.selfReady) "Waiting" else if (shop.coop) "Ready" else "Next Stage", onNext, Modifier.weight(1f), enabled = !choosing && !shop.selfReady)
        }
    }
}

@Composable
private fun OfferTile(offer: ShopOffer, modifier: Modifier, enabled: Boolean, onBuy: () -> Unit) {
    val c = Theme.colors
    Tile(
        name = if (offer.empty) "Empty" else offer.name,
        eyebrow = if (offer.empty) null else offer.rarity.label.uppercase(),
        blurb = offer.blurb,
        tint = if (offer.empty) null else when (offer.rarity) {
            CardRarity.COMMON -> null
            CardRarity.UNCOMMON -> if (c.dark) Color(0xFF76CF8A) else Color(0xFF007834)
            CardRarity.RARE -> if (c.dark) Color(0xFF8EB5FF) else Color(0xFF3B5EB2)
            CardRarity.EPIC -> if (c.dark) Color(0xFFCBA1FA) else Color(0xFF764AA2)
            CardRarity.LEGENDARY -> c.coin
        },
        enabled = offer.affordable && enabled && !offer.empty && !offer.maxed,
        faded = offer.maxed,
        onClick = onBuy,
        modifier = modifier,
    ) {
        if (!offer.empty) LevelPips(offer.level, offer.maxLevel)
        Spacer(Modifier.weight(1f))
        if (offer.empty) {
            Text("Sold / empty", Type.footnote, c.ink3)
        } else if (offer.maxed) {
            Text("Maxed", Type.subhead, c.ink2)
        } else {
            CoinAmount(offer.price, color = if (offer.affordable) c.ink else c.ink3, style = Type.subhead)
        }
    }
}

@Composable
private fun Tile(
    name: String,
    blurb: String,
    tint: Color? = null,
    enabled: Boolean,
    faded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    eyebrow: String? = null,
    footer: @Composable RowScope.() -> Unit,
) {
    val c = Theme.colors
    val source = remember { MutableInteractionSource() }
    Column(
        modifier
            .pressScale(source, enabled)
            .height(124.dp)
            .alpha(if (faded) 0.45f else 1f)
            .clip(RoundedCornerShape(Corner.button))
            .background(if (c.dark) c.raised else c.ground)
            .then(if (tint != null) Modifier.border(2.dp, tint, RoundedCornerShape(Corner.button)) else Modifier)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.x3, vertical = 10.dp),
    ) {
        if (eyebrow != null) Text(eyebrow, Type.footnote, tint ?: c.ink2, maxLines = 1)
        Text(name, Type.headline, c.ink, maxLines = 2)
        Text(blurb, Type.footnote, c.ink2, maxLines = 3)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, content = footer)
    }
}

@Composable
private fun CompactAction(text: String, modifier: Modifier, enabled: Boolean, active: Boolean, action: () -> Unit) {
    val c = Theme.colors
    androidx.compose.foundation.layout.Box(modifier.height(44.dp).clip(RoundedCornerShape(Corner.button))
        .background(if (active) c.ink else c.track).clickable(enabled = enabled, role = Role.Button, onClick = action)
        .padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        Text(text, Type.footnote, if (!enabled) c.ink3 else if (active) c.ground else c.ink, maxLines = 2)
    }
}
