package com.wepin.android.providerlib.types

import com.wepin.android.commonlib.types.WepinAttribute

class WepinProviderAttributes(defaultLanguage: String? = "en", defaultCurrency: String? = "USD") :
    WepinAttribute(defaultLanguage, defaultCurrency)

class WepinProviderAttributeWithProviders(
    defaultLanguage: String? = "en",
    defaultCurrency: String? = "USD",
    var loginProviders: List<String> = emptyList()
) : WepinAttribute(defaultLanguage, defaultCurrency)
