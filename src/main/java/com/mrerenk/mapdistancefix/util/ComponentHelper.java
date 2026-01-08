package com.mrerenk.mapdistancefix.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to handle ItemStack component access across different Minecraft versions.
 * Uses intermediary mappings for reflection to ensure compatibility across versions.
 *
 * The ItemStack.get() method has different intermediary names:
 * - method_57824: Used in some versions
 * - method_58694: Used in other versions (1.21-1.21.4 vs 1.21.5+)
 *
 * This class uses reflection with intermediary names to work across all versions.
 */
public class ComponentHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        "MapDistanceFix/ComponentHelper"
    );

    // Cached reflection objects for performance
    private static Object MAP_ID_COMPONENT_TYPE;
    private static Method ITEMSTACK_GET_METHOD;
    private static Method MAPID_ID_METHOD;
    private static boolean REFLECTION_INITIALIZED = false;
    private static boolean REFLECTION_FAILED = false;

    /**
     * Initialize reflection objects once on first use.
     * This caching approach avoids the performance overhead of reflection on every call.
     */
    private static synchronized void initializeReflection() {
        if (REFLECTION_INITIALIZED || REFLECTION_FAILED) {
            return;
        }

        try {
            // 1. Get the MAP_ID component type field from DataComponentTypes
            // Using intermediary mapping: class_9334 = DataComponentTypes, field_49646 = MAP_ID
            Class<?> dataComponentTypesClass = Class.forName(
                "net.minecraft.class_9334"
            );
            Field mapIdField = dataComponentTypesClass.getField("field_49646");
            MAP_ID_COMPONENT_TYPE = mapIdField.get(null);

            if (MAP_ID_COMPONENT_TYPE == null) {
                throw new IllegalStateException(
                    "MAP_ID component type is null"
                );
            }
            LOGGER.debug(
                "Found MAP_ID component type: {}",
                MAP_ID_COMPONENT_TYPE.getClass().getName()
            );

            // 2. Find the get() method on ItemStack using intermediary mapping
            // Try both method_57824 and method_58694 to support different versions
            String[] possibleMethodNames = { "method_58694", "method_57824" };

            for (String methodName : possibleMethodNames) {
                try {
                    // The parameter type is ComponentType which has intermediary name class_9331
                    for (Method method : ItemStack.class.getMethods()) {
                        if (
                            method.getName().equals(methodName) &&
                            method.getParameterCount() == 1 &&
                            method
                                .getParameterTypes()[0].getName()
                                .contains("class_9331")
                        ) {
                            ITEMSTACK_GET_METHOD = method;
                            LOGGER.debug(
                                "Found ItemStack.get() method: {} ({})",
                                methodName,
                                method.toGenericString()
                            );
                            break;
                        }
                    }
                    if (ITEMSTACK_GET_METHOD != null) {
                        break;
                    }
                } catch (Exception e) {
                    LOGGER.debug(
                        "Method {} not found, trying next",
                        methodName
                    );
                }
            }

            if (ITEMSTACK_GET_METHOD == null) {
                throw new IllegalStateException(
                    "Could not find ItemStack.get(ComponentType) method"
                );
            }

            // 3. Find the id() method on MapIdComponent (it's a record)
            // Using intermediary mapping: class_9209 = MapIdComponent, comp_2315 = id()
            Class<?> mapIdComponentClass = Class.forName(
                "net.minecraft.class_9209"
            );
            MAPID_ID_METHOD = mapIdComponentClass.getMethod("comp_2315");

            if (MAPID_ID_METHOD == null) {
                throw new IllegalStateException(
                    "Could not find MapIdComponent.id() method"
                );
            }
            LOGGER.debug("Found MapIdComponent.id() method");

            REFLECTION_INITIALIZED = true;
            LOGGER.debug("ComponentHelper reflection initialized successfully");
        } catch (Exception e) {
            REFLECTION_FAILED = true;
            LOGGER.error(
                "Failed to initialize ComponentHelper reflection - mod will not function properly",
                e
            );
        }
    }

    /**
     * Gets the map ID from an ItemStack using reflection with intermediary mappings.
     *
     * @param stack The ItemStack to check
     * @return The map ID if present, null otherwise
     */
    public static Integer getMapId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        // Initialize reflection on first use
        if (!REFLECTION_INITIALIZED && !REFLECTION_FAILED) {
            initializeReflection();
        }

        // If reflection failed, return null
        if (REFLECTION_FAILED) {
            return null;
        }

        try {
            // Call stack.get(DataComponentTypes.MAP_ID)
            Object mapIdComponent = ITEMSTACK_GET_METHOD.invoke(
                stack,
                MAP_ID_COMPONENT_TYPE
            );

            if (mapIdComponent != null) {
                // Call mapIdComponent.id()
                Object result = MAPID_ID_METHOD.invoke(mapIdComponent);
                return (Integer) result;
            }
        } catch (Exception e) {
            LOGGER.debug(
                "Failed to get map ID from ItemStack: {}",
                e.getMessage()
            );
        }

        return null;
    }

    /**
     * Check if the ComponentHelper is working correctly.
     *
     * @return true if initialized successfully, false if reflection failed
     */
    public static boolean isInitialized() {
        if (!REFLECTION_INITIALIZED && !REFLECTION_FAILED) {
            initializeReflection();
        }
        return REFLECTION_INITIALIZED && !REFLECTION_FAILED;
    }
}
