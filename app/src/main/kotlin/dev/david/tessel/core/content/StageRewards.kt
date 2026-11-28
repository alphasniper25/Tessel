package dev.david.tessel.core.content

/** Per-seat stage completion reward. Only current-stage income and kills count. */
object StageRewards {
    fun share(stage:Int)=CampaignEconomy.band(stage).pickupShare
    fun coins(stage:Int,collected:Int,kills:Int,difficulty:Difficulty=Difficulty.NORMAL)=
        CampaignEconomy.clearReward(stage,collected,kills,difficulty)
}
