package dev.david.tessel.core.sim

import dev.david.tessel.core.content.TankEvolution
import dev.david.tessel.core.content.Bestiary
import dev.david.tessel.core.content.CardFamily
import dev.david.tessel.core.content.CardCode
import dev.david.tessel.core.content.CardRarity
import dev.david.tessel.core.content.Perks
import dev.david.tessel.core.content.SkillTree
import dev.david.tessel.core.content.SkillGraph
import dev.david.tessel.core.content.SkillBonus
import dev.david.tessel.core.content.CardMission
import dev.david.tessel.core.content.Upgrades
import dev.david.tessel.core.content.UpgradeStat
import kotlin.math.min
import kotlin.math.roundToInt

const val SHOP_SLOTS = 3

/** Three single-use offers per player; locks carry across stages, including their price. */
class Shop {
    private val stock = Array(MAX_PLAYERS) { IntArray(SHOP_SLOTS) { -1 } }
    private val locks = Array(MAX_PLAYERS) { BooleanArray(SHOP_SLOTS) }
    private val quotes = Array(MAX_PLAYERS) { IntArray(SHOP_SLOTS) }
    private val rerolls = IntArray(MAX_PLAYERS) // paid refreshes only
    private val freeUsed = IntArray(MAX_PLAYERS)
    private val heals = IntArray(MAX_PLAYERS)
    private val evolutions = Array(MAX_PLAYERS) { IntArray(TankEvolution.REWARD_SLOTS) { -1 } }
    private val offers = Array(MAX_PLAYERS) { IntArray(Perks.OFFERED) { -1 } }

    fun reset() {
        for (p in 0 until MAX_PLAYERS) {
            stock[p].fill(-1); locks[p].fill(false); quotes[p].fill(0)
            offers[p].fill(-1); evolutions[p].fill(-1); rerolls[p] = 0; freeUsed[p] = 0; heals[p] = 0
        }
    }

    fun open(w: World) {
        for (p in 0 until w.playerCount) {
            rerolls[p] = 0
            freeUsed[p] = 0
            heals[p] = 0
            restock(w, p)
            offerPerks(w, p)
        }
    }

