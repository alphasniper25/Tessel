package dev.david.tessel.core.sim
import dev.david.tessel.core.content.*
import kotlin.math.*

// Same deterministic policy used for both versions; no invincibility, free money, skips, or forced unlocks.
private fun shop(w: World, p: Int): Int {
    val s=w.shop; val pl=w.players; var bought=0
    for (i in 0 until Perks.OFFERED) if(s.perkOffer(p,i)>=0){s.pickPerk(w,p,i);break}
    repeat(2){if(pl.hp[p] <= pl.maxHp[p]-2 && s.canHeal(w,p))s.heal(w,p)}
    repeat(3){
        val k=(0 until SHOP_SLOTS).filter{s.canBuy(w,p,it)}.maxByOrNull { slot ->
            val u=s.item(p,slot); val weight=when(u){Upgrades.DAMAGE->8;Upgrades.MULTISHOT->10;Upgrades.FIRE_RATE->8;Upgrades.MAX_HEALTH->9;Upgrades.PIERCE->7;Upgrades.SPEED->6;Upgrades.WEALTH->5;else->3};weight*s.levelsBought(w,p,slot).toDouble()/s.price(w,p,slot).coerceAtLeast(1)
        } ?: return@repeat
        if(s.buy(w,p,k))bought++
    }
    return bought
}
fun main(){
  for(d in listOf(Difficulty.EASY,Difficulty.NORMAL,Difficulty.HARD,Difficulty.IMPOSSIBLE)) for(n in listOf(1,2)) for(seed in 1L..4L){
    val w=World(seed);w.reset(n,seed,d);val chars=CharacterDef.entries
    for(p in 0 until n)w.players.chooseCharacter(p,chars[((seed-1+p)%chars.size).toInt()],d.bonusHp)
    val bots=Array(n){Bot(it*3.1f)};val input=Array(MAX_PLAYERS){PlayerInput()};var purchases=0;var firstCoins=-1;var firstBuys=-1;var frames=0
    while(!w.over && frames<60*60*90){
      if(w.phase==Phase.SHOP){if(w.stage==1)firstCoins=w.players.coins[0];var b=0;for(p in 0 until n)b+=shop(w,p);if(w.stage==1)firstBuys=b;purchases+=b;w.nextStage()}
      for(p in 0 until n)bots[p].drive(w,p,input[p]);w.step(input);w.events.clear();frames++
    }
    println("${d.name},$n,$seed,${w.stage},${w.victory},$firstCoins,$firstBuys,$purchases,${w.bossesDown},$frames,${w.over}")
  }
}
