package blue.feelingso.khaos

import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.meta.Damageable
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import kotlin.math.absoluteValue
import kotlin.math.ceil

/**
 * Digging blocks likes Mogura
 *
 */

class Mogura(private val executor: Player, private val block: Block, private val tool: ItemStack, private val conf: KhaosConfig) {
    private val blockTypes = conf.getAllowedItems(tool.type)
    val runnable = blockTypes.contains(block.type.toString())

    fun run() {
        // 向いている向きとradius設定から破壊する範囲を設定し
        val direction = executor.eyeLocation.direction.normalize()

        // 方位を取得する
        // x軸が東西，z軸が南北
        val compass =
                if (direction.z.absoluteValue > direction.x.absoluteValue)
                    if (direction.z < 0)
                        Compass.NORTH
                    else
                        Compass.SOUTH
                else
                    if (direction.x < 0)
                        Compass.WEST
                    else Compass.EAST

        // 対象になるブロックをここに格納する
        val targetBlocks = mutableListOf<Block>()

        for (i in 1 - conf.radius until conf.radius) {
            for (j in 1 - conf.radius until conf.radius) {
                for (k in conf.near until conf.far)
                {
                    val targetBlock =
                            when (compass) {
                                Compass.EAST -> {
                                    block.getRelative(k, i, j)
                                }
                                Compass.WEST -> {
                                    block.getRelative(-k, i, j)
                                }
                                Compass.NORTH -> {
                                    block.getRelative(j, i, -k)
                                }
                                Compass.SOUTH -> {
                                    block.getRelative(j, i, k)
                                }
                            }

                    if (canDigBlock(targetBlock)) {
                        targetBlocks.add(targetBlock)
                    }
                }
            }
        }

        targetBlocks.forEach { it.breakNaturally(tool) }

        // 耐久を減らす
        val damage = if (conf.consume) targetBlocks.size else 1

        val damageable = tool.itemMeta as Damageable

        damageable.damage = damageable.damage + damage

        tool.itemMeta = damageable as ItemMeta

        // 耐久上限超えたら壊します
        if (damageable.damage > tool.type.maxDurability) {
            executor.inventory.remove(tool)
        }
    }

    // 対象のブロックが自分の足元より高い位置にあるか
    //　草の道やソウルサンドのような少し低いブロックの上で掘った際のことを考慮し、プレイヤの高さを切り上げている
    private fun isHigherThanFloor(block: Block) = block.y >= ceil(executor.location.y)

    private fun canDigBlock(block: Block): Boolean {
        // そのブロックはツールの対象に含まれているか
        if (!conf.isTargetBlockByTool(tool, block)) return false

        if (!conf.dontDigFloor) return true

        // 床下を掘らないという設定の場合、対象のブロックが自分の足元より高いか
        return isHigherThanFloor(block)
    }

    // ItemStackと破壊個数からダメージを計算する
    private fun calculateDamage(item: ItemStack, count: Int): Int {
        val ratio = 1.0f / (item.getEnchantmentLevel(Enchantment.DURABILITY) + 1)
        if (ratio == 1.0f) return count
        return (0 until count).filter { Math.random().toFloat() < ratio } .size
    }
}