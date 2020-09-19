package dynamicdata.binding

import dynamicdata.kernel.NotifyPropertyChanged
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty1

internal class ObservablePropertyFactoryCache private constructor() {
    companion object{
        private val INSTANCE = ObservablePropertyFactoryCache()

        fun instance(): ObservablePropertyFactoryCache {
            return INSTANCE
        }
    }

    private val _factories = ConcurrentHashMap<String, Any>()

    fun <T: NotifyPropertyChanged,R> getFactory(accessor: KProperty1<T,R>):ObservablePropertyFactory<T,R>{
        //var fields = mutableListOf<Field>()
        //getAllFields()
        //val owner = (la as PropertyReference1Impl).owner
        //val key = accessor.name

        return ObservablePropertyFactory(accessor)
    }

    private fun getAllFields(fields: MutableList<Field>, type: Class<*>): List<Field> {
        fields.addAll(listOf(*type.declaredFields))
        if (type.superclass != null) {
            getAllFields(fields, type.superclass)
        }
        return fields
    }
}
