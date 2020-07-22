package com.github.manosbatsis.vaultaire.example.workflow

import com.github.manosbatsis.corbeans.corda.common.NodesProperties
import com.github.manosbatsis.corbeans.corda.common.NodesPropertiesLoadingConfig
import com.github.manosbatsis.corbeans.corda.common.Util
import com.github.manosbatsis.corbeans.test.integration.NodeDriverHelper
import net.corda.core.utilities.loggerFor
import net.corda.testing.driver.NodeHandle

class WithNodeDriverProperties: NodesProperties() {
    companion object Config: NodesPropertiesLoadingConfig {
        override val resourcePath = "/nodedriver.properties"
        override val wrappingClass = WithNodeDriverProperties::class.java
        override val ignoreError = true
    }
}
interface WithDriverNodes {

    companion object {
        private val logger = loggerFor<WithDriverNodes>()
    }
    fun loadNodeProperties(): NodesProperties {
        return Util.loadProperties(WithNodeDriverProperties.Config)
    }
    fun getDriver(): NodeDriverHelper
    /**
     * Launch a network, execute the action code, and shut the network down
     */
    fun withDriverNodes(action: () -> Unit)

    fun nodeHandles(): Map<String, NodeHandle>
    fun nodeHandles(name: String): NodeHandle?
}

abstract class SimpleDriverNodes: WithDriverNodes {

    companion object {
        private val logger = loggerFor<SimpleDriverNodes>()
        private var driver: NodeDriverHelper? = null
    }

    override fun getDriver() = driver ?: initDriver()
    final override fun nodeHandles(): Map<String, NodeHandle> {
        return getDriver().nodeHandles
    }

    override fun nodeHandles(name: String) = nodeHandles()[name]

    protected fun initDriver(): NodeDriverHelper{
        driver = NodeDriverHelper(loadNodeProperties())
        return driver!!
    }
    /**
     * Launch a network, execute the action code, and shut the network down
     */
    override fun withDriverNodes(action: () -> Unit) {
        getDriver().withDriverNodes(action)
    }



}