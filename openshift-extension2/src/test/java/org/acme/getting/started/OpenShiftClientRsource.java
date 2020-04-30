package org.acme.getting.started;

import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

final class OpenShiftClientRsource implements CloseableResource {
    final OpenShiftClient client;

    static OpenShiftClientRsource createDefault() {
        return new OpenShiftClientRsource(new DefaultOpenShiftClient());
    }

    private OpenShiftClientRsource(OpenShiftClient client) {
        this.client = client;
    }

    @Override
    public void close() {
        client.close();
    }
}
