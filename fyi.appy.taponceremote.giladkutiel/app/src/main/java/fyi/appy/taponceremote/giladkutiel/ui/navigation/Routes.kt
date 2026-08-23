package fyi.appy.taponceremote.giladkutiel.ui.navigation

object Routes {
    const val LAUNCH = "launch"
    const val DEVICES = "devices"
    const val REMOTE = "remote/{deviceId}"
    const val TOUCHPAD = "remote/{deviceId}/touchpad"
    const val IR = "ir"
    const val SETTINGS = "settings"

    fun remote(deviceId: Long) = "remote/$deviceId"
    fun touchpad(deviceId: Long) = "remote/$deviceId/touchpad"
}
