package com.wepin.android.providerlib.utils

import android.content.pm.PackageManager
import com.wepin.android.commonlib.BuildConfig
import com.wepin.android.core.storage.WepinStorageManager
import com.wepin.android.core.types.storage.SelectedAddress
import com.wepin.android.core.types.storage.StorageDataType
import java.util.concurrent.CompletableFuture

fun getVersionMetaDataValue(): String {
    try {
        return BuildConfig.LIBRARY_VERSION
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return ""
}

fun <T> failedFuture(ex: Throwable): CompletableFuture<T> {
    val future = CompletableFuture<T>()
    future.completeExceptionally(ex)
    return future
}

internal fun getSelectedAddress(network: String): SelectedAddress? {
    val selectedAddressInfo =
        WepinStorageManager.getStorage<StorageDataType.WepinProviderSelectedAddress>("selectedAddress")
    if (selectedAddressInfo == null || selectedAddressInfo.addresses.size == 0) {
        return null
    }

    val userId = WepinStorageManager.getStorage<String>("user_id") ?: return null

    val selectedAddress = selectedAddressInfo.addresses.first { info ->
        info.network == network && info.userId == userId
    }
    return selectedAddress
}

internal fun setSelectedAddress(network: String, address: String) {
    val userId = WepinStorageManager.getStorage<String>("user_id") ?: return

    val selectedAddressInfo =
        WepinStorageManager.getStorage<StorageDataType.WepinProviderSelectedAddress>("selectedAddress")
            ?: StorageDataType.WepinProviderSelectedAddress(mutableListOf())

    val foundIndex = selectedAddressInfo.addresses.indexOfFirst { addressInfo ->
        addressInfo.network === network && addressInfo.userId === userId
    }

    if (foundIndex > 0) {
        selectedAddressInfo.addresses[foundIndex] = SelectedAddress(
            userId = userId,
            network = network,
            address = address
        )
    } else {
        selectedAddressInfo.addresses.add(
            SelectedAddress(
                userId = userId,
                network = network,
                address = address
            )
        )
    }

    WepinStorageManager.setStorage(key = "selectedAddress", data = selectedAddressInfo)
}