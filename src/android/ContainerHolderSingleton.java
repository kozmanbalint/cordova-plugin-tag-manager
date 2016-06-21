package com.jareddickson.cordova.tagmanager;

import com.google.android.gms.tagmanager.ContainerHolder;

public class ContainerHolderSingleton {
    private static ContainerHolder containerHolder;

    private ContainerHolderSingleton() {
    }

    public static ContainerHolder getContainerHolder() {
        return containerHolder;
    }

    public static void setContainerHolder(ContainerHolder c) {
        containerHolder = c;
    }
}