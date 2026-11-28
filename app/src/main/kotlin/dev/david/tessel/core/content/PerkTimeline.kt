package dev.david.tessel.core.content

object PerkTimeline {
    const val ROUND_BASE=200
    const val FUSION_BASE=100
    fun roundPoint(index:Int)=UpgradeMap.point(295f,-90f+360f*index/RewardRounds.COUNT)
    fun fusionPoint(index:Int)=UpgradeMap.point(450f,UpgradeMap.angle(index))
    fun links(rounds:List<Int>,perks:Int):List<MapLink> = buildList{
        for(combo in PerkCombo.entries.filter{it.active(perks)}){
            for(id in listOf(combo.first,combo.second)){
                val index=rounds.indexOf(id)
                if(index in 0 until RewardRounds.COUNT)add(MapLink(ROUND_BASE+index,FUSION_BASE+combo.ordinal))
            }
        }
    }
    fun unrecorded(rounds:List<Int>,perks:Int)= (0 until SkillTree.ORIGINAL_PERKS).filter{perks and (1 shl it)!=0 && it !in rounds}
}
