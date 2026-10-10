package com.vlg.quotation.extraction;

/**
 * Scriptable provider double. Tests stage either a payload or a failure;
 * the last call is retained for forwarding assertions.
 */
public class FakeExtractionProvider implements QuotationExtractionProvider {

    public static final String PROVIDER_ID = "test-fake";

    private String nextPayload;
    private RuntimeException nextFailure;
    private ProviderCall lastCall;

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public synchronized String fetchDraftPayload(ProviderCall providerCall) {
        this.lastCall = providerCall;
        if (nextFailure != null) {
            RuntimeException stagedFailure = nextFailure;
            nextFailure = null;
            throw stagedFailure;
        }
        return nextPayload;
    }

    public synchronized void stagePayload(String draftPayload) {
        this.nextPayload = draftPayload;
        this.nextFailure = null;
    }

    public synchronized void stageFailure(RuntimeException stagedFailure) {
        this.nextFailure = stagedFailure;
    }

    public synchronized ProviderCall lastCall() {
        return lastCall;
    }
}
