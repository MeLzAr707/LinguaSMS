package com.translator.messagingapp;

import android.util.Log;

import java.lang.reflect.Method;

/**
 * Utility class for safe reflection operations.
 * Prevents NullPointerExceptions when invoking methods through reflection.
 */
public class ReflectionUtils {
    private static final String TAG = "ReflectionUtils";

    /**
     * Safely invokes a method through reflection with proper null checking.
     * 
     * @param method The method to invoke (can be null)
     * @param obj The object to invoke the method on
     * @param args Arguments to pass to the method
     * @return The result of the method invocation, or null if the method is null or invocation fails
     */
    public static Object safeInvoke(Method method, Object obj, Object... args) {
        if (method == null) {
            Log.w(TAG, "Cannot invoke null method");
            return null;
        }
        
        try {
            return method.invoke(obj, args);
        } catch (Exception e) {
            Log.w(TAG, "Failed to invoke method: " + method.getName(), e);
            return null;
        }
    }

    /**
     * Safely gets a method from a class with proper error handling.
     * 
     * @param clazz The class to get the method from
     * @param methodName The name of the method
     * @param parameterTypes The parameter types of the method
     * @return The method if found, null otherwise
     */
    public static Method safeGetMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        if (clazz == null || methodName == null) {
            Log.w(TAG, "Cannot get method with null class or method name");
            return null;
        }
        
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            Log.d(TAG, "Method not found: " + clazz.getName() + "." + methodName);
            return null;
        } catch (Exception e) {
            Log.w(TAG, "Error getting method: " + clazz.getName() + "." + methodName, e);
            return null;
        }
    }

    /**
     * Safely gets a declared method from a class with proper error handling.
     * 
     * @param clazz The class to get the method from
     * @param methodName The name of the method
     * @param parameterTypes The parameter types of the method
     * @return The method if found, null otherwise
     */
    public static Method safeGetDeclaredMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        if (clazz == null || methodName == null) {
            Log.w(TAG, "Cannot get declared method with null class or method name");
            return null;
        }
        
        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            Log.d(TAG, "Declared method not found: " + clazz.getName() + "." + methodName);
            return null;
        } catch (Exception e) {
            Log.w(TAG, "Error getting declared method: " + clazz.getName() + "." + methodName, e);
            return null;
        }
    }

    /**
     * Safely gets a class by name with proper error handling.
     * 
     * @param className The fully qualified class name
     * @return The class if found, null otherwise
     */
    public static Class<?> safeGetClass(String className) {
        if (className == null) {
            Log.w(TAG, "Cannot get class with null name");
            return null;
        }
        
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "Class not found: " + className);
            return null;
        } catch (Exception e) {
            Log.w(TAG, "Error getting class: " + className, e);
            return null;
        }
    }

    /**
     * Attempts common garbage collection suppression methods safely.
     * This is a defensive approach to handle scenarios where system optimization
     * code might be trying to suppress GC during UI operations.
     * 
     * @param suppress Whether to suppress or resume GC
     * @return true if any GC control method was successfully invoked, false otherwise
     */
    public static boolean tryGcControl(boolean suppress) {
        boolean success = false;
        
        // Try VMRuntime.gcSuppression (if available)
        success |= tryVMRuntimeGcSuppression(suppress);
        
        // Try other potential GC control methods
        success |= tryAlternativeGcControl(suppress);
        
        return success;
    }

    /**
     * Safely starts an activity with GC protection during transitions.
     * This prevents NPEs that might occur during activity transitions.
     * 
     * @param activity The current activity
     * @param intent The intent to start
     * @return true if the activity was started successfully, false otherwise
     */
    public static boolean safeStartActivity(android.app.Activity activity, android.content.Intent intent) {
        if (activity == null || intent == null) {
            Log.w(TAG, "Cannot start activity with null activity or intent");
            return false;
        }
        
        // Protect against NPEs during activity start
        tryGcControl(true);
        
        try {
            activity.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to start activity: " + e.getMessage(), e);
            return false;
        } finally {
            tryGcControl(false);
        }
    }

    /**
     * Safely finishes an activity with GC protection during transitions.
     * This prevents NPEs that might occur during activity finishing.
     * 
     * @param activity The activity to finish
     * @return true if the activity was finished successfully, false otherwise
     */
    public static boolean safeFinish(android.app.Activity activity) {
        if (activity == null) {
            Log.w(TAG, "Cannot finish null activity");
            return false;
        }
        
        // Protect against NPEs during activity finish
        tryGcControl(true);
        
        try {
            activity.finish();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to finish activity: " + e.getMessage(), e);
            return false;
        } finally {
            tryGcControl(false);
        }
    }

    private static boolean tryVMRuntimeGcSuppression(boolean suppress) {
        try {
            Class<?> vmRuntimeClass = safeGetClass("dalvik.system.VMRuntime");
            if (vmRuntimeClass == null) {
                return false;
            }
            
            Method getRuntimeMethod = safeGetMethod(vmRuntimeClass, "getRuntime");
            if (getRuntimeMethod == null) {
                return false;
            }
            
            Object runtime = safeInvoke(getRuntimeMethod, null);
            if (runtime == null) {
                return false;
            }
            
            // Try different possible method names for GC suppression
            String[] methodNames = {"gcSuppression", "callGcSuppression", "setGcSuppression"};
            
            for (String methodName : methodNames) {
                Method gcMethod = safeGetMethod(vmRuntimeClass, methodName, boolean.class);
                if (gcMethod != null) {
                    Object result = safeInvoke(gcMethod, runtime, suppress);
                    if (result != null) {
                        Log.d(TAG, "Successfully called " + methodName + "(" + suppress + ")");
                        return true;
                    }
                }
            }
            
        } catch (Exception e) {
            Log.d(TAG, "VMRuntime GC suppression not available", e);
        }
        
        return false;
    }

    private static boolean tryAlternativeGcControl(boolean suppress) {
        try {
            // Try System.gc() control if available
            if (!suppress) {
                // Only suggest GC when resuming, never suppress through System.gc()
                System.gc();
                Log.d(TAG, "Called System.gc() as fallback");
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "Alternative GC control failed", e);
        }
        
        return false;
    }
}