package dynamicdata.binding

import java.beans.PropertyChangeListener
import java.util.concurrent.ConcurrentHashMap

internal class ObservablePropertyFactoryCache private constructor() {
    companion object{
        private val INSTANCE = ObservablePropertyFactoryCache()

        fun <T> instance(): ObservablePropertyFactoryCache {
            return INSTANCE
        }
    }

    private val _factories = ConcurrentHashMap<String, Any>()

    fun <T: PropertyChangeListener,R> getFactory(expression: (T)->R):ObservablePropertyFactory<T,R>{
        TODO()
    }
}
