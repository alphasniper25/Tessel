package dev.david.tessel.core.sim

import dev.david.tessel.core.content.Bestiary
import dev.david.tessel.core.content.CardFamily
import dev.david.tessel.core.content.CardCode
import dev.david.tessel.core.content.CardRarity
import dev.david.tessel.core.content.Perks
import dev.david.tessel.core.content.CardMission
import dev.david.tessel.core.content.Upgrades
import kotlin.math.min
import kotlin.math.roundToInt

const val SHOP_SLOTS = 3

/** Three single-use offers per player; locks carry across stages, including their price. */
class Shop {
    private val stock = Array(MAX_PLAYERS) { IntArray(SHOP_SLOTS) { -1 } }
    private val locks = Array(MAX_PLAYERS) { BooleanArray(SHOP_SLOTS) }
    private val quotes = Array(MAX_PLAYERS) { IntArray(SHOP_SLOTS) }
    private val rerolls = IntArray(MAX_PLAYERS)
    private val heals = IntArray(MAX_PLAYERS)
    private val offers = Array(MAX_PLAYERS) { IntArray(Perks.OFFERED) { -1 } }

    fun reset() {
        for (p in 0 until MAX_PLAYERS) {
            stock[p].fill(-1); locks[p].fill(false); quotes[p].fill(0)
            offers[p].fill(-1); rerolls[p] = 0; heals[p] = 0
        }
    }

    fun open(w: World) {
        for (p in 0 until w.playerCount) {
            rerolls[p] = 0
            heals[p] = 0
            restock(w, p)
            offerPerks(w, p)
        }
    }

    /** After a boss stage a player is offered a choice of perks to take. */
    private fun offerPerks(w: World, p: Int) {
        offers[p].fill(-1)
        if (w.stage % Bestiary.BOSS_EVERY != 0 && w.devBoss < 0) return
        var n = 0
        val free = IntArray(Perks.count)
        for (k in 0 until Perks.count) if (!w.players.has(p, k)) free[n++] = k
        for (slot in 0 until minOf(Perks.OFFERED, n)) {
            val pick = w.rng.int(slot, n - 1)
            val held = free[slot]
            free[slot] = free[pick]
            free[pick] = held
            offers[p][slot] = free[slot]
        }
    }

    /** Perk id offered in [slot], or -1. */
    fun perkOffer(p: Int, slot: Int) = offers[p][slot]

    fun perkPending(p: Int) = offers[p].any { it >= 0 }

    fun pickPerk(w: World, p: Int, slot: Int): Boolean {
        if (p !in 0 until w.playerCount) return false
        val k = offers.getOrNull(p)?.getOrNull(slot) ?: return false
        if (k < 0) return false
        val pl = w.players
        pl.perks[p] = pl.perks[p] or (1 shl k)
        if (k == Perks.THICK_SKIN) {
            pl.maxHp[p] += 2
            pl.hp[p] = min(pl.hp[p] + 2, pl.maxHp[p])
        }
        offers[p].fill(-1)
        return true
    }

    fun item(p: Int, slot: Int) = CardCode.item(stock.getOrNull(p)?.getOrNull(slot) ?: -1)
    fun raw(p: Int, slot: Int) = stock[p][slot]
    fun rarity(p: Int, slot: Int) = CardCode.tier(raw(p, slot))
    fun isRare(p: Int, slot: Int) = rarity(p, slot) == CardRarity.RARE
    fun locked(p: Int, slot: Int) = locks[p][slot]

    fun levelsBought(w: World, p: Int, slot: Int): Int {
        val u = item(p, slot)
        if (u < 0) return 0
        return minOf(rarity(p, slot).levels, Upgrades.of(u).maxLevel - w.players.levels[p][u]).coerceAtLeast(0)
    }

    fun maxed(w: World, p: Int, slot: Int) = levelsBought(w, p, slot) == 0

    private fun calculatePrice(w: World, p: Int, slot: Int): Int {
        val u = item(p, slot)
        if (u < 0) return 0
        var total = 0.0
        for (k in 0 until levelsBought(w, p, slot)) total += Upgrades.price(u, w.players.levels[p][u] + k, w.stage)
        total *= rarity(p, slot).premium
        if (w.players.has(p, Perks.HAGGLER)) total *= 0.9
        return total.roundToInt().coerceAtLeast(1)
    }

    fun price(w: World, p: Int, slot: Int): Int =
        if (locked(p, slot)) quotes[p][slot] else calculatePrice(w, p, slot)

    fun setLocked(w: World, p: Int, slot: Int, value: Boolean): Boolean {
        if (p !in 0 until w.playerCount || slot !in 0 until SHOP_SLOTS || maxed(w, p, slot)) return false
        if (value == locks[p][slot]) return false
        if (value) quotes[p][slot] = calculatePrice(w, p, slot) else quotes[p][slot] = 0
        locks[p][slot] = value
        return true
    }

    fun canBuy(w: World, p: Int, slot: Int) = p in 0 until w.playerCount && slot in 0 until SHOP_SLOTS &&
        !maxed(w, p, slot) && CardMission.eligible(item(p, slot), w.players.missionMask[p], rarity(p, slot)) && w.players.coins[p] >= price(w, p, slot)

