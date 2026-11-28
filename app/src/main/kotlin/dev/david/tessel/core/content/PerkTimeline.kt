package dev.david.tessel.core.content

object PerkTimeline {
    const val ROUND_BASE=200
    const val FUSION_BASE=100
    fun roundPoint(index:Int)=UpgradeMap.point(265f,-90f+120f*index)
    fun fusionPoint(index:Int)=UpgradeMap.point(if(index<6)390f else 475f,-90f+(index%6)*60f+if(index>=6)23f else 0f)
    fun links(rounds:List<Int>,perks:Int):List<MapLink> = buildList{
        for(combo in PerkCombo.entries.filter{it.active(perks)}){
            for(id in combo.inputs){
                val index=PerkRounds.roundOf(id)
                if(index>=0&&PerkRounds.choice(rounds,index)==id)add(MapLink(ROUND_BASE+index,FUSION_BASE+combo.ordinal))
            }
        }
    }
    fun unrecorded(rounds:List<Int>,perks:Int)=Perks.runIds.filter{id->
        perks and (1 shl id)!=0 && PerkRounds.stages.indices.none{PerkRounds.choice(rounds,it)==id}
    }
}
