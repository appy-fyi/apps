package fyi.appy.taponceremote.giladkutiel.data.ir

import android.content.Context
import org.json.JSONObject

class IrProfileRepository(private val context: Context) {
    fun loadProfiles(): List<IrProfile> {
        val json = context.assets.open("ir_profiles.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val array = root.getJSONArray("profiles")
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            val commandsObj = obj.getJSONObject("commands")
            val commands = commandsObj.keys().asSequence().associateWith { key ->
                java.lang.Long.decode(commandsObj.getString(key))
            }
            IrProfile(
                name = obj.getString("name"),
                protocol = obj.getString("protocol"),
                carrierFrequencyHz = obj.getInt("carrierFrequencyHz"),
                commands = commands,
            )
        }
    }
}