    fun buy(w: World, p: Int, slot: Int): Boolean {
        if (!canBuy(w, p, slot)) return false
        val u = item(p, slot)
        val pl = w.players
        val n = levelsBought(w, p, slot)
        pl.coins[p] -= price(w, p, slot)
        pl.levels[p][u] += n
        val addedHp = when (u) { Upgrades.MAX_HEALTH -> n; Upgrades.TITAN_PLATING -> n * Upgrades.of(u).step.toInt(); else -> 0 }
        if (addedHp > 0) {
            pl.maxHp[p] += addedHp
            pl.hp[p] = min(pl.hp[p] + addedHp, pl.maxHp[p])
        }
        stock[p][slot] = -1
        locks[p][slot] = false
        quotes[p][slot] = 0
        return true
    }

    fun healPrice(w: World) = Upgrades.healPrice(w.stage)

    fun healAmount(w: World, p: Int) = Upgrades.HEAL_AMOUNT + w.players.levels[p][Upgrades.HEAL_POWER] + Upgrades.of(Upgrades.FIELD_MEDIC).step.toInt() * w.players.levels[p][Upgrades.FIELD_MEDIC]

    fun healsLeft(p: Int) = Upgrades.HEALS_PER_SHOP - heals[p]

    fun canHeal(w: World, p: Int) =
        p in 0 until w.playerCount && healsLeft(p) > 0 && w.players.hp[p] < w.players.maxHp[p] && w.players.coins[p] >= healPrice(w)

    fun heal(w: World, p: Int): Boolean {
        if (!canHeal(w, p)) return false
        val pl = w.players
        pl.coins[p] -= healPrice(w)
        pl.hp[p] = min(pl.hp[p] + healAmount(w, p), pl.maxHp[p])
        heals[p]++
        return true
    }

    fun rerollPrice(w: World, p: Int) = Upgrades.rerollPrice(w.stage, rerolls[p])

    fun canReroll(w: World, p: Int) = p in 0 until w.playerCount &&
        locks[p].any { !it } && w.players.coins[p] >= rerollPrice(w, p) &&
        (0 until Upgrades.count).any { u -> CardMission.eligible(u, w.players.missionMask[p]) && w.players.levels[p][u] < Upgrades.of(u).maxLevel &&
            (0 until SHOP_SLOTS).none { slot -> locked(p, slot) && item(p, slot) == u } }

    fun reroll(w: World, p: Int): Boolean {
        if (!canReroll(w, p)) return false
        w.players.coins[p] -= rerollPrice(w, p)
        rerolls[p]++
        restock(w, p)
        return true
    }

    internal fun keep(cp: Checkpoint) {
        for (p in 0 until cp.players) {
            stock[p].copyInto(cp.stock[p])
            locks[p].copyInto(cp.locked[p])
            quotes[p].copyInto(cp.quotes[p])
            cp.stockSaved[p] = true
            offers[p].copyInto(cp.perkOffer[p])
            cp.rerolls[p] = rerolls[p]
            cp.heals[p] = heals[p]
        }
    }

    /** Puts back the stock a [Checkpoint] kept; a seat with none gets a fresh one. */
    internal fun bringBack(w: World, cp: Checkpoint) {
        for (p in 0 until cp.players) {
            rerolls[p] = cp.rerolls[p]
            heals[p] = cp.heals[p]
            cp.stock[p].copyInto(stock[p])
            cp.locked[p].copyInto(locks[p])
            cp.quotes[p].copyInto(quotes[p])
            if (!cp.stockSaved[p]) restock(w, p)
            cp.perkOffer[p].copyInto(offers[p])
        }
    }

    private fun restock(w: World, p: Int) {
        val used = BooleanArray(Upgrades.count)
        // Reserve every retained offer before drawing anything, even a lock in the last slot.
        for (slot in 0 until SHOP_SLOTS) {
            val u = item(p, slot)
            if (locked(p, slot) && u >= 0 && CardMission.eligible(u, w.players.missionMask[p], rarity(p, slot)) && !maxed(w, p, slot)) used[u] = true
            else { locks[p][slot] = false; stock[p][slot] = -1; quotes[p][slot] = 0 }
        }
        val character = w.players.character[p]
        for (slot in 0 until SHOP_SLOTS) {
            if (locked(p, slot)) continue
            val available = (0 until Upgrades.count).filter { !used[it] && CardMission.eligible(it, w.players.missionMask[p]) && w.players.levels[p][it] < Upgrades.of(it).maxLevel }
            if (available.isEmpty()) continue
            var tier = CardRarity.roll(w.rng)
            var pool = available.filter { CardMission.eligible(it, w.players.missionMask[p], tier) }
            // Only if every starter is capped/retained: still offer an eligible mission card.
            if (pool.isEmpty()) {
                tier = CardMission.forUpgrade(available.first())!!.rarity
                pool = available.filter { CardMission.eligible(it, w.players.missionMask[p], tier) }
            }
            // Roll family first: adding more cards to a family does not inflate its odds.
            val families = CardFamily.entries.filter { family -> pool.any { Upgrades.of(it).family == family } }
            var ticket = w.rng.int(1, families.sumOf { character.weight(it) })
            val family = families.first { ticket -= character.weight(it); ticket <= 0 }
            val candidates = pool.filter { Upgrades.of(it).family == family }
            val u = candidates[w.rng.int(0, candidates.lastIndex)]
            used[u] = true
            stock[p][slot] = CardCode.pack(u, tier)
        }
    }
}
