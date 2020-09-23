package xyz.magentaize.dynamicdata.binding

data class PropertyValue<TS, TV>(
    val sender: TS,
    val value: TV
) {
    internal var unobtainableValue: Boolean = false
        private set
}
