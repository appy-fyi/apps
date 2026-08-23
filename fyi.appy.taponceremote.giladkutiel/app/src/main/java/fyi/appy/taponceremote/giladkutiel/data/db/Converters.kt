package fyi.appy.taponceremote.giladkutiel.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromProtocol(protocol: RemoteProtocol): String = protocol.name

    @TypeConverter
    fun toProtocol(value: String): RemoteProtocol = RemoteProtocol.valueOf(value)
}
