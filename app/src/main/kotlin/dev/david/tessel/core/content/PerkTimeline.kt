package dev.david.tessel.core.content

/** All 18 ordinary perks occupy stable positions on their exclusive round ring.
 * Fusion positions exist mathematically but are never drawn before activation. */
object PerkTimeline {
    const val ROUND_BASE=200
    const val FUSION_BASE=100
    const val NODE_RADIUS=42f
    val radii=listOf(132f,253f,374f)
    fun perkNode(id:Int)=ROUND_BASE+id
    fun perkPoint(id:Int):Pair<Float,Float>{
        val round=PerkRounds.roundOf(id)
        require(round>=0)
        val index=PerkRounds.pools[round].indexOf(id)
        return UpgradeMap.point(radii[round],-90f+60f*index+if(round==1)30f else 0f)
    }
    fun fusionPoint(index:Int)=UpgradeMap.point(478f,-54f+index*72f)
    fun chosen(rounds:List<Int>,id:Int):Boolean{
        val round=PerkRounds.roundOf(id)
        return round>=0&&PerkRounds.choice(rounds,round)==id
    }
    fun links(rounds:List<Int>,perks:Int):List<MapLink> = buildList{
        for(combo in PerkCombo.entries.filter{it.active(perks)})
            for(id in combo.inputs)if(chosen(rounds,id))add(MapLink(perkNode(id),FUSION_BASE+combo.ordinal))
    }
    fun unrecorded(rounds:List<Int>,perks:Int)=Perks.runIds.filter{id->perks and (1 shl id)!=0&&!chosen(rounds,id)}
}
