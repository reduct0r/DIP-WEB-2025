package pingtest.domain.repository

import com.dip.pingtest.domain.model.Component

interface ComponentRepository {
    fun getComponents(filter: String? = null): List<Component>

    fun getComponent(id: Int): Component?

    fun getRequestItems(key: Int): List<Component>
}