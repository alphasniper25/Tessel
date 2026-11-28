package dev.david.tessel.core.content

/** One actual boss-shop reward decision per stage 5–45. Stage 50 ends the run.
 * Old checkpoints do not have historical choice data; never guess their dates. */
object RewardRounds {
    const val COUNT=9
    const val UNKNOWN=-1
    fun stage(index:Int)=(index+1)*Bestiary.BOSS_EVERY
    fun index(stage:Int)=if(stage in 5..45&&stage%Bestiary.BOSS_EVERY==0)stage/Bestiary.BOSS_EVERY-1 else -1
    fun valid(id:Int)=id in 0 until SkillTree.ORIGINAL_PERKS||id==TankEvolution.STAY_BASIC||TankEvolution.fromReward(id)!=null
}
