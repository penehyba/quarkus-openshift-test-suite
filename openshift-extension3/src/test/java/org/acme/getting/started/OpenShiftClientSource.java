package org.acme.getting.started;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

final class OpenShiftClientSource implements CloseableResource {
    final OpenShiftClient client;

    static OpenShiftClientSource createDefault() {
        return new OpenShiftClientSource(new DefaultOpenShiftClient());
    }

    private OpenShiftClientSource(OpenShiftClient client) {
        this.client = client;
    }

    @Override
    public void close() {
        client.close();
    }
}
