package dev.david.tessel.core.content

import kotlin.math.*

/** Stable IDs (ordinal); append only. All weapon geometry is also consumed by the client and renderer.
 * damage is per projectile; barrels are bounded, so extra barrels are not a free DPS multiplier. */
enum class TankEvolution(
    val label: String, val detail: String, val parent: Int, val tier: Int,
    val barrels: Int = 1, val damage: Float = 1f, val rate: Float = 1f,
    val spread: Float = .12f, val reach: Float = 1f, val velocity: Float = 1f,
    val move: Float = 1f, val extraPierce: Int = 0, val ram: Float = 0f,
    val shieldSeconds: Float = 0f, val regenSeconds: Int = 0,
    val drones: Boolean = false, val length: Float = 1.3f, val lateral: Float = .32f,
    val secret: Boolean = false, val laser: Boolean = false,
) {
    BASIC("Basic", "Single standard barrel", -1, 0),
    TWIN("Twin", "Two forward barrels; needs close aim for both hits", 0, 1, 2, 0.7616f, 1.05f),
    MACHINE("Machine Gun", "Fast light shots; weak individual hits", 0, 1, 1, 0.767f, 2.1f, length=1.1f),
    SNIPER("Sniper", "Long-range piercing shots; slow reload", 0, 1, 1, 2.15f, .58f, reach=1.55f, velocity=1.4f, extraPierce=1, length=2f),
    RAMMER("Rammer", "Contact damage; rechargeable impact shield", 0, 1, 0, ram=65f, shieldSeconds=.58f, regenSeconds=4, move=1.20f),
    TRIPLET("Triplet", "Three focused barrels", 1, 2, 3, 0.6496f, 1.05f, spread=.07f),
    QUAD("Quad", "Four wider barrels", 1, 2, 4, 0.5824f, 1f, spread=.25f),
    MINIGUN("Minigun", "High sustained fire", 2, 2, 1, 0.7198f, 2.75f, length=1.5f),
    DESTROYER("Destroyer", "Heavy cannon; slow reload", 2, 2, 1, 6.726f, .31f, reach=1.15f, extraPierce=2, length=1.1f),
    ASSASSIN("Assassin", "Precision and long penetration", 3, 2, 1, 3.2f, .48f, reach=1.8f, velocity=1.55f, extraPierce=2, length=2.3f),
    OVERSEER("Overseer", "4 drones; aim to command, release to defend", 3, 2, 2, .80f, 1f, drones=true, lateral=1.9f),
    SMASHER("Smasher", "Fast shielded contact attacks", 4, 2, 0, ram=90f, shieldSeconds=.46f, regenSeconds=4, move=1.30f),
    FORTRESS("Fortress", "Regeneration and impact defense", 4, 2, 0, ram=100f, shieldSeconds=.50f, regenSeconds=3, move=1.12f),
    FOCUS("Focus", "Three concentrated barrels", 5, 3, 3, 0.784f, 1.08f, spread=.035f, extraPierce=1),
    FAN("Fan", "Three broad coverage barrels", 5, 3, 3, 0.8288f, 1.08f, spread=.27f),
    CROSSFIRE("Crossfire", "Four directional shots", 6, 3, 4, 0.7168f, 1.08f, spread=.62f),
    QUAD_GUARD("Quad Guard", "Four tight barrels", 6, 3, 4, 0.6608f, 1.08f, spread=.095f),
    GATLING("Gatling", "Two fast, light barrels", 7, 3, 2, 0.4956f, 2.75f, spread=.10f),
    SUPPRESSOR("Suppressor", "Rapid, penetrating stream", 7, 3, 1, 0.826f, 3.0f, extraPierce=1, length=1.7f),
    ANNIHILATOR("Annihilator", "Massive cannon; long gaps between shots", 8, 3, 1, 10.62f, .27f, extraPierce=3, length=1.2f),
    ARTILLERY("Artillery", "Twin heavy cannons", 8, 3, 2, 4.484f, .34f, spread=.18f, reach=1.3f, extraPierce=2),
    RANGER("Ranger", "Extreme-range precise fire", 9, 3, 1, 4.1f, .48f, reach=2.15f, velocity=1.7f, extraPierce=3, length=2.5f),
    MARKSMAN("Marksman", "Faster precision shots", 9, 3, 1, 2.8f, .72f, reach=1.65f, velocity=1.5f, extraPierce=2, length=2f),
    OVERLORD("Overlord", "6 durable drones; slower replacement", 10, 3, 4, .50f, 1.05f, drones=true, lateral=1.7f),
    SWARM("Swarm", "8 light drones; fast replacement, fragile hull", 10, 3, 3, .45f, 1.45f, drones=true, lateral=1.85f),
    SPIKE("Spike", "Powerful impacts", 11, 3, 0, ram=130f, shieldSeconds=.40f, regenSeconds=4, move=1.32f),
    BLITZ("Blitz", "Quick shield recharge", 11, 3, 0, ram=105f, shieldSeconds=.30f, regenSeconds=4, move=1.40f),
    LANDMINE("Landmine", "Heavy impact, strong recovery", 12, 3, 0, ram=150f, shieldSeconds=.46f, regenSeconds=3, move=1.13f),
    GUARDIAN("Guardian", "Rapid recovery, reliable shield", 12, 3, 0, ram=118f, shieldSeconds=.34f, regenSeconds=3, move=1.15f),
    MOTHERSHIP("Mothership", "8 durable autonomous drones", 23, 4, 8, .36f, 1.12f, drones=true, lateral=2.2f),
    PRISM("Prism Railgun", "Secret: near-instant straight piercing rail pulses", 0, 4, 1, 11f, .72f, reach=3.5f, velocity=16f, extraPierce=48, length=2.6f, secret=true, laser=true),
    DREADNOUGHT("Dreadnought", "Secret: eight elite drones with rapid replacement", 0, 4, 8, .45f, 1.25f, reach=1.35f, drones=true, lateral=2.6f, secret=true),
    TEMPEST("Tempest", "Secret: eight barrels in a broad fan", 0, 4, 8, .69f, 1.65f, spread=.14f, lateral=.36f, secret=true);

    val melee get() = barrels == 0
    val hullBonus get() = if(melee) tier*3+3 else 0
    // Impact splash and shield resistance are frame templates, not per-class branches.
    val impactRadius get() = if(melee) 58f + 12f*tier else 0f
    val impactSplash get() = if(melee) .45f else 0f
    val projectileArmor get() = if(melee) 1 + tier/2 else 0
    fun angle(aim: Float, k: Int, n: Int) = aim + (k-(n-1)/2f)*spread
    fun offset(k: Int, n: Int) = (k-(n-1)/2f)*lateral*Balance.PLAYER_RADIUS
    fun muzzle() = Balance.PLAYER_RADIUS * if (drones) lateral else length
    fun originAngle(aim: Float, k: Int, n: Int) = if(drones) aim + k * (2f * PI.toFloat() / n) else angle(aim,k,n)
    companion object {
        const val REWARD_BASE = 32
        const val REWARD_SLOTS = 5
        const val STAY_BASIC = 31
        const val SECRET_STAGE = 40
        private val children = Array(entries.size) { id -> entries.filter { it.parent == id }.toTypedArray() }
        fun fromId(id: Int) = entries.getOrElse(id) { BASIC }
        fun options(frame: TankEvolution, stage: Int = 0): Array<TankEvolution> = when {
            frame==BASIC && stage>=SECRET_STAGE -> children[0].filter { it.secret }.toTypedArray()
            else -> children[frame.ordinal].filter { !it.secret }.toTypedArray()
        }
        fun targetTier(stage: Int) = when { stage >= 35 -> 4; stage >= 25 -> 3; stage >= 15 -> 2; stage >= 5 -> 1; else -> 0 }
        fun fromReward(id: Int) = entries.getOrNull(id - REWARD_BASE)?.takeIf { it != BASIC }
        fun validReward(id: Int) = id == STAY_BASIC || id in 0 until Perks.count || fromReward(id) != null
        fun rewardName(id: Int) = if(id==STAY_BASIC) "Stay Basic" else fromReward(id)?.label ?: Perks.of(id).name
        fun rewardDetail(id: Int) = if(id==STAY_BASIC) "Skip evolution; clear stage 40 as Basic for secret frames." else fromReward(id)?.detail ?: Perks.of(id).blurb
    }
}
