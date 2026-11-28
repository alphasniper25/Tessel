package dev.david.tessel.core.content

/** Tunable campaign-only pacing; duel and tutorial retain their own damage rules. */
object CampaignScaling {
    const val TWIN_STAGE=30
    const val TWIN_HP_SHARE=.60f
    const val TWIN_FIRE_SHARE=.65f
    /** Modest late-game relief offsets reduced purchasing power; no dynamic skill adjustment. */
    fun enemyHullFactor(stage:Int)=1f-.10f*((stage-20).coerceIn(0,30)/30f)
    fun bossHullFactor(stage:Int)=1f-.07f*((stage-20).coerceIn(0,30)/30f)
    fun repair(stage:Int)=if(stage>=1) 1 else 0
    fun hullGrowth(stage:Int)=if(stage in 5..45 && stage%Bestiary.BOSS_EVERY==0) 2 else 0
    fun damage(stage:Int,difficulty:Difficulty,boss:Boolean):Int {
        val late=if(difficulty==Difficulty.EASY){if(stage>=26)1 else 0}else if(stage>=36)2 else if(stage>=16)1 else 0
        return late+if(boss&&stage>=25&&difficulty!=Difficulty.EASY)1 else 0
    }
}
