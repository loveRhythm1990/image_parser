package com.example.floatingscreenshot

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 英雄数据类
 */
data class HeroSpec(
    val name: String,
    val title: String,
    val skill4Name: String,
    val skill4Cooldown: String,
    val skill4Cost: String,
    val skill4Description: String
)

/**
 * 英雄数据管理器
 * 负责加载和查询英雄大招及通用技能的 CD 时间
 */
class HeroDataManager(private val context: Context) {
    
    companion object {
        private const val TAG = "HeroDataManager"
        private const val HERO_SPEC_FILE = "hero_spec.json"
        private const val COMMON_SKILL_FILE = "common_skill.json"
    }
    
    // 英雄大招数据映射 (英雄名 -> HeroSpec)
    private val heroSpecMap = mutableMapOf<String, HeroSpec>()
    
    // 通用技能 CD 时间映射 (技能名 -> CD时间)
    private val commonSkillCdMap = mutableMapOf<String, Int>()
    
    // 未找到英雄的随机 CD 缓存 (英雄名 -> 随机CD)
    private val unknownHeroCdCache = mutableMapOf<String, Int>()
    
    init {
        loadHeroSpec()
        loadCommonSkills()
    }
    
    /**
     * 从 assets 加载英雄大招数据
     */
    private fun loadHeroSpec() {
        try {
            val jsonString = readJsonFromAssets(HERO_SPEC_FILE)
            val jsonArray = JSONArray(jsonString)
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val heroSpec = HeroSpec(
                    name = jsonObject.getString("name"),
                    title = jsonObject.optString("title", ""),
                    skill4Name = jsonObject.optString("skill4_name", ""),
                    skill4Cooldown = jsonObject.optString("skill4_cooldown", "0"),
                    skill4Cost = jsonObject.optString("skill4_cost", "0"),
                    skill4Description = jsonObject.optString("skill4_description", "")
                )
                heroSpecMap[heroSpec.name] = heroSpec
            }
            
            Log.d(TAG, "加载了 ${heroSpecMap.size} 个英雄的大招数据")
            
        } catch (e: Exception) {
            Log.e(TAG, "加载英雄大招数据失败: ${e.message}", e)
        }
    }
    
    /**
     * 从 assets 加载通用技能 CD 数据
     */
    private fun loadCommonSkills() {
        try {
            val jsonString = readJsonFromAssets(COMMON_SKILL_FILE)
            val jsonObject = JSONObject(jsonString)
            
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val skillName = keys.next()
                val cd = jsonObject.getInt(skillName)
                commonSkillCdMap[skillName] = cd
            }
            
            Log.d(TAG, "加载了 ${commonSkillCdMap.size} 个通用技能的 CD 数据")
            
        } catch (e: Exception) {
            Log.e(TAG, "加载通用技能 CD 数据失败: ${e.message}", e)
        }
    }
    
    /**
     * 从 assets 读取 JSON 文件
     */
    private fun readJsonFromAssets(fileName: String): String {
        val stringBuilder = StringBuilder()
        try {
            val inputStream = context.assets.open(fileName)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
            bufferedReader.close()
            inputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "读取 assets 文件失败: $fileName", e)
        }
        return stringBuilder.toString()
    }
    
    /**
     * 获取英雄大招的 CD 时间（秒）
     * @param heroName 英雄名字
     * @return CD 时间，如果未找到则返回随机 CD（20-50秒）并缓存
     */
    fun getHeroUltimateCd(heroName: String): Int {
        val heroSpec = heroSpecMap[heroName]
        
        if (heroSpec == null) {
            // 未找到英雄，检查缓存
            if (unknownHeroCdCache.containsKey(heroName)) {
                val cachedCd = unknownHeroCdCache[heroName]!!
                Log.d(TAG, "未找到英雄: $heroName，使用缓存的随机 CD: ${cachedCd}秒")
                return cachedCd
            } else {
                // 生成 20-50 之间的随机 CD 并缓存
                val randomCd = (20..50).random()
                unknownHeroCdCache[heroName] = randomCd
                Log.w(TAG, "未找到英雄: $heroName 的大招数据，分配随机 CD: ${randomCd}秒")
                return randomCd
            }
        }
        
        return try {
            // skill4_cooldown 可能包含其他字符，如 "40(-4" 等，只提取数字部分
            val cdString = heroSpec.skill4Cooldown
            val numberPattern = Regex("\\d+")
            val match = numberPattern.find(cdString)
            val cd = match?.value?.toInt() ?: 0
            
            if (cd == 0) {
                // 如果 CD 为 0，也分配随机 CD
                if (unknownHeroCdCache.containsKey(heroName)) {
                    val cachedCd = unknownHeroCdCache[heroName]!!
                    Log.d(TAG, "英雄: $heroName 的 CD 为 0，使用缓存的随机 CD: ${cachedCd}秒")
                    return cachedCd
                } else {
                    val randomCd = (20..50).random()
                    unknownHeroCdCache[heroName] = randomCd
                    Log.w(TAG, "英雄: $heroName 的 CD 为 0，分配随机 CD: ${randomCd}秒")
                    return randomCd
                }
            }
            
            cd
        } catch (e: Exception) {
            Log.e(TAG, "解析英雄 $heroName 的 CD 时间失败: ${heroSpec.skill4Cooldown}", e)
            
            // 解析失败也分配随机 CD
            if (unknownHeroCdCache.containsKey(heroName)) {
                unknownHeroCdCache[heroName]!!
            } else {
                val randomCd = (20..50).random()
                unknownHeroCdCache[heroName] = randomCd
                randomCd
            }
        }
    }
    
    /**
     * 获取通用技能的 CD 时间（秒）
     * @param skillName 技能名字（如 "闪现"）
     * @return CD 时间，如果未找到返回 0
     */
    fun getCommonSkillCd(skillName: String): Int {
        return commonSkillCdMap[skillName] ?: run {
            Log.w(TAG, "未找到通用技能: $skillName 的 CD 数据")
            0
        }
    }
    
    /**
     * 获取英雄数据
     */
    fun getHeroSpec(heroName: String): HeroSpec? {
        return heroSpecMap[heroName]
    }
    
    /**
     * 检查英雄是否存在
     */
    fun hasHero(heroName: String): Boolean {
        return heroSpecMap.containsKey(heroName)
    }
}

