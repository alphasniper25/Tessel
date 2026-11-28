package dev.david.tessel.core.content

import dev.david.tessel.core.stats.Totals

enum class CosmeticSlot { BODY, TURRET, PAINT, SKIN }
enum class HullVisual { CLASS, CIRCLE, SQUARE, DIAMOND, HEXAGON }
enum class SkinVisual { PLAIN, STRIPE, RING }
data class Cosmetic(val id: Int, val slot: CosmeticSlot, val label: String, val cost: Int,
    val color: Int = 0, val hull: HullVisual = HullVisual.CLASS, val barrelLength: Float = 1.15f,
    val barrels: Int = 1, val pattern: SkinVisual = SkinVisual.PLAIN)
object GarageCatalog {
    // Stable IDs, append only. Each slot's zero-cost first item is the default.
    val items = listOf(
        Cosmetic(0, CosmeticSlot.BODY, "Class hull", 0), Cosmetic(1, CosmeticSlot.BODY, "Square", 40, hull = HullVisual.SQUARE), Cosmetic(2, CosmeticSlot.BODY, "Diamond", 60, hull = HullVisual.DIAMOND), Cosmetic(3, CosmeticSlot.BODY, "Hexagon", 80, hull = HullVisual.HEXAGON),
        Cosmetic(4, CosmeticSlot.TURRET, "Standard", 0), Cosmetic(5, CosmeticSlot.TURRET, "Long", 45, barrelLength = 1.55f), Cosmetic(6, CosmeticSlot.TURRET, "Twin", 75, barrels = 2),
        Cosmetic(7, CosmeticSlot.PAINT, "Original", 0), Cosmetic(8, CosmeticSlot.PAINT, "Mint", 25, 0xFF76CF8A.toInt()), Cosmetic(9, CosmeticSlot.PAINT, "Blue", 25, 0xFF8EB5FF.toInt()), Cosmetic(10, CosmeticSlot.PAINT, "Amber", 35, 0xFFF2C14E.toInt()), Cosmetic(11, CosmeticSlot.PAINT, "Purple", 35, 0xFFCBA1FA.toInt()),
        Cosmetic(12, CosmeticSlot.SKIN, "Plain", 0), Cosmetic(13, CosmeticSlot.SKIN, "Stripe", 50, pattern = SkinVisual.STRIPE), Cosmetic(14, CosmeticSlot.SKIN, "Ring", 90, pattern = SkinVisual.RING),
        // Appended IDs preserve existing ownership bits and four-nibble wire appearance.
        Cosmetic(15,CosmeticSlot.BODY,"Round",55,hull=HullVisual.CIRCLE),
        Cosmetic(16,CosmeticSlot.TURRET,"Stub",55,barrelLength=.90f),
        Cosmetic(17,CosmeticSlot.TURRET,"Long twin",110,barrelLength=1.65f,barrels=2),
        Cosmetic(18,CosmeticSlot.PAINT,"Graphite",60,0xFF8995A1.toInt()),
        Cosmetic(19,CosmeticSlot.PAINT,"Copper",75,0xFFC78962.toInt()),
        Cosmetic(20,CosmeticSlot.PAINT,"Ice",55,0xFFA8DFEC.toInt()),
        Cosmetic(21,CosmeticSlot.PAINT,"Crimson",75,0xFFE88088.toInt()),
        Cosmetic(22,CosmeticSlot.PAINT,"Lime",55,0xFFC5D779.toInt()),
        Cosmetic(23,CosmeticSlot.PAINT,"Ivory",80,0xFFE3DCC9.toInt()),
        Cosmetic(24,CosmeticSlot.PAINT,"Indigo",85,0xFF888DD9.toInt()),
        Cosmetic(25,CosmeticSlot.PAINT,"Rose",85,0xFFD59AB4.toInt())
    )
    init { require(items.size <= 32 && items.indices.all { items[it].id == it }) }
    // Local slot indexes are stable as long as new items are appended to the catalog.
    val bySlot = CosmeticSlot.entries.map { slot -> items.filter { it.slot == slot } }
    private val wireIndexes = IntArray(items.size).also { indexes ->
        for (group in bySlot) { require(group.size <= 16); group.forEachIndexed { index, item -> indexes[item.id] = index } }
    }
    fun wirePart(id: Int) = wireIndexes.getOrElse(id) { 0 }
    fun partId(slot: CosmeticSlot, code: Int) = bySlot[slot.ordinal].getOrElse(code) { bySlot[slot.ordinal][0] }.id
    val defaultMask = items.filter { it.cost == 0 }.fold(0) { m, i -> m or (1 shl i.id) }
    fun owned(mask: Int, id: Int) = items.getOrNull(id)?.let { it.cost == 0 || mask and (1 shl id) != 0 } ?: false
    fun earned(t: Totals): Long = t.seconds.coerceAtLeast(0).toLong() / 30 + t.kills.coerceAtLeast(0).toLong() / 10 + t.bosses.coerceAtLeast(0).toLong() * 15 + t.completed.coerceAtLeast(0).toLong() * 100
    fun balance(t: Totals, mask: Int): Long = (earned(t) - items.filter { owned(mask, it.id) }.sumOf { it.cost }).coerceAtLeast(0)
    fun purchase(t: Totals, mask: Int, id: Int): Int {
        val item = items.getOrNull(id) ?: return mask
        return if (!owned(mask, id) && balance(t, mask) >= item.cost) mask or (1 shl id) else mask
    }
    fun classShape(c: CharacterDef) = when(c) { CharacterDef.BALANCED -> HullVisual.CIRCLE; CharacterDef.VANGUARD -> HullVisual.SQUARE; CharacterDef.STRIKER -> HullVisual.HEXAGON; CharacterDef.RAM -> HullVisual.DIAMOND }
    fun hull(look: TankLook, c: CharacterDef): HullVisual = items[look.body].hull.let { if(it == HullVisual.CLASS) classShape(c) else it }
}
data class TankLook(val body: Int = 0, val turret: Int = 4, val paint: Int = 7, val skin: Int = 12) {
    fun get(slot: CosmeticSlot) = when(slot) { CosmeticSlot.BODY -> body; CosmeticSlot.TURRET -> turret; CosmeticSlot.PAINT -> paint; CosmeticSlot.SKIN -> skin }
    fun equip(id: Int, mask: Int): TankLook {
        if (!GarageCatalog.owned(mask, id)) return this
        return when(GarageCatalog.items[id].slot) { CosmeticSlot.BODY -> copy(body=id); CosmeticSlot.TURRET -> copy(turret=id); CosmeticSlot.PAINT -> copy(paint=id); CosmeticSlot.SKIN -> copy(skin=id) }
    }
    fun encode() = "$body,$turret,$paint,$skin"
    // Four stable nibble IDs, validated independently. Purely visual.
    fun code() = GarageCatalog.wirePart(body) or (GarageCatalog.wirePart(turret) shl 4) or (GarageCatalog.wirePart(paint) shl 8) or (GarageCatalog.wirePart(skin) shl 12)
    companion object {
        fun fromCode(code: Int): TankLook {
            fun part(slot: CosmeticSlot) = GarageCatalog.partId(slot, (code ushr (slot.ordinal*4)) and 15)
            return TankLook(part(CosmeticSlot.BODY),part(CosmeticSlot.TURRET),part(CosmeticSlot.PAINT),part(CosmeticSlot.SKIN))
        }
        fun decode(raw: String?, mask: Int): TankLook {
            var look = TankLook()
            raw.orEmpty().split(',').mapNotNull { it.toIntOrNull() }.forEach { look = look.equip(it, mask) }
            return look
        }
    }
}
