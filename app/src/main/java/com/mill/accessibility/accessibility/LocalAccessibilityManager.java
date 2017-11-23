package com.mill.accessibility.accessibility;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地Accessibility的管理类
 */
public class LocalAccessibilityManager {
    private List<AccessibilityChangedListener> listeners = new ArrayList<>();

    public static LocalAccessibilityManager getInstance(){
        return Holder.instance;
    }

    public void notifyAccessibilityChanged(boolean enable){
        for(AccessibilityChangedListener listener : listeners){
            listener.onAccessibilityChanged(enable);
        }
    }

    public void addAccessibilityChangedListener(AccessibilityChangedListener listener){
        if(listener != null && !listeners.contains(listener)){
            listeners.add(listener);
        }
    }

    public void removeAccessibilityChangedListener(AccessibilityChangedListener listener){
        listeners.remove(listener);
    }

    private LocalAccessibilityManager(){

    }


    private static class Holder{
        private static LocalAccessibilityManager instance = new LocalAccessibilityManager();
    }

    public interface AccessibilityChangedListener{
        void onAccessibilityChanged(boolean enable);
    }
}