    /** After a boss stage a player is offered a choice of perks to take. */
    private fun offerPerks(w: World, p: Int) {
        offers[p].fill(-1)
        if (offerEvolution(w,p)) return
        if (w.stage % Bestiary.BOSS_EVERY != 0 && w.devBoss < 0) return
        var n = 0
        val free = IntArray(Perks.count)
        for (k in 0 until SkillTree.ORIGINAL_PERKS) if (!w.players.has(p, k)) free[n++] = k
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

    fun rewardOffer(p: Int, slot: Int) = if (evolutions[p][0]>=0) evolutions[p][slot] else offers[p].getOrElse(slot){-1}
    fun perkPending(p: Int) = evolutions[p][0]>=0 || offers[p].any { it >= 0 }
    private fun offerEvolution(w: World,p: Int): Boolean {
        val out=evolutions[p]; out.fill(-1)
        val frame=w.players.evolution[p]
        if(frame==TankEvolution.BASIC && w.stage!=5 && w.stage!=15 && w.stage!=25 && w.stage!=40) return false
        if(frame!=TankEvolution.BASIC && w.stage!=15 && w.stage!=25 && w.stage!=35) return false
        if(w.devBoss>=0 || w.stage%Bestiary.BOSS_EVERY!=0 || frame.tier>=TankEvolution.targetTier(w.stage) || w.players.lastEvolutionStage[p]==w.stage) return false
        val options=TankEvolution.options(frame,w.stage)
        for(i in options.indices) out[i]=TankEvolution.REWARD_BASE+options[i].ordinal
        if(frame==TankEvolution.BASIC && options.isNotEmpty() && w.stage<TankEvolution.SECRET_STAGE) out[options.size]=TankEvolution.STAY_BASIC
        return options.isNotEmpty()
    }

    fun pickPerk(w: World, p: Int, slot: Int): Boolean {
        if (p !in 0 until w.playerCount) return false
        if(evolutions[p][0]>=0){
            val chosen=evolutions[p].getOrElse(slot){-1}
            val pl=w.players
            if(chosen==TankEvolution.STAY_BASIC && pl.evolution[p]==TankEvolution.BASIC){
                pl.lastEvolutionStage[p]=w.stage; evolutions[p].fill(-1); offers[p].fill(-1); return true
            }
            val next=TankEvolution.fromReward(chosen) ?: return false
            if(next.parent!=pl.evolution[p].ordinal) return false
            // Refund legacy multishot; pre-evolution gun cards have a use in melee except these incompatible effects.
            for(u in 0 until Upgrades.count) if(!Upgrades.offered(u,next) && pl.levels[p][u]>0){
                // Refund actual spend; legacy saves estimate from uninflated base prices.
                pl.coins[p] += (pl.upgradeSpent[p][u]*.7).toInt()
                pl.upgradeSpent[p][u]=0
                pl.levels[p][u]=0
            }
            val hpGain=(next.hullBonus-pl.evolution[p].hullBonus).coerceAtLeast(0)
            pl.maxHp[p]+=hpGain; if(pl.hp[p]>0) pl.hp[p]=min(pl.maxHp[p],pl.hp[p]+hpGain)
            pl.evolution[p]=next; pl.shieldCooldown[p]=0; pl.lastEvolutionStage[p]=w.stage
            evolutions[p].fill(-1); offers[p].fill(-1)
            restock(w,p)
            return true
        }
        val k = offers.getOrNull(p)?.getOrNull(slot) ?: return false
        if (k < 0 || w.players.has(p,k)) return false
        val pl = w.players
        if (SkillTree.isNode(k)) return false
        pl.perks[p] = pl.perks[p] or (1 shl k)
        if (k == Perks.THICK_SKIN) {
            pl.maxHp[p] += 2
            pl.hp[p] = min(pl.hp[p] + 2, pl.maxHp[p])
        }
        offers[p].fill(-1)
        return true
    }

    /** Separate, optional coin purchase; never consumes an upgrade card or creates a forced reward. */
    fun canBuySkill(w:World,p:Int,id:Int):Boolean =
        w.phase==Phase.SHOP && !w.duel.on && p in 0 until w.playerCount && w.players.present[p] &&
        !perkPending(p) && SkillGraph.canBuy(w.players.skillMask(p),id,w.stage,w.players.coins[p])
    fun buySkill(w:World,p:Int,id:Int):Boolean {
        if(!canBuySkill(w,p,id))return false
        val pl=w.players
        val node=SkillGraph.nodes[id]
        pl.coins[p]-=SkillGraph.price(pl.skillMask(p),id)
        pl.setSkills(p,pl.skillMask(p) or SkillGraph.bit(id))
        if(node.bonus==SkillBonus.HEALTH){
            pl.maxHp[p]+=node.amount
            if(pl.hp[p]>0)pl.hp[p]=min(pl.maxHp[p],pl.hp[p]+node.amount)
        }
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
        return minOf(rarity(p, slot).levels, Upgrades.runCap(u) - w.players.levels[p][u]).coerceAtLeast(0)
    }

    fun maxed(w: World, p: Int, slot: Int) = levelsBought(w, p, slot) == 0

    private fun calculatePrice(w: World, p: Int, slot: Int): Int {
        val u = item(p, slot)
        if (u < 0) return 0
        var total = 0.0
        for (k in 0 until levelsBought(w, p, slot)) total += Upgrades.price(u, w.players.levels[p][u] + k, w.stage)
        total *= rarity(p, slot).premium
        if (w.players.has(p, Perks.HAGGLER)) total *= 0.9
        if(w.players.character[p] == dev.david.tessel.core.content.CharacterDef.BALANCED) total *= 0.9
        total *= 1.0 - w.players.skill(p,SkillBonus.CARD_DISCOUNT)*.01
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
        !perkPending(p) && Upgrades.availableAt(item(p,slot),w.stage) && Upgrades.offered(item(p,slot),w.players.evolution[p]) && !maxed(w, p, slot) && CardMission.eligible(item(p, slot), w.players.missionMask[p], rarity(p, slot)) && w.players.coins[p] >= price(w, p, slot)

    fun buy(w: World, p: Int, slot: Int): Boolean {
        if (!canBuy(w, p, slot)) return false
        val u = item(p, slot)
        val pl = w.players
        val n = levelsBought(w, p, slot)
        val paid=price(w,p,slot)
        pl.coins[p] -= paid
        pl.upgradeSpent[p][u] = (pl.upgradeSpent[p][u].toLong()+paid).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        pl.levels[p][u] += n
        val addedHp = (n * Upgrades.amount(u, UpgradeStat.MAX_HP)).toInt()
        if (addedHp > 0) {
            pl.maxHp[p] += addedHp
            pl.hp[p] = min(pl.hp[p] + addedHp, pl.maxHp[p])
        }
        stock[p][slot] = -1
        locks[p][slot] = false
        quotes[p][slot] = 0
        return true
    }

    fun healPrice(w: World,p:Int=0) = (Upgrades.healPrice(w.stage)*(1.0-w.players.skill(p,SkillBonus.HEAL_DISCOUNT)*.01)).roundToInt().coerceAtLeast(1)

    fun healAmount(w: World, p: Int) = Upgrades.HEAL_AMOUNT + Upgrades.delta(w.players.levels[p], UpgradeStat.HEAL).toInt() + w.players.skill(p,SkillBonus.HEAL_POWER)

    fun healsLeft(p: Int) = Upgrades.HEALS_PER_SHOP - heals[p]

    fun canHeal(w: World, p: Int) =
        p in 0 until w.playerCount && healsLeft(p) > 0 && w.players.hp[p] < w.players.maxHp[p] && w.players.coins[p] >= healPrice(w,p)

    fun heal(w: World, p: Int): Boolean {
        if (!canHeal(w, p)) return false
        val pl = w.players
        pl.coins[p] -= healPrice(w,p)
        pl.hp[p] = min(pl.hp[p] + healAmount(w, p), pl.maxHp[p])
        heals[p]++
        return true
    }

    fun freeRefreshesLeft(w:World,p:Int)=(w.players.skill(p,SkillBonus.FREE_REFRESH)-freeUsed[p]).coerceAtLeast(0)
    fun rerollPrice(w: World, p: Int): Int =
        if(freeRefreshesLeft(w,p)>0)0 else (Upgrades.rerollPrice(w.stage,rerolls[p])*(1.0-w.players.skill(p,SkillBonus.REROLL_DISCOUNT)*.01)).roundToInt().coerceAtLeast(1)

    fun canReroll(w: World, p: Int) = p in 0 until w.playerCount &&
        !perkPending(p) && locks[p].any { !it } && w.players.coins[p] >= rerollPrice(w, p) &&
        (0 until Upgrades.count).any { u -> Upgrades.availableAt(u,w.stage) && CardMission.eligible(u, w.players.missionMask[p]) && Upgrades.offered(u,w.players.evolution[p]) && w.players.levels[p][u] < Upgrades.runCap(u) &&
            (0 until SHOP_SLOTS).none { slot -> locked(p, slot) && item(p, slot) == u } }

    fun reroll(w: World, p: Int): Boolean {
        if (!canReroll(w, p)) return false
        val price=rerollPrice(w,p)
        w.players.coins[p]-=price
        if(price==0)freeUsed[p]++ else rerolls[p]++
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
            cp.freeRerollsUsed[p] = freeUsed[p]
            cp.heals[p] = heals[p]
        }
    }

    /** Puts back the stock a [Checkpoint] kept; a seat with none gets a fresh one. */
    internal fun bringBack(w: World, cp: Checkpoint) {
        for (p in 0 until cp.players) {
            rerolls[p] = cp.rerolls[p]
            freeUsed[p] = cp.freeRerollsUsed[p]
            heals[p] = cp.heals[p]
            cp.stock[p].copyInto(stock[p])
            cp.locked[p].copyInto(locks[p])
            cp.quotes[p].copyInto(quotes[p])
            if (!cp.stockSaved[p]) restock(w, p)
            cp.perkOffer[p].copyInto(offers[p])
            for(i in offers[p].indices)if(SkillTree.isNode(offers[p][i]))offers[p][i]=-1
            if(offerEvolution(w,p)) offers[p].fill(-1)
            // One-time migration: an alpha.10 shop with no remaining cards can
            // immediately offer new reserves instead of requiring a paid refresh.
            if(cp.loadedUpgradeCount <= Upgrades.RESERVE_POWER && w.stage >= 15 &&
                (0 until SHOP_SLOTS).all { maxed(w,p,it) }) restock(w,p)
            // Old saved offers may contain retired cards.
            for(slot in 0 until SHOP_SLOTS) if(item(p,slot)>=0 && (!Upgrades.availableAt(item(p,slot),w.stage) || !Upgrades.offered(item(p,slot),w.players.evolution[p]) || !CardMission.eligible(item(p,slot),w.players.missionMask[p],rarity(p,slot)))) {
                val available=(0 until Upgrades.count).filter { u -> Upgrades.availableAt(u,w.stage) && Upgrades.offered(u,w.players.evolution[p]) && w.players.levels[p][u]<Upgrades.runCap(u) && CardMission.eligible(u,w.players.missionMask[p],CardRarity.COMMON) && (0 until SHOP_SLOTS).none{item(p,it)==u} }
                stock[p][slot]=if(available.isEmpty()) -1 else CardCode.pack(available[w.rng.int(0,available.lastIndex)],CardRarity.COMMON)
                locks[p][slot]=false;quotes[p][slot]=0
            }
        }
    }

    private fun restock(w: World, p: Int) {
        val used = BooleanArray(Upgrades.count)
        // Reserve every retained offer before drawing anything, even a lock in the last slot.
        for (slot in 0 until SHOP_SLOTS) {
            val u = item(p, slot)
            if (locked(p, slot) && u >= 0 && Upgrades.availableAt(u,w.stage) && Upgrades.offered(u,w.players.evolution[p]) && CardMission.eligible(u, w.players.missionMask[p], rarity(p, slot)) && !maxed(w, p, slot)) used[u] = true
            else { locks[p][slot] = false; stock[p][slot] = -1; quotes[p][slot] = 0 }
        }
        val character = w.players.character[p]
        for (slot in 0 until SHOP_SLOTS) {
            if (locked(p, slot)) continue
            val available = (0 until Upgrades.count).filter { !used[it] && Upgrades.availableAt(it,w.stage) && CardMission.eligible(it, w.players.missionMask[p]) && Upgrades.offered(it,w.players.evolution[p]) && w.players.levels[p][it] < Upgrades.runCap(it) }
            if (available.isEmpty()) continue
            var tier = CardRarity.roll(w.rng, w.players.skill(p,SkillBonus.LUCK))
            var pool = available.filter { CardMission.eligible(it, w.players.missionMask[p], tier) }
            // Only if every starter is capped/retained: still offer an eligible mission card.
            if (pool.isEmpty()) {
                tier = CardMission.forUpgrade(available.first())!!.rarity
                pool = available.filter { CardMission.eligible(it, w.players.missionMask[p], tier) }
            }
            // Roll family first: adding more cards to a family does not inflate its odds.
            val families = CardFamily.entries.filter { family -> pool.any { Upgrades.of(it).family == family } }
            fun weight(f: CardFamily): Int {
                val frame=w.players.evolution[p]
                return character.weight(f) * when { frame.melee && (f==CardFamily.RAM || f==CardFamily.HEALTH) -> 3; !frame.melee && frame.tier>0 && f==CardFamily.GUN -> 2; else -> 1 }
            }
            var ticket = w.rng.int(1, families.sumOf { weight(it) })
            val family = families.first { ticket -= weight(it); ticket <= 0 }
            val candidates = pool.filter { Upgrades.of(it).family == family }
            val u = candidates[w.rng.int(0, candidates.lastIndex)]
            used[u] = true
            stock[p][slot] = CardCode.pack(u, tier)
        }
    }
}
