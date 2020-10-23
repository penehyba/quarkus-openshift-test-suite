package io.quarkus.ts.openshift.infinispan.client;

import io.quarkus.runtime.StartupEvent;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

public class InfinispanClientApp {

    private static final Logger LOGGER = LoggerFactory.getLogger("InfinispanClientApp");

    @Inject
    RemoteCacheManager cacheManager;

    private static final String CACHE_CONFIG = "<infinispan><cache-container>" +
            "<distributed-cache name=\"%s\"></distributed-cache>" +
            "</cache-container></infinispan>";

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("Create or get cache named mycache with the default configuration");
        RemoteCache<Object, Object> cache = cacheManager.administration().getOrCreateCache("mycache",
               new XMLStringConfiguration(String.format(CACHE_CONFIG, "mycache")));
        cache.put("hello", "Hello World, Infinispan is up!");
    }
}
