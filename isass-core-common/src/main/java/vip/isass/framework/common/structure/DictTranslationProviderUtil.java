package vip.isass.framework.common.structure;

import java.util.function.Supplier;

/**
 * Holds the optional dictionary translation provider without binding core code to an IoC container.
 */
public final class DictTranslationProviderUtil {

    private static volatile IDictTranslationProvider provider;
    private static volatile Supplier<IDictTranslationProvider> providerSupplier = () -> null;

    private DictTranslationProviderUtil() {
    }

    public static void setProvider(IDictTranslationProvider provider) {
        DictTranslationProviderUtil.provider = provider;
        DictTranslationProviderUtil.providerSupplier = () -> provider;
    }

    public static void setProviderSupplier(Supplier<IDictTranslationProvider> providerSupplier) {
        DictTranslationProviderUtil.provider = null;
        DictTranslationProviderUtil.providerSupplier = providerSupplier == null ? () -> null : providerSupplier;
    }

    public static IDictTranslationProvider getProvider() {
        if (provider == null) {
            try {
                provider = providerSupplier.get();
            } catch (Exception e) {
                provider = null;
            }
        }
        return provider;
    }
}
