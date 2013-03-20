package grails.plugin.gson.test

import javax.servlet.http.*
import com.google.gson.*
import grails.plugin.gson.adapters.*
import grails.plugin.gson.metaclass.ArtefactEnhancer
import grails.plugin.gson.spring.GsonBuilderFactory
import grails.plugin.gson.support.proxy.DefaultEntityProxyHandler
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.*

class GsonUnitTestMixin extends GrailsUnitTestMixin {

	@BeforeClass
	static void initializeGsonDependencies() {

		// this has to be called first as there's no declarative way to enforce
		// execution order
		initGrailsApplication()

		defineBeans {

			// this is not named `proxyHandler` as a real app because GrailsUnitTestMixin
			// names it differently and we need to override that bean or
			// ControllerUnitTestMixin will shit the bed because something asserts
			// that there's only one bean that implements ProxyHandler
			grailsProxyHandler DefaultEntityProxyHandler

			domainSerializer GrailsDomainSerializer, ref('grailsApplication'), ref('grailsProxyHandler')
			domainDeserializer GrailsDomainDeserializer, ref('grailsApplication')

			gsonBuilder(GsonBuilderFactory) {
				// GrailsUnitTestMixin ignores PluginManagerAware so we need to wire
				// this explicitly
				pluginManager = ref('pluginManager')
			}
		}
	}

	@Before
	void enhanceApplication() {
		def gsonBuilder = applicationContext.getBean('gsonBuilder', GsonBuilder)
		def domainDeserializer = applicationContext.getBean('domainDeserializer', GrailsDomainDeserializer)

		def enhancer = new ArtefactEnhancer(grailsApplication, gsonBuilder, domainDeserializer)
		enhancer.enhanceControllers()
		enhancer.enhanceDomains()
		enhancer.enhanceRequest()

		def parser = new JsonParser()

		HttpServletRequest.metaClass.setGSON = { JsonElement json ->
			delegate.contentType = 'application/json'
			delegate.content = new Gson().toJson(json).getBytes('UTF-8')
		}

		HttpServletRequest.metaClass.setGSON = { CharSequence json ->
			delegate.setGSON parser.parse(json.toString())
		}

		HttpServletResponse.metaClass.getGSON = {->
			parser.parse delegate.contentAsString
		}
	}

}