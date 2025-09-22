package pingtest.domain.repository

import com.dip.pingtest.domain.model.Component
import com.dip.pingtest.domain.model.Request

interface ComponentRepository {
    fun getComponent(id: Int): Component?
    fun getRequestItemCount(id: Int): Int
    fun getRequest(id: Int): Request?
    fun getComponents(filter: String? = null): List<Component>
}