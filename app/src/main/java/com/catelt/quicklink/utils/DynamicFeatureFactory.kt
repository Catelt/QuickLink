package com.catelt.quicklink.utils

import android.util.Log
import com.catelt.quicklink.presentation.component.DynamicFeature
import com.catelt.quicklink.presentation.component.DynamicFeatureComposable
import kotlin.reflect.full.createInstance

/**
 * Generic factory class for creating dynamic feature instances from dynamic modules.
 * This factory can handle any dynamic feature module that implements DynamicFeatureComposable.
 */
object DynamicFeatureFactory {
    
    /**
     * Attempts to load and create a dynamic feature interface from the specified module.
     * @param dynamicFeature The dynamic feature to load
     * @return A DynamicFeatureComposable instance if successful, null otherwise
     */
    fun createDynamicFeature(
        dynamicFeature: DynamicFeature
    ): DynamicFeatureComposable? {
        return try {
            // Try to load the implementation from the dynamic module
            Log.e("Hello", dynamicFeature.implementationClassName)
            val implementationClass = Class.forName(dynamicFeature.implementationClassName)
            implementationClass.kotlin.createInstance() as DynamicFeatureComposable
        } catch (e: ClassNotFoundException) {
            // The class doesn't exist, which means the module isn't loaded or the implementation doesn't exist
            null
        } catch (e: Exception) {
            // Other errors (instantiation, casting, etc.)
            null
        }
    }
    
    /**
     * Checks if a specific dynamic feature module is available.
     * @param dynamicFeature The dynamic feature to check
     * @return true if the dynamic feature is available, false otherwise
     */
    fun isDynamicFeatureAvailable(dynamicFeature: DynamicFeature): Boolean {
        return try {
            Class.forName(dynamicFeature.implementationClassName)
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
} 